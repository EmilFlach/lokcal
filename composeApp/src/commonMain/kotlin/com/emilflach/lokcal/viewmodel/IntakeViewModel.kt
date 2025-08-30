package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.LabelService
import com.emilflach.lokcal.data.PortionService
import com.emilflach.lokcal.util.NumberUtils
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

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)

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
    fun defaultPortionGrams(food: Food) = portionService.defaultPortionForFood(food)
    fun defaultPortionGrams(meal: Meal) = portionService.defaultPortionForMeal(meal.id)
    fun parseGrams(text: String) = NumberUtils.parseDecimal(text)

    fun subtitleForMeal(meal: Meal, initialPortions: String): String {
        val portionG = defaultPortionGrams(meal)
        val portions = parseGrams(initialPortions)
        val grams = (portionG * portions).coerceAtLeast(0.0)
        return labelService.subtitleForMeal(meal.id, grams)
    }

    fun subtitleForFood(food: Food, initialGrams: String): String {
        val grams = parseGrams(initialGrams).coerceAtLeast(0.0)
        return labelService.subtitleForFood(food, grams)
    }

    fun logPortion(foodId: Long, portionG: Double) {
        intakeRepo.logOrUpdateFoodIntake(foodId, portionG, mealType())
    }

    fun logMealPortion(mealId: Long, portionG: Double) {
        intakeRepo.logOrUpdateMealIntake(mealId, portionG, mealType())
    }

    // Moved UI business logic here
    fun addMealByPortions(mealId: Long, portionsText: String, onSuccess: () -> Unit) {
        val portionG = portionService.defaultPortionForMeal(mealId)
        val portions = NumberUtils.parseDecimal(portionsText, min = 0.0)
        val grams = (portionG * portions).coerceAtLeast(0.0)
        if (grams > 0.0) {
            logMealPortion(mealId, grams)
            onSuccess()
        }
    }

    fun addFoodByGrams(foodId: Long, gramsText: String, onSuccess: () -> Unit) {
        val grams = NumberUtils.parseDecimal(gramsText, min = 0.0).coerceAtLeast(0.0)
        if (grams > 0.0) {
            logPortion(foodId, grams)
            onSuccess()
        }
    }

    private fun mealType() = _state.value.selectedMealType
}