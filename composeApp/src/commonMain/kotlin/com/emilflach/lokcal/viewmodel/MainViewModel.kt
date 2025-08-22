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
    }

    private fun buildSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        val counts = list.groupingBy { it.item_name }.eachCount().entries
            .sortedByDescending { it.value }
            .take(3)
        return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
    }
}
