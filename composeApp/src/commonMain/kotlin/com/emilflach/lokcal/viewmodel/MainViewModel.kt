package com.emilflach.lokcal.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.EggAlt
import androidx.compose.ui.graphics.vector.ImageVector
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SettingsRepository
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
    private val settingsRepo: SettingsRepository,
    initialDateIso: String
) {
    data class MealSummary(
        val mealType: String,
        val items: List<Intake>,
        val totalKcal: Double,
        val summaryText: String,
        val mealIcon: ImageVector
    )

    data class DayDelta(
        val date: LocalDate,
        val deltaKcal: Double // positive => over, negative => under
    )

    data class DayState(
        val summaries: List<MealSummary> = emptyList(),
        val percentageLeft: Double = 0.0,
        val leftKcal: Double = 0.0,
        val burnedKcal: Double = 0.0,
        val eatenKcal: Double = 0.0,
        val startingKcal: Double = 0.0,
        val showWeightPrompt: Boolean = false,
    )

    private val _summaries = MutableStateFlow<List<MealSummary>>(emptyList())
    val summaries: StateFlow<List<MealSummary>> = _summaries.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.parse(initialDateIso))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _percentageLeft = MutableStateFlow(0.0)
    val percentageLeft: StateFlow<Double> = _percentageLeft.asStateFlow()

    private val _leftKcal = MutableStateFlow(1690.0)
    val leftKcal: StateFlow<Double> = _leftKcal.asStateFlow()

    private val _burnedKcal = MutableStateFlow(0.0)
    val burnedKcal: StateFlow<Double> = _burnedKcal.asStateFlow()

    private val _eatenKcal = MutableStateFlow(0.0)
    val eatenKcal: StateFlow<Double> = _eatenKcal.asStateFlow()

    private val _startingKcal = MutableStateFlow(0.0)
    val startingKcal: StateFlow<Double> = _startingKcal.asStateFlow()

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

    fun setToCurrentDate() {
        _selectedDate.value = currentDateIso().let { LocalDate.parse(it) }
        loadFor(_selectedDate.value)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plus(1, DateTimeUnit.DAY)
        loadFor(_selectedDate.value)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.plus(-1, DateTimeUnit.DAY)
        loadFor(_selectedDate.value)
    }

    fun loadFor(date: LocalDate) {
        _selectedDate.value = date
        val state = getDayStateFor(date)
        _summaries.value = state.summaries
        _percentageLeft.value = state.percentageLeft
        _eatenKcal.value = state.eatenKcal
        _burnedKcal.value = state.burnedKcal
        _leftKcal.value = state.leftKcal
        _startingKcal.value = state.startingKcal
        _showWeightPrompt.value = state.showWeightPrompt

        computeLast7Deltas()
    }

    fun getDayStateFor(date: LocalDate): DayState {
        val dateIso = date.toString()
        val startIso = "${dateIso}T00:00:00"
        val endIso = "${dateIso}T23:59:59"
        val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        val summaries = mealTypes.map { type ->
            val list = intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso)
            val totalKcal = list.sumOf { it.energy_kcal_total }
            MealSummary(
                mealType = type,
                items = list,
                totalKcal = totalKcal,
                summaryText = buildMealSummary(list),
                mealIcon = getMealIcon(type)
            )
        }

        val exercises = exerciseRepo.getByDateRange(startIso, endIso)
        val eaten = summaries.sumOf { it.totalKcal }.coerceAtLeast(0.0)
        val start = settingsRepo.getStartingKcal().coerceAtLeast(0.0)
        val burned = exercises.sumOf { it.energy_kcal_total }
        val totalBudget = start + burned
        val left = totalBudget - eaten
        val percentageLeft = (left / start).coerceIn(0.0, 1.0)

        // Update Thursday weight prompt visibility
        val isThursday = date.dayOfWeek.name == "THURSDAY"
        val hasWeight = weightRepo.getForDate(dateIso) != null
        val showWeightPrompt = isThursday && !hasWeight

        return DayState(
            summaries = summaries,
            percentageLeft = percentageLeft,
            leftKcal = left,
            burnedKcal = burned,
            eatenKcal = eaten,
            startingKcal = start,
            showWeightPrompt = showWeightPrompt
        )
    }

    private fun buildMealSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        val counts = list.groupingBy { it.item_name }.eachCount().entries
            .sortedByDescending { it.value }
            .take(6)
        return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
    }

    private fun getMealIcon(mealType: String) = when (mealType) {
        "BREAKFAST" -> Icons.Outlined.EggAlt
        "LUNCH" -> Icons.Filled.LunchDining
        "DINNER" -> Icons.Filled.DinnerDining
        "SNACK" -> Icons.Filled.Cookie
        else -> Icons.Filled.Restaurant
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
            val delta = (eaten - (startKcal + burned))
            DayDelta(date = d, deltaKcal = delta)
        }.reversed()
        _last7Deltas.value = list
    }

    fun formattedDate(): String {
        val selectedDate = _selectedDate.value
        val isToday = selectedDate.toString() == currentDateIso()

        val weekDay =
            if (isToday) "Today" else selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
        val month = selectedDate.month.name.take(3).lowercase().replaceFirstChar { it.titlecase() }

        return "$weekDay, ${selectedDate.day} $month ${selectedDate.year}"
    }
}
