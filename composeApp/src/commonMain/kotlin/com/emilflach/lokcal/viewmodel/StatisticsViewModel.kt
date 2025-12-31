package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.StatsMostEatenByWeight
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StatisticsViewModel(
    private val intakeRepo: IntakeRepository,
    private val exerciseRepo: ExerciseRepository,
    private val settingsRepo: SettingsRepository
) {
    data class DailyDelta(
        val day: String,
        val eaten: Double,
        val burned: Double,
        val delta: Double
    )

    data class Insights(
        val totalKgEaten: Double,
        val intakeCount: Long,
        val totalKcalEaten: Double,
        val totalKcalBurned: Double,
        val totalDelta: Double
    )

    data class ChartState(
        val allDays: List<String> = emptyList(),
        val startIndex: Int = 0,
        val endIndex: Int = -1,
    ) {
        val displayedDays: List<String> =
            if (allDays.isEmpty() || startIndex > endIndex) emptyList()
            else allDays.subList(startIndex.coerceAtLeast(0), (endIndex + 1).coerceAtMost(allDays.size))
    }

    private val _mostEaten = MutableStateFlow<List<StatsMostEatenByWeight>>(emptyList())
    val mostEaten: StateFlow<List<StatsMostEatenByWeight>> = _mostEaten.asStateFlow()

    private val _dailyKcal = MutableStateFlow<List<DailyDelta>>(emptyList())
    val dailyKcal: StateFlow<List<DailyDelta>> = _dailyKcal.asStateFlow()

    private val _chart = MutableStateFlow(ChartState())
    val chart: StateFlow<ChartState> = _chart.asStateFlow()

    private val _daysFilled = MutableStateFlow(0L)
    val daysFilled: StateFlow<Long> = _daysFilled.asStateFlow()

    private val _insights = MutableStateFlow<Insights?>(null)
    val insights: StateFlow<Insights?> = _insights.asStateFlow()

    enum class GraphMode {
        BALANCE, BURNED, EATEN
    }

    private val _graphMode = MutableStateFlow(GraphMode.BALANCE)
    val graphMode: StateFlow<GraphMode> = _graphMode.asStateFlow()

    init {
        val days: List<String> = intakeRepo.getDaysWithInformation()
        val today: String = currentDateIso()
        val filteredDays: List<String> = days.filter { d -> d != today }
        
        val start = (filteredDays.size - 30).coerceAtLeast(0)
        val end = (filteredDays.size - 1).coerceAtLeast(-1)
        
        _chart.value = ChartState(allDays = filteredDays, startIndex = start, endIndex = end)
        loadStats()
    }

    fun onChartRangeChanged(start: Int, end: Int) {
        val allDays = _chart.value.allDays
        if (allDays.isEmpty()) return
        _chart.value = ChartState(
            allDays = allDays,
            startIndex = start.coerceIn(0, (allDays.size - 1).coerceAtLeast(0)),
            endIndex = end.coerceIn(start.coerceAtLeast(0), (allDays.size - 1).coerceAtLeast(0))
        )
        loadStats()
    }

    fun setGraphMode(mode: GraphMode) {
        _graphMode.value = mode
    }

    private fun loadStats() {
        val currentChart = _chart.value
        val displayedDays = currentChart.displayedDays
        if (displayedDays.isEmpty()) {
            _mostEaten.value = emptyList()
            _dailyKcal.value = emptyList()
            _insights.value = null
            _daysFilled.value = intakeRepo.countDaysWithInformation()
            return
        }

        val startDate = displayedDays.first()
        val endDate = displayedDays.last()
        
        val startIso = "${startDate}T00:00:00"
        val endIso = "${endDate}T23:59:59"

        val eatenPerDay = intakeRepo.getDailyKcal(startIso, endIso)
        val burnedPerDay = exerciseRepo.getDailyBurned(startIso, endIso)
        val startingKcal = settingsRepo.getStartingKcal().coerceAtLeast(0.0)

        // Merge datasets based on displayedDays to ensure we show all days in range even if no data
        val sortedDays = displayedDays
        val deltas = sortedDays.map { day ->
            val eaten = eatenPerDay.find { it.day == day }?.total_kcal ?: 0.0
            val burned = burnedPerDay.find { it.day == day }?.total_burned ?: 0.0
            val delta = (eaten - (startingKcal + burned))
            DailyDelta(day = day, eaten = eaten, burned = burned, delta = delta)
        }

        _mostEaten.value = intakeRepo.getMostEatenByWeight(startIso, endIso)
        _dailyKcal.value = deltas
        _daysFilled.value = intakeRepo.countDaysWithInformation()

        val totalKcalEaten = intakeRepo.getTotalKcalEaten(startIso, endIso)
        val totalKcalBurned = exerciseRepo.sumKcalByDate(startIso, endIso)
        val totalKgEaten = intakeRepo.getTotalWeightEatenG(startIso, endIso) / 1000.0
        val intakeCount = intakeRepo.getCountTrackedIntakes(startIso, endIso)

        val totalBudget = (startingKcal * sortedDays.size) + totalKcalBurned
        val totalDelta = totalKcalEaten - totalBudget

        _insights.value = Insights(
            totalKgEaten = totalKgEaten,
            intakeCount = intakeCount,
            totalKcalEaten = totalKcalEaten,
            totalKcalBurned = totalKcalBurned,
            totalDelta = totalDelta
        )
    }
}
