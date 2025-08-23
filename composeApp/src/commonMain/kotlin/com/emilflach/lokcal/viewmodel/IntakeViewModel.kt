package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType, foods = emptyList()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Background scope for debounced searches to keep UI responsive
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        loadAddedSummary()
        launchSearchDebounced()
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        launchSearchDebounced()
    }

    private fun launchSearchDebounced() {
        searchJob?.cancel()
        val qSnapshot = _state.value.query
        searchJob = scope.launch {
            kotlinx.coroutines.delay(50)
            // If the query changed during delay, let the newer job handle it
            if (qSnapshot != _state.value.query) return@launch
            val foods = computeFoodsForQuery(qSnapshot)
            // Only apply if still relevant
            if (qSnapshot == _state.value.query) {
                _state.value = _state.value.copy(foods = foods)
            }
        }
    }

    private fun computeFoodsForQuery(q: String): List<Food> {
        return if (q.isBlank()) {
            val recent = intakeRepo.getRecentFoods(20)
            if (recent.size >= 20) {
                recent
            } else {
                val recentIds = recent.map { it.id }.toSet()
                val all = foodRepo.getAll()
                val remaining = all.filter { it.id !in recentIds }
                    .sortedBy { it.name.lowercase() }
                if (remaining.isEmpty()) recent else recent + remaining.take(20 - recent.size)
            }
        } else {
            foodRepo.search(q)
        }
    }

    private fun nowIso(): String = currentDateIso() + "T12:00:00"

    private fun todayRange(): Pair<String, String> {
        val date = currentDateIso()
        return "${date}T00:00:00" to "${date}T23:59:59"
    }

    private fun buildSummary(list: List<Intake>): String {
        if (list.isEmpty()) return ""
        // Group by item name to show total grams per item (more informative summary)
        val grouped = list.groupBy { it.item_name }
        val items = grouped.map { (name, entries) ->
            val totalG = entries.sumOf { it.quantity_g }
            name to totalG
        }.sortedByDescending { it.second }
            .take(3)
        return items.joinToString(", ") { (name, g) -> "$name ${g.toInt()} g" }
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
        val (startIso, endIso) = todayRange()
        val type = _state.value.selectedMealType
        val todayItems = intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso)
        val existing = todayItems.firstOrNull { it.source_type == "FOOD" && it.source_food_id == foodId }
        if (existing == null) {
            // No entry yet for this food today in this meal: create one
            intakeRepo.logFoodIntake(
                foodId = foodId,
                quantityG = portionG,
                timestamp = nowIso(),
                mealType = type,
                notes = null
            )
        } else {
            // Increase grams for the existing item
            val newQty = existing.quantity_g + portionG
            intakeRepo.updateIntakeQuantity(existing.id, newQty)
        }
        // Refresh the summary so the user immediately sees their addition reflected
        loadAddedSummary()
    }
}
