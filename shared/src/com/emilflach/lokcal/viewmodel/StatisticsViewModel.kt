package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.StatsMostEatenByKcal
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.WeightRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatisticsViewModel(
    private val intakeRepo: IntakeRepository,
    private val exerciseRepo: ExerciseRepository,
    private val settingsRepo: SettingsRepository,
    private val weightRepo: WeightRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    data class DailyKcal(val day: String, val eaten: Double, val burned: Double, val balance: Double)

    data class Insights(
        val avgEaten: Double,                // average kcal eaten per day
        val avgBurned: Double,               // average kcal burned via exercise per day
        val avgNetIntake: Double,            // average of (eaten − burned) per day
        val dailyBudget: Double,             // current calorie goal from settings
        val weightTrendKgPerWeek: Double?,        // null if < 2 weight entries; from linear regression
        val impliedMaintenanceKcal: Double?,      // back-calculated via net intake and weight trend; null if no weight data
        val impliedTotalWeightChangeKg: Double?,  // trend * period weeks; null if no weight data
        val recommendation: String,
    )

    enum class Period { MONTH, THREE_MONTHS, ALL }

    private val _period = MutableStateFlow(Period.MONTH)
    val period: StateFlow<Period> = _period.asStateFlow()

    private val _topFoods = MutableStateFlow<List<StatsMostEatenByKcal>>(emptyList())
    val topFoods: StateFlow<List<StatsMostEatenByKcal>> = _topFoods.asStateFlow()

    private val _dailyKcal = MutableStateFlow<List<DailyKcal>>(emptyList())
    val dailyKcal: StateFlow<List<DailyKcal>> = _dailyKcal.asStateFlow()

    private val _daysFilled = MutableStateFlow(0L)
    val daysFilled: StateFlow<Long> = _daysFilled.asStateFlow()

    private val _insights = MutableStateFlow<Insights?>(null)
    val insights: StateFlow<Insights?> = _insights.asStateFlow()

    // Weight entries in the selected date range
    private val _weightData = MutableStateFlow<List<WeightLog>>(emptyList())
    val weightData: StateFlow<List<WeightLog>> = _weightData.asStateFlow()

    // All weight entries ever (for onboarding nudge)
    private val _allWeightEntries = MutableStateFlow<List<WeightLog>>(emptyList())
    val allWeightEntries: StateFlow<List<WeightLog>> = _allWeightEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // All tracked days (excluding today), used to resolve period indices
    private var allDays: List<String> = emptyList()

    init {
        scope.launch {
            _allWeightEntries.value = weightRepo.getAll()
            allDays = intakeRepo.getDaysWithInformation()
                .filter { it != currentDateIso() }
            _daysFilled.value = intakeRepo.countDaysWithInformation()
            loadStats()
            _isLoading.value = false
        }
    }

    fun setPeriod(p: Period) {
        _period.value = p
        loadStats()
    }

    private fun loadStats() {
        scope.launch {
            val days = allDays
            if (days.isEmpty()) {
                _topFoods.value = emptyList()
                _dailyKcal.value = emptyList()
                _weightData.value = emptyList()
                _insights.value = null
                return@launch
            }

            val displayedDays = when (_period.value) {
                Period.MONTH -> days.takeLast(30)
                Period.THREE_MONTHS -> days.takeLast(90)
                Period.ALL -> days
            }

            val startDate = displayedDays.first()
            val endDate = displayedDays.last()
            val startIso = "${startDate}T00:00:00"
            val endIso = "${endDate}T23:59:59"

            val eatenPerDay = intakeRepo.getDailyKcal(startIso, endIso)
            val burnedPerDay = exerciseRepo.getDailyBurned(startIso, endIso)
            val dailyBudget = settingsRepo.getStartingKcal().coerceAtLeast(0.0)

            val kcalByDay = displayedDays.map { day ->
                val eaten = eatenPerDay.find { it.day == day }?.total_kcal ?: 0.0
                val burned = burnedPerDay.find { it.day == day }?.total_burned ?: 0.0
                DailyKcal(day = day, eaten = eaten, burned = burned, balance = eaten - (dailyBudget + burned))
            }

            val trimmedKcalByDay = kcalByDay.dropLastWhile { it.eaten == 0.0 && it.burned == 0.0 }
            val trimmedEnd = trimmedKcalByDay.lastOrNull()?.day ?: endDate
            val weightInRange = weightRepo.getInRange(startDate, trimmedEnd)

            _topFoods.value = intakeRepo.getMostEatenByKcal(startIso, endIso).take(10)
            _dailyKcal.value = trimmedKcalByDay
            _weightData.value = weightInRange

            _insights.value = computeInsights(trimmedKcalByDay, weightInRange, dailyBudget)
        }
    }

    companion object {
        const val TREND_THRESHOLD_KG_PER_WEEK = 0.1
        const val TOTAL_CHANGE_THRESHOLD_KG = 1.5

        fun computeInsights(
            dailyKcal: List<DailyKcal>,
            weightLogs: List<WeightLog>,
            dailyBudget: Double
        ): Insights? {
            if (dailyKcal.isEmpty()) return null
            val n = dailyKcal.size.toDouble()
            val avgEaten = dailyKcal.sumOf { it.eaten } / n
            val avgBurned = dailyKcal.sumOf { it.burned } / n
            val avgNetIntake = avgEaten - avgBurned
            val weightTrend = computeWeightTrend(weightLogs)
            // 7700 kcal per kg of body fat ÷ 7 days = 1100 kcal/day per kg/week
            val impliedMaintenance = weightTrend?.let { avgNetIntake - it * 1100.0 }
            val periodWeeks = if (weightLogs.size >= 2)
                daysBetweenDates(weightLogs.first().date, weightLogs.last().date) / 7.0
            else 0.0
            val impliedTotalChange = weightTrend?.times(periodWeeks)
            return Insights(
                avgEaten = avgEaten,
                avgBurned = avgBurned,
                avgNetIntake = avgNetIntake,
                dailyBudget = dailyBudget,
                weightTrendKgPerWeek = weightTrend,
                impliedMaintenanceKcal = impliedMaintenance,
                impliedTotalWeightChangeKg = impliedTotalChange,
                recommendation = buildRecommendation(avgNetIntake, dailyBudget, weightTrend, impliedMaintenance, impliedTotalChange)
            )
        }

        /**
         * Returns kg/week trend via linear regression across all weight entries.
         * Much more robust than first-vs-last: single noisy measurements don't skew the result.
         * Returns null if fewer than 2 entries.
         */
        fun computeWeightTrend(weights: List<WeightLog>): Double? {
            if (weights.size < 2) return null
            val origin = weights.first()
            val xs = weights.map { daysBetweenDates(origin.date, it.date).toDouble() }
            val ys = weights.map { it.weight_kg }
            val n = xs.size
            val xMean = xs.sum() / n
            val yMean = ys.sum() / n
            val numerator = xs.indices.sumOf { i -> (xs[i] - xMean) * (ys[i] - yMean) }
            val denominator = xs.sumOf { x -> (x - xMean) * (x - xMean) }
            if (denominator == 0.0) return null
            return (numerator / denominator) * 7.0  // kg/day → kg/week
        }

        fun buildRecommendation(
            avgNetIntake: Double,
            dailyBudget: Double,
            weightTrend: Double?,
            impliedMaintenance: Double?,
            impliedTotalChange: Double? = null
        ): String {
            val netInt = avgNetIntake.toInt()
            val budgetInt = dailyBudget.toInt()
            val losing = weightTrend != null && (weightTrend < -TREND_THRESHOLD_KG_PER_WEEK || (impliedTotalChange != null && impliedTotalChange < -TOTAL_CHANGE_THRESHOLD_KG))
            val gaining = weightTrend != null && (weightTrend > TREND_THRESHOLD_KG_PER_WEEK || (impliedTotalChange != null && impliedTotalChange > TOTAL_CHANGE_THRESHOLD_KG))

            if (weightTrend == null) {
                val delta = (avgNetIntake - dailyBudget).toInt()
                val monthlyKg = (kotlin.math.round(kotlin.math.abs(delta) * 30.0 / 770.0) / 10.0)
                return when {
                    delta < -200 ->
                        "You're eating a net $netInt kcal/day — ${-delta} kcal below your $budgetInt kcal goal. If accurate, that's roughly $monthlyKg kg lost per month. Add regular weigh-ins to verify."
                    delta > 200 ->
                        "You're eating a net $netInt kcal/day — $delta kcal above your $budgetInt kcal goal, roughly $monthlyKg kg gained per month. Try trimming portions or logging more exercise."
                    else ->
                        "You're eating a net $netInt kcal/day, close to your $budgetInt kcal goal. Add regular weigh-ins to confirm the effect on your weight."
                }
            }

            val trendStr = kotlin.math.round(kotlin.math.abs(weightTrend) * 100.0) / 100.0
            val mainInt = impliedMaintenance!!.toInt()

            return when {
                losing -> {
                    val budgetNote = if (mainInt - budgetInt > 300)
                        " Your maintenance calories are ~$mainInt kcal — your $budgetInt kcal goal is set ${mainInt - budgetInt} kcal below that, which explains the loss."
                    else
                        " Your maintenance calories are ~$mainInt kcal/day."
                    "You're losing $trendStr kg/week on a net ~$netInt kcal/day.$budgetNote Keep it up!"
                }
                gaining -> {
                    "You're gaining $trendStr kg/week on a net ~$netInt kcal/day. Based on your data, your maintenance calories are ~$mainInt kcal/day. " +
                        "To stop gaining, aim for ~$mainInt kcal/day net."
                }
                else -> {
                    val budgetNote = when {
                        mainInt - budgetInt > 200 ->
                            " Your $budgetInt kcal goal is set ${mainInt - budgetInt} kcal below your maintenance — consider updating it to ~$mainInt kcal to reflect reality."
                        budgetInt - mainInt > 200 ->
                            " Your $budgetInt kcal goal is ${budgetInt - mainInt} kcal above your maintenance."
                        else -> ""
                    }
                    "Your weight is stable eating a net ~$netInt kcal/day — that's your maintenance.$budgetNote"
                }
            }
        }

        private fun daysBetweenDates(from: String, to: String): Long {
            fun parts(d: String) = Triple(d.substring(0, 4).toInt(), d.substring(5, 7).toInt(), d.substring(8, 10).toInt())
            val (y1, m1, d1) = parts(from)
            val (y2, m2, d2) = parts(to)
            fun toEpochDay(y: Int, m: Int, d: Int): Long {
                val a = (14 - m) / 12
                val yr = y + 4800 - a
                val mo = m + 12 * a - 3
                return d + (153 * mo + 2) / 5 + 365L * yr + yr / 4 - yr / 100 + yr / 400 - 32045
            }
            return toEpochDay(y2, m2, d2) - toEpochDay(y1, m1, d1)
        }
    }
}
