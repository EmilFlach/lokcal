package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.WeightRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.util.currentDateIso
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

    data class DayDelta(
        val date: LocalDate,
        val deltaKcal: Double // positive => over, negative => under
    )

    private val _summaries = MutableStateFlow<List<MealSummary>>(emptyList())
    val summaries: StateFlow<List<MealSummary>> = _summaries.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.parse(initialDateIso))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedDateIsToday = MutableStateFlow(initialDateIso == currentDateIso())
    val selectedDateIsToday: StateFlow<Boolean> = _selectedDateIsToday.asStateFlow()

    private val _percentageLeft = MutableStateFlow(0.0)
    val percentageLeft: StateFlow<Double> = _percentageLeft.asStateFlow()

    private val _leftKcal = MutableStateFlow(1690.0)
    val leftKcal: StateFlow<Double> = _leftKcal.asStateFlow()

    private val _burnedKcal = MutableStateFlow(0.0)
    val burnedKcal: StateFlow<Double> = _burnedKcal.asStateFlow()

    // Thursday weight prompt visibility
    private val _showWeightPrompt = MutableStateFlow(false)
    val showWeightPrompt: StateFlow<Boolean> = _showWeightPrompt.asStateFlow()

    private val _last7Deltas = MutableStateFlow<List<DayDelta>>(emptyList())
    val last7Deltas: StateFlow<List<DayDelta>> = _last7Deltas.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)


    init {
        loadFor(_selectedDate.value)
    }

    fun fetchAndLogHealthData() {
        if(HealthManager.arePermissionsGranted()) {
            viewModelScope.launch {
                val steps = HealthManager.readSteps()
                if (steps > -1) {
                    exerciseRepo.logAutomaticSteps(steps)
                    loadFor(_selectedDate.value)
                }
            }
        }
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plus(1, DateTimeUnit.DAY)
        _selectedDateIsToday.value = _selectedDate.value.toString() == currentDateIso()
        loadFor(_selectedDate.value)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.plus(-1, DateTimeUnit.DAY)
        _selectedDateIsToday.value = _selectedDate.value.toString() == currentDateIso()
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

        val exercises = exerciseRepo.getByDateRange(startIso, endIso)
        val eaten = _summaries.value.sumOf { it.totalKcal }.coerceAtLeast(0.0)
        val start = settingsRepo.getStartingKcal().coerceAtLeast(0.0)
        val burned = exercises.sumOf { it.energy_kcal_total }
        val totalBudget = start + burned
        val left = totalBudget - eaten
        _percentageLeft.value = (left / start).coerceIn(0.0, 1.0)
        _burnedKcal.value = burned
        _leftKcal.value = left

        // Update Thursday weight prompt visibility
        val isThursday = date.dayOfWeek.name == "THURSDAY"
        val hasWeight = weightRepo.getForDate(dateIso) != null
        _showWeightPrompt.value = isThursday && !hasWeight

        computeLast7Deltas()
    }

    private fun buildMealSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        val counts = list.groupingBy { it.item_name }.eachCount().entries
            .sortedByDescending { it.value }
            .take(6)
        return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
    }

    private fun computeLast7Deltas() {
        val today = LocalDate.parse(selectedDate.value.toString())
        val startKcal = settingsRepo.getStartingKcal().coerceAtLeast(0.0)
        val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        val list = (1..7).map { offset ->
            val d = today.plus(-offset, DateTimeUnit.DAY)
            val iso = d.toString()
            val startIso = "${iso}T00:00:00"
            val endIso = "${iso}T23:59:59"
            val eaten = mealTypes.sumOf { type ->
                intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso).sumOf { it.energy_kcal_total }
            }.coerceAtLeast(0.0)
            val burned = exerciseRepo.sumKcalByDate(startIso, endIso)
            val delta = (eaten - (startKcal + burned)) * -1
            DayDelta(date = d, deltaKcal = delta)
        }.reversed()
        _last7Deltas.value = list
    }
}
