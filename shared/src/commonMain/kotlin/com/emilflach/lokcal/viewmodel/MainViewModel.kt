package com.emilflach.lokcal.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.EggAlt
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.WeightRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.milliseconds

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
    val initialPage = 5000
    val pageCount = 10000

    private val _uiState = MutableStateFlow(
        MainUiState(selectedDate = LocalDate.parse(initialDateIso))
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _animationTrigger = MutableStateFlow(0)
    val animationTrigger: StateFlow<Int> = _animationTrigger.asStateFlow()

    private val viewModelScope = CoroutineScope(Dispatchers.Main)


    init {
        loadFor(_uiState.value.selectedDate)
        startPeriodicUpdates()
    }

    private fun startPeriodicUpdates() {
        viewModelScope.launch {
            while (isActive) {
                fetchAndLogHealthData()
                delay(10000.milliseconds)
                _animationTrigger.value++
            }
        }
    }

    fun fetchAndLogHealthData() {
        if(HealthManager.arePermissionsGranted()) {
            viewModelScope.launch {
                val steps = HealthManager.readSteps()
                if (steps > -1) {
                    exerciseRepo.logAutomaticSteps(steps)
                    loadFor(_uiState.value.selectedDate)
                }
            }
        }
    }

    @Suppress("unused") // Used in MainView.swift
    fun navigateToDate(dateIso: String) {
        loadFor(LocalDate.parse(dateIso))
    }

    @Suppress("unused") // Used in MainView.swift
    fun getSelectedDateIso(): String = _uiState.value.selectedDate.toString()

    fun onPageSelected(page: Int) {
        val today = LocalDate.parse(currentDateIso())
        val diff = page - initialPage
        val targetDate = today.plus(diff, DateTimeUnit.DAY)
        if (_uiState.value.selectedDate != targetDate) {
            loadFor(targetDate)
        }
    }

    fun getPageForDate(date: LocalDate): Int {
        val today = LocalDate.parse(currentDateIso())
        val diff = (date.toEpochDays() - today.toEpochDays()).toInt()
        return initialPage + diff
    }

    fun getDateForPage(page: Int): LocalDate {
        val today = LocalDate.parse(currentDateIso())
        val diff = page - initialPage
        return today.plus(diff, DateTimeUnit.DAY)
    }

    fun loadFor(date: LocalDate) {
        viewModelScope.launch {
            val dayState = getDayStateFor(date)
            val last7 = computeLast7Deltas(date)
            _uiState.value = _uiState.value.copy(
                selectedDate = date,
                dayState = dayState,
                last7Deltas = last7
            )
        }
    }

    suspend fun getDayStateFor(date: LocalDate): DayState {
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
        val percentageLeft = if (start > 0) (left / start) else if (left < 0) -1.0 else 0.0

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

    private suspend fun computeLast7Deltas(today: LocalDate): List<DayDelta> {
        val startKcal = settingsRepo.getStartingKcal().coerceAtLeast(0.0)
        val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        return (1..7).map { offset ->
            val d = today.plus(-offset, DateTimeUnit.DAY)
            val iso = d.toString()
            val startIso = "${iso}T00:00:00"
            val endIso = "${iso}T23:59:59"
            val eaten = mealTypes.sumOf { type ->
                intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso).sumOf { it.energy_kcal_total }
            }.coerceAtLeast(0.0)
            val burned = exerciseRepo.sumKcalByDate(startIso, endIso)
            val delta = if (eaten == 0.0 && burned == 0.0) 0.0 else (eaten - (startKcal + burned))
            DayDelta(date = d, deltaKcal = delta)
        }.reversed()
    }

    fun refresh() {
        loadFor(_uiState.value.selectedDate)
    }

    fun formattedDate(): String {
        val selectedDate = _uiState.value.selectedDate
        val isToday = selectedDate.toString() == currentDateIso()

        val weekDay =
            if (isToday) "Today" else selectedDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() }
        val month = selectedDate.month.name.take(3).lowercase().replaceFirstChar { it.titlecase() }

        return "$weekDay, ${selectedDate.day} $month"
    }
}
