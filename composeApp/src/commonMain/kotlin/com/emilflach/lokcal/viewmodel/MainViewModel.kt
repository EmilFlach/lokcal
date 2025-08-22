package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple multiplatform ViewModel-like class to manage state for the Main screen.
 * It does not rely on Android-specific lifecycle components to keep it reusable across targets.
 */
class MainViewModel(private val intakeRepo: IntakeRepository) {
    data class MealSummary(
        val mealType: String,
        val items: List<Intake>,
        val totalKcal: Double,
        val summaryText: String,
    )

    private val _summaries = MutableStateFlow<List<MealSummary>>(emptyList())
    val summaries: StateFlow<List<MealSummary>> = _summaries.asStateFlow()

    // Derived totals for the header
    private val _eatenKcal = MutableStateFlow(0.0)
    val eatenKcal: StateFlow<Double> = _eatenKcal.asStateFlow()

    private val _leftKcal = MutableStateFlow(1690.0)
    val leftKcal: StateFlow<Double> = _leftKcal.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    init {
        loadToday()
    }

    fun loadToday() {
        val date = currentDateIso()
        val startIso = "${date}T00:00:00"
        val endIso = "${date}T23:59:59"
        val entries = intakeRepo.getIntakeByDateRange(startIso, endIso)
        val groups = entries.groupBy { it.meal_type }
        val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        _summaries.value = mealTypes.map { type ->
            val list = groups[type].orEmpty()
            val totalKcal = list.sumOf { it.energy_kcal_total }
            MealSummary(
                mealType = type,
                items = list,
                totalKcal = totalKcal,
                summaryText = buildSummary(list)
            )
        }
        // Update derived totals
        val eaten = _summaries.value.sumOf { it.totalKcal }.coerceAtLeast(0.0)
        val start = 1690.0
        val left = start - eaten
        val prog = if (eaten + left > 0) (eaten / (eaten + left)).toFloat() else 0f
        _eatenKcal.value = eaten
        _leftKcal.value = left
        _progress.value = prog
    }

    private fun buildSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        val counts = list.groupingBy { it.item_name }.eachCount().entries
            .sortedByDescending { it.value }
            .take(3)
        return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
    }
}
