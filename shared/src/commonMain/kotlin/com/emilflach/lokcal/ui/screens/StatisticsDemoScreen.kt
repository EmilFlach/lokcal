package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.emilflach.lokcal.StatsMostEatenByKcal
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.viewmodel.StatisticsViewModel

// Reference end date for all demo data
private const val DEMO_END = "2026-04-09"

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StatisticsDemoScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val scenario = remember { buildDemoScenario() }
    var period by remember { mutableStateOf(StatisticsViewModel.Period.MONTH) }
    val listState = rememberLazyListState()
    val colors = LocalRecipesColors.current

    val displayedDays = when (period) {
        StatisticsViewModel.Period.MONTH -> scenario.allDailyKcal.takeLast(30)
        StatisticsViewModel.Period.THREE_MONTHS -> scenario.allDailyKcal.takeLast(90)
        StatisticsViewModel.Period.ALL -> scenario.allDailyKcal
    }
    val startDate = displayedDays.firstOrNull()?.day
    val endDate = displayedDays.lastOrNull()?.day
    val filteredWeights = if (startDate != null && endDate != null) {
        scenario.allWeightLogs.filter { it.date in startDate..endDate }
    } else emptyList()
    val insights = remember(displayedDays, filteredWeights) {
        StatisticsViewModel.computeInsights(displayedDays, filteredWeights, dailyBudget = 2200.0)
    }

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Demo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.backgroundPage)
            )
        },
        containerColor = colors.backgroundPage,
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { paddingValues ->
        StatisticsBody(
            topFoods = scenario.topFoods,
            dailyKcal = displayedDays,
            daysFilled = scenario.daysFilled,
            insights = insights,
            period = period,
            weightData = filteredWeights,
            allWeightEntries = scenario.allWeightLogs,
            onPeriodChange = { period = it },
            paddingValues = paddingValues,
            listState = listState
        )
    }
}

// ── Scenario ──────────────────────────────────────────────────────────────────

private data class DemoScenario(
    val topFoods: List<StatsMostEatenByKcal>,
    val allDailyKcal: List<StatisticsViewModel.DailyKcal>,
    val allWeightLogs: List<WeightLog>,
    val daysFilled: Long,
)

/**
 * 180-day scenario with three phases:
 * - Oldest 90 days: deficit → losing weight (87→83 kg)
 * - Middle 60 days: surplus → gaining (83→85.5 kg)
 * - Recent 30 days: deficit again → losing (85.5→84.2 kg)
 *
 * Result per tab:
 * - 30 days: clearly losing
 * - 90 days: net upward trend (gaining phase dominates)
 * - All time: net loss overall
 */
private fun buildDemoScenario(): DemoScenario {
    val allDates = generateDates(DEMO_END, 180)
    val phase1 = allDates.take(90)       // oldest: losing
    val phase2 = allDates.drop(90).take(60) // middle: gaining
    val phase3 = allDates.drop(150)      // recent: losing again

    val eaten = realisticKcal(phase1, 1920.0, 260.0, 340.0, 0.10, 380.0, 10) +
                realisticKcal(phase2, 2680.0, 340.0, 520.0, 0.20, 560.0, 20) +
                realisticKcal(phase3, 1870.0, 250.0, 310.0, 0.10, 360.0, 30)

    val burned = realisticBurned(phase1, 3.0, 200.0, 11) +
                 realisticBurned(phase2, 1.5, 150.0, 21) +
                 realisticBurned(phase3, 3.5, 220.0, 31)

    val dailyKcal = allDates.indices.map { i ->
        StatisticsViewModel.DailyKcal(day = allDates[i], eaten = eaten[i], burned = burned[i],
            balance = eaten[i] - (2200.0 + burned[i]))
    }

    val weights = (realisticWeightLogs(phase1, 87.0, 83.0, 0.60, 3, 101) +
                   realisticWeightLogs(phase2, 83.0, 85.5, 0.65, 3, 201) +
                   realisticWeightLogs(phase3, 85.5, 84.2, 0.55, 3, 301))
        .mapIndexed { i, w -> w.copy(id = (i + 1).toLong()) }

    return DemoScenario(
        topFoods = demoFoods(),
        allDailyKcal = dailyKcal,
        allWeightLogs = weights,
        daysFilled = 180L
    )
}

// ── Food list ─────────────────────────────────────────────────────────────────

private fun demoFoods(): List<StatsMostEatenByKcal> = listOf(
    fakeFood(1L, "Chicken breast", 7400.0, 3700.0, 38L),
    fakeFood(2L, "Rice", 6200.0, 2300.0, 42L),
    fakeFood(3L, "Eggs", 4800.0, 2000.0, 55L),
    fakeFood(4L, "Bread", 4400.0, 1600.0, 48L),
    fakeFood(5L, "Greek yogurt", 3600.0, 2400.0, 35L),
    fakeFood(6L, "Oats", 3100.0, 1250.0, 30L),
    fakeFood(7L, "Banana", 2600.0, 2900.0, 60L),
    fakeFood(8L, "Cheese", 2200.0, 440.0, 22L),
)

