package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.util.PortionsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    )

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    init {
        performSearch()
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        performSearch()
    }

    private fun performSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            val (meals, foods) = if (_state.value.query.isBlank()) {
                getDefaultFoods()
            } else {
                intakeRepo.searchMeals(_state.value.query) to foodRepo.search(_state.value.query)
            }
            _state.value = _state.value.copy(meals = meals, foods = foods)
        }
    }

    private fun getDefaultFoods(): Pair<List<Meal>, List<Food>> {
        val recent = intakeRepo.getRecentFoods(20)
        val foods = if (recent.size < 20) {
            val recentIds = recent.map { it.id }.toSet()
            recent + foodRepo.getAll()
                .filter { it.id !in recentIds }
                .sortedBy { it.name.lowercase() }
                .take(20 - recent.size)
        } else {
            recent
        }
        return emptyList<Meal>() to foods
    }

    // UI helpers
    fun defaultPortionGrams(food: Food) = PortionsCalculator.defaultPortion(food.serving_size)
    fun defaultPortionGrams(meal: Meal) = intakeRepo.getMealPortionGrams(meal.id)
    fun parseGrams(text: String) = NumberUtils.parseDecimal(text)

    fun computeFoodKcal(food: Food, grams: Double): Double = food.energy_kcal_per_100g * grams / 100.0

    fun computeMealKcal(mealId: Long, grams: Double): Double {
        val (totalG, totalKcal) = intakeRepo.computeMealTotals(mealId)
        return if (totalG > 0.0 && grams > 0.0) totalKcal * (grams / totalG) else 0.0
    }

    fun subtitleForMeal(meal: Meal, initialPortions: String): String {
        val portionG = defaultPortionGrams(meal)
        val portions = parseGrams(initialPortions)
        val grams = (portionG * portions).coerceAtLeast(0.0)
        val kcal = computeMealKcal(meal.id, grams)
        return PortionsCalculator.subtitleKcalPortions(
            kcal = kcal,
            grams = grams,
            portionGrams = portionG
        )
    }

    fun subtitleForFood(food: Food, initialGrams: String): String {
        val grams = parseGrams(initialGrams).coerceAtLeast(0.0)
        return PortionsCalculator.subtitleKcalPortions(
            kcal = computeFoodKcal(food, grams),
            grams = grams,
            portionGrams = defaultPortionGrams(food)
        )
    }
    fun logPortion(foodId: Long, portionG: Double) {
        intakeRepo.logOrUpdateFoodIntake(foodId, portionG, mealType())
    }

    fun logMealPortion(mealId: Long, portionG: Double) {
        intakeRepo.logOrUpdateMealIntake(mealId, portionG, mealType())
    }

    private fun mealType() = _state.value.selectedMealType
}