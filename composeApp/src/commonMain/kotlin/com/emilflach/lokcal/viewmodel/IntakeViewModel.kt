package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.util.NumberUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IntakeViewModel(
    private val foodRepo: FoodRepository,
    private val intakeRepo: IntakeRepository,
    initialMealType: String,
    private val dateIso: String,
) {
    data class UiState(
        val query: String = "",
        val meals: List<Meal> = emptyList(),
        val foods: List<Food> = emptyList(),
        val selectedMealType: String,
        val isSearchingOnline: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)
    private val openFoodFactsSearch = OpenFoodFactsSearch()
    private val openFoodFactsResults = mutableMapOf<Long, OffItem>()
    private var openFoodFactsTempId = -1L

    init {
        performSearch(state.value.selectedMealType)
    }

    fun setQuery(value: String) {
        _state.value = _state.value.copy(query = value)
        openFoodFactsResults.clear()
        openFoodFactsTempId = -1L
        performSearch(state.value.selectedMealType)
    }

    private fun performSearch(mealType: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            val (meals, foods) = if (_state.value.query.isBlank()) {
                getDefaultFoods(mealType)
            } else {
                intakeRepo.searchMeals(_state.value.query) to foodRepo.search(_state.value.query)
            }
            _state.value = _state.value.copy(meals = meals, foods = foods)
        }
    }

    private fun getDefaultFoods(mealType: String): Pair<List<Meal>, List<Food>> {
        val frequent = intakeRepo.getFrequentFoods(mealType, 20)
        val foods = if (frequent.size < 20) {
            val recentIds = frequent.map { it.id }.toSet()
            frequent + foodRepo.getAll()
                .filter { it.id !in recentIds }
                .sortedBy { it.name.lowercase() }
                .take(20 - frequent.size)
        } else {
            frequent
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
        intakeRepo.logOrUpdateFoodIntake(foodId, portionG, mealType(), dateIso)
    }

    fun logMealPortion(mealId: Long, portionG: Double) {
        intakeRepo.logOrUpdateMealIntake(mealId, portionG, mealType(), dateIso)
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
        if (grams <= 0.0) return

        // If this is an OpenFoodFacts item (negative ID), insert it first
        if (foodId < 0 && openFoodFactsResults.containsKey(foodId)) {
            val it = openFoodFactsResults[foodId] ?: return
            try {
                val newId = foodRepo.insertManual(
                    name = it.name,
                    brandName = null,
                    energyKcalPer100g = it.energyKcalPer100g ?: 0.0,
                    productUrl = it.productUrl,
                    imageUrl = it.imageUrl,
                    gtin13 = it.gtin13,
                    servingSize = if(it.servingSize != null)  it.servingSize.toString() else "100",
                    englishName = null,
                    dutchName = it.dutchName,
                    source = "manual",
                )
                logPortion(newId, grams)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            logPortion(foodId, grams)
            onSuccess()
        }

    }

    private fun mealType() = _state.value.selectedMealType

    fun searchOpenFoodFacts() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        _state.value = _state.value.copy(isSearchingOnline = true)
        scope.launch {
            try {
                val items = openFoodFactsSearch.search(q)
                openFoodFactsResults.clear()
                openFoodFactsTempId = -1L

                val transientFoods = items.map {
                    val tempId = openFoodFactsTempId--
                    openFoodFactsResults[tempId] = it
                    Food(
                        id = tempId,
                        name = it.name,
                        description = null,
                        brand = null,
                        category = null,
                        energy_kcal_per_100g = it.energyKcalPer100g ?: 0.0,
                        unit = "g",
                        external_id = null,
                        plural_name = null,
                        english_name = null,
                        dutch_name = it.dutchName,
                        brand_name = null,
                        serving_size = it.servingSize.toString(),
                        gtin13 = it.gtin13,
                        image_url = it.imageUrl,
                        product_url = it.productUrl,
                        source = "off",
                        label_id = null,
                        created_at_source = null,
                        updated_at_source = null,
                        on_hand = 0L,
                        raw_json = null,
                        created_at = ""
                    )
                }
                _state.value = _state.value.copy(foods = transientFoods, isSearchingOnline = false)
            } catch (_: Throwable) {}
        }
    }
}