private fun fakeFood(id: Long, name: String, kcal: Double, grams: Double, count: Long) =
    StatsMostEatenByKcal(food_id = id, item_name = name, total_kcal = kcal, total_quantity_g = grams, intake_count = count)

// ── Data generation utilities ─────────────────────────────────────────────────

private fun generateDates(endDate: String, count: Int): List<String> {
    val end = isoToEpochDay(endDate)
    return (count - 1 downTo 0).map { epochDayToIso(end - it) }
}

private fun realisticKcal(
    dates: List<String>,
    base: Double,
    noise: Double,
    weekendBump: Double,
    spikeProbability: Double,
    spikeKcal: Double,
    seed: Int
): List<Double> {
    val drift = correlatedNoise(dates.size, seed, persistence = 0.55)
    return dates.mapIndexed { i, date ->
        val dayOfWeek = (isoToEpochDay(date) + 4) % 7
        val isWeekend = dayOfWeek >= 5
        val weekend = if (isWeekend) weekendBump * (0.5 + pseudoRandom(i, seed + 100) * 0.5) else 0.0
        val spike = if (pseudoRandom(i, seed + 200) < spikeProbability) spikeKcal * (0.6 + pseudoRandom(i, seed + 300) * 0.4) else 0.0
        (base + drift[i] * noise + weekend + spike).coerceAtLeast(900.0)
    }
}

private fun realisticBurned(
    dates: List<String>,
    workoutsPerWeek: Double,
    avgBurnPerWorkout: Double,
    seed: Int
): List<Double> {
    val baseProbPerDay = workoutsPerWeek / 7.0
    return dates.mapIndexed { i, date ->
        val dayOfWeek = (isoToEpochDay(date) + 4) % 7
        val prob = when (dayOfWeek.toInt()) {
            0, 2, 4 -> baseProbPerDay * 1.35
            5       -> baseProbPerDay * 1.2
            6       -> baseProbPerDay * 0.4
            else    -> baseProbPerDay
        }.coerceAtMost(0.92)
        if (pseudoRandom(i, seed) < prob) {
            val intensity = pseudoRandom(i, seed + 50)
            val multiplier = when {
                intensity < 0.30 -> 0.40 + pseudoRandom(i, seed + 60) * 0.25
                intensity < 0.72 -> 0.72 + pseudoRandom(i, seed + 70) * 0.30
                else             -> 1.15 + pseudoRandom(i, seed + 80) * 0.45
            }
            (avgBurnPerWorkout * multiplier).coerceAtLeast(50.0)
        } else 0.0
    }
}

private fun realisticWeightLogs(
    dates: List<String>,
    startKg: Double,
    endKg: Double,
    dailyNoiseKg: Double,
    step: Int,
    seed: Int
): List<WeightLog> {
    val noise = correlatedNoise(dates.size, seed, persistence = 0.65)
    var idCounter = 1L
    return dates.filterIndexed { i, _ -> i % step == 0 }.mapIndexed { i, date ->
        val globalIdx = i * step
        val t = globalIdx.toDouble() / (dates.size - 1).toDouble().coerceAtLeast(1.0)
        val trend = startKg + t * (endKg - startKg)
        WeightLog(id = idCounter++, date = date,
            weight_kg = (trend + noise[globalIdx] * dailyNoiseKg).coerceAtLeast(30.0))
    }
}

private fun correlatedNoise(n: Int, seed: Int, persistence: Double): List<Double> {
    val result = mutableListOf<Double>()
    var state = 0.0
    for (i in 0 until n) {
        val white = pseudoRandom(i, seed) * 2.0 - 1.0
        state = state * persistence + white * (1.0 - persistence)
        result.add(state)
    }
    return result
}

private fun pseudoRandom(index: Int, seed: Int): Double {
    var x = index.toLong() * 2654435761L xor seed.toLong() * 2246822519L
    x = x xor (x ushr 17)
    x = x * -7046029254386353131L
    x = x xor (x ushr 31)
    x = x * -2270073950306794567L
    x = x xor (x ushr 33)
    return (x and 0x7FFFFFFFFFFFFFFFL).toDouble() / 0x7FFFFFFFFFFFFFFFL.toDouble()
}

private fun isoToEpochDay(date: String): Long {
    val y = date.substring(0, 4).toInt()
    val m = date.substring(5, 7).toInt()
    val d = date.substring(8, 10).toInt()
    val a = (14 - m) / 12
    val yr = y + 4800 - a
    val mo = m + 12 * a - 3
    return d + (153 * mo + 2) / 5 + 365L * yr + yr / 4 - yr / 100 + yr / 400 - 32045
}

private fun epochDayToIso(epochDay: Long): String {
    val z = epochDay + 32044
    val g = z / 146097
    val dg = z % 146097
    val c = (dg / 36524 + 1) * 3 / 4
    val dc = dg - c * 36524
    val b = dc / 1461
    val db = dc % 1461
    val a = (db / 365 + 1) * 3 / 4
    val da = db - a * 365
    val y = g * 400 + c * 100 + b * 4 + a
    val mm = (da * 5 + 308) / 153 - 2
    val dd = da - (mm + 4) * 153 / 5 + 122
    val year = y - 4800 + (mm + 2) / 12
    val month = (mm + 2) % 12 + 1
    val day = dd + 1
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
