package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.Meal
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
        val meals: List<Meal> = emptyList(),
        val foods: List<Food> = emptyList(),
        val selectedMealType: String,
        val addedCount: Int = 0,
        val addedTotalKcal: Double = 0.0,
        val addedSummaryText: String = "",
    )

    // --- UI helpers moved from IntakeScreen ---
    fun defaultPortionGrams(food: Food): Double {
        return food.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
    }

    fun defaultPortionGrams(meal: Meal): Double {
        // derived from total grams / total_portions via repository helper
        return intakeRepo.getMealPortionGrams(meal.id)
    }

    fun parseGrams(text: String): Double {
        return text
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    fun computeKcalFor(food: Food, totalGrams: Double): Double {
        return (food.energy_kcal_per_100g * totalGrams / 100.0)
    }

    fun buildSubtitle(food: Food, gramsText: String): String {
        val totalGrams = parseGrams(gramsText)
        val kcal = computeKcalFor(food, totalGrams)
        return "${totalGrams.toInt()} g • ${kcal.toInt()} kcal"
    }

    // --- Meal subtitle helpers ---
    private fun computeMealKcalFor(mealId: Long, totalGrams: Double): Double {
        val (totalG, totalKcal) = intakeRepo.computeMealTotals(mealId)
        if (totalG <= 0.0 || totalGrams <= 0.0) return 0.0
        val kcalPerGram = totalKcal / totalG
        return kcalPerGram * totalGrams
    }

    fun buildMealSubtitle(meal: Meal, portionsText: String): String {
        val portionG = defaultPortionGrams(meal)
        val portions = parseGrams(portionsText)
        val grams = (portionG * portions).coerceAtLeast(0.0)
        val kcal = computeMealKcalFor(meal.id, grams)
        return "${grams.toInt()} g • ${kcal.toInt()} kcal"
    }

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
            kotlinx.coroutines.delay(10)
            if (qSnapshot != _state.value.query) return@launch
            val (meals, foods) = computeFoodsAndMealsForQuery(qSnapshot)
            if (qSnapshot == _state.value.query) {
                _state.value = _state.value.copy(meals = meals, foods = foods)
            }
        }
    }

    private fun computeFoodsAndMealsForQuery(q: String): Pair<List<Meal>, List<Food>> {
        return if (q.isBlank()) {
            val recentFoods = intakeRepo.getRecentFoods(20)
            val foods = if (recentFoods.size >= 20) {
                recentFoods
            } else {
                val recentIds = recentFoods.map { it.id }.toSet()
                val all = foodRepo.getAll()
                val remaining = all.filter { it.id !in recentIds }
                    .sortedBy { it.name.lowercase() }
                if (remaining.isEmpty()) recentFoods else recentFoods + remaining.take(20 - recentFoods.size)
            }
            emptyList<Meal>() to foods
        } else {
            val meals = intakeRepo.searchMeals(q)
            val foods = foodRepo.search(q)
            meals to foods
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

    fun logMealPortion(mealId: Long, portionG: Double) {
        val (startIso, endIso) = todayRange()
        val type = _state.value.selectedMealType
        val todayItems = intakeRepo.getIntakeByMealAndDateRange(type, startIso, endIso)
        val existing = todayItems.firstOrNull { it.source_type == "MEAL" && it.source_meal_id == mealId }
        if (existing == null) {
            intakeRepo.logMealIntake(
                mealId = mealId,
                quantityG = portionG,
                timestamp = nowIso(),
                mealType = type,
                notes = null
            )
        } else {
            val newQty = existing.quantity_g + portionG
            intakeRepo.updateIntakeQuantity(existing.id, newQty)
        }
        loadAddedSummary()
    }
}
