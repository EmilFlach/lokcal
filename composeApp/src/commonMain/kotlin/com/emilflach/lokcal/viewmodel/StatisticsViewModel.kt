package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.StatsMostEatenByWeight
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class StatisticsViewModel(
    private val intakeRepo: IntakeRepository,
    private val exerciseRepo: ExerciseRepository,
    private val settingsRepo: SettingsRepository
) {
    data class DailyDelta(
        val day: String,
        val delta: Double
    )

    private val _mostEaten = MutableStateFlow<List<StatsMostEatenByWeight>>(emptyList())
    val mostEaten: StateFlow<List<StatsMostEatenByWeight>> = _mostEaten.asStateFlow()

    private val _dailyKcal = MutableStateFlow<List<DailyDelta>>(emptyList())
    val dailyKcal: StateFlow<List<DailyDelta>> = _dailyKcal.asStateFlow()

    private val _selectedMonths = MutableStateFlow(1)
    val selectedMonths: StateFlow<Int> = _selectedMonths.asStateFlow()

    init {
        loadStats()
    }

    fun setPeriod(months: Int) {
        _selectedMonths.value = months
        loadStats()
    }

    private fun loadStats() {
        val endDate = LocalDate.parse(currentDateIso())
        val startDate = endDate.minus(_selectedMonths.value, DateTimeUnit.MONTH)
        
        val startIso = "${startDate}T00:00:00"
        val endIso = "${endDate}T23:59:59"

        val eatenPerDay = intakeRepo.getDailyKcal(startIso, endIso)
        val burnedPerDay = exerciseRepo.getDailyBurned(startIso, endIso)
        val startingKcal = settingsRepo.getStartingKcal().coerceAtLeast(0.0)

        // Merge datasets
        val allDays = mutableSetOf<String>()
        eatenPerDay.forEach { row -> row.day.let { allDays.add(it) } }
        burnedPerDay.forEach { row -> row.day.let { allDays.add(it) } }

        val sortedDays = allDays.sorted()
        val deltas = sortedDays.map { day ->
            val eaten = eatenPerDay.find { it.day == day }?.total_kcal ?: 0.0
            val burned = burnedPerDay.find { it.day == day }?.total_burned ?: 0.0
            val delta = (eaten - (startingKcal + burned))
            DailyDelta(day = day, delta = delta)
        }

        _mostEaten.value = intakeRepo.getMostEatenByWeight(startIso, endIso)
        _dailyKcal.value = deltas
    }
}
