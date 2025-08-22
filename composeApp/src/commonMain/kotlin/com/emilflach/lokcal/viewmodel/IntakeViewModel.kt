package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IntakeViewModel(
    private val foodRepo: FoodRepository,
    private val intakeRepo: IntakeRepository,
    initialMealType: String,
) {
    data class UiState(
        val query: String = "",
        val foods: List<Food> = emptyList(),
        val selectedMealType: String,
        val addedCount: Int = 0,
        val addedTotalKcal: Double = 0.0,
        val addedSummaryText: String = "",
    )

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType, foods = foodRepo.getAll()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadAddedSummary()
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        refreshSearch()
    }

    fun selectMealType(type: String) {
        _state.value = _state.value.copy(selectedMealType = type)
        loadAddedSummary()
    }

    fun refreshSearch() {
        val q = _state.value.query
        val foods = if (q.isBlank()) foodRepo.getAll() else foodRepo.search(q)
        _state.value = _state.value.copy(foods = foods)
    }

    private fun nowIso(): String = currentDateIso() + "T12:00:00"

    private fun todayRange(): Pair<String, String> {
        val date = currentDateIso()
        return "${date}T00:00:00" to "${date}T23:59:59"
    }

    private fun buildSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        val counts = list.groupingBy { it.item_name }.eachCount().entries
            .sortedByDescending { it.value }
            .take(3)
        return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
    }

    fun loadAddedSummary() {
        val (startIso, endIso) = todayRange()
        val type = _state.value.selectedMealType
        val items = intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso)
        val totalKcal = items.sumOf { it.energy_kcal_total }
        val summary = buildSummary(items)
        _state.value = _state.value.copy(
            addedCount = items.size,
            addedTotalKcal = totalKcal,
            addedSummaryText = summary,
        )
    }

    fun logPortion(foodId: Long, portionG: Double) {
        intakeRepo.logFoodIntake(
            foodId = foodId,
            quantityG = portionG,
            timestamp = nowIso(),
            mealType = _state.value.selectedMealType,
            notes = null
        )
        // Refresh the summary so the user immediately sees their addition reflected
        loadAddedSummary()
    }
}
