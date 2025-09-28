package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.WeightRepository
import com.emilflach.lokcal.health.HealthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Simple multiplatform ViewModel-like class to manage state for the Main screen.
 * It does not rely on Android-specific lifecycle components to keep it reusable across targets.
 */
class MainViewModel(
    private val intakeRepo: IntakeRepository,
    private val exerciseRepo: ExerciseRepository,
    private val weightRepo: WeightRepository,
    private val settingsRepo: com.emilflach.lokcal.data.SettingsRepository,
    initialDateIso: String
) {
    data class MealSummary(
        val mealType: String,
        val items: List<Intake>,
        val totalKcal: Double,
        val summaryText: String,
    )

    private val _summaries = MutableStateFlow<List<MealSummary>>(emptyList())
    val summaries: StateFlow<List<MealSummary>> = _summaries.asStateFlow()

    // Selected date for which data is displayed
    private val _selectedDate = MutableStateFlow(LocalDate.parse(initialDateIso))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Exercise card info for main screen
    private val _exerciseTotalKcal = MutableStateFlow(0.0)
    val exerciseTotalKcal: StateFlow<Double> = _exerciseTotalKcal.asStateFlow()
    private val _exerciseSummaryText = MutableStateFlow("")
    val exerciseSummaryText: StateFlow<String> = _exerciseSummaryText.asStateFlow()

    // Derived totals for the header
    private val _eatenKcal = MutableStateFlow(0.0)
    val eatenKcal: StateFlow<Double> = _eatenKcal.asStateFlow()

    private val _leftKcal = MutableStateFlow(1690.0)
    val leftKcal: StateFlow<Double> = _leftKcal.asStateFlow()

    private val _burnedKcal = MutableStateFlow(0.0)
    val burnedKcal: StateFlow<Double> = _burnedKcal.asStateFlow()

    // Thursday weight prompt visibility
    private val _showWeightPrompt = MutableStateFlow(false)
    val showWeightPrompt: StateFlow<Boolean> = _showWeightPrompt.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)


    init {
        loadFor(_selectedDate.value)
        if (HealthManager.arePermissionsGranted()) {
            fetchAndLogHealthData()
        }
    }

    private fun fetchAndLogHealthData() {
        viewModelScope.launch {
            val steps = HealthManager.readSteps()
            if (steps > -1) {
                exerciseRepo.logAutomaticSteps(steps)
                loadFor(_selectedDate.value)
            }
        }
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plus(1, DateTimeUnit.DAY)
        loadFor(_selectedDate.value)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.plus(-1, DateTimeUnit.DAY)
        loadFor(_selectedDate.value)
    }

    private fun loadFor(date: LocalDate) {
        val dateIso = date.toString()
        val startIso = "${dateIso}T00:00:00"
        val endIso = "${dateIso}T23:59:59"
        val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        _summaries.value = mealTypes.map { type ->
            val list = intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso)
            val totalKcal = list.sumOf { it.energy_kcal_total }
            MealSummary(
                mealType = type,
                items = list,
                totalKcal = totalKcal,
                summaryText = buildMealSummary(list)
            )
        }
        // Exercise summary
        val exercises = exerciseRepo.getByDateRange(startIso, endIso)
        _exerciseTotalKcal.value = exercises.sumOf { it.energy_kcal_total }
        _exerciseSummaryText.value = buildExerciseSummary(exercises.map { it.exercise_type to it.duration_min })

        // Update derived totals
        val eaten = _summaries.value.sumOf { it.totalKcal }.coerceAtLeast(0.0)
        val start = settingsRepo.getStartingKcal()
        val burned = _exerciseTotalKcal.value
        val totalBudget = start + burned
        val left = totalBudget - eaten
        _eatenKcal.value = eaten
        _burnedKcal.value = burned
        _leftKcal.value = left

        // Update Thursday weight prompt visibility
        val isThursday = date.dayOfWeek.name == "THURSDAY"
        val hasWeight = weightRepo.getForDate(dateIso) != null
        _showWeightPrompt.value = isThursday && !hasWeight
    }

    private fun buildMealSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        val counts = list.groupingBy { it.item_name }.eachCount().entries
            .sortedByDescending { it.value }
            .take(6)
        return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
    }

    private fun buildExerciseSummary(pairs: List<Pair<String, Double>>): String {
        if (pairs.isEmpty()) return ""
        val byType = pairs.groupBy { it.first }.mapValues { it.value.sumOf { p -> p.second } }
        val parts = byType.entries.sortedByDescending { it.value }.take(3).map { (type, min) ->
            val label = when (type) {
                "WALKING" -> "Walking"
                "RUNNING" -> "Running"
                "AUTOMATIC_STEPS" -> "Step counter"
                else -> type
            }
            "$label ${min.toInt()} min"
        }
        return parts.joinToString(", ")
    }
}
