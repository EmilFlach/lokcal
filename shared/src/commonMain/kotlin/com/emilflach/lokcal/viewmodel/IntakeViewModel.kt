package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.util.normalize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class SearchResult {
    data class FoodResult(val food: Food) : SearchResult()
    data class MealResult(val meal: Meal) : SearchResult()
}

class IntakeViewModel(
    private val foodRepo: FoodRepository,
    private val intakeRepo: IntakeRepository,
    private val mealRepo: MealRepository,
    private val settingsRepo: SettingsRepository,
    initialMealType: String,
    private val dateIso: String,
) {
    data class UiState(
        val query: String = "",
        val results: List<SearchResult> = emptyList(),
        val selectedMealType: String,
        val isSearchingOnline: Boolean = false,
        val sourceSections: List<OnlineSearchManager.SearchSection> = emptyList(),
        val gramsById: Map<Long, String> = emptyMap(),
        val showScanner: Boolean = false,
        val onlineSearchAttempted: Boolean = false,
        val showGlobalNoResults: Boolean = false,
        val sourcesConfigured: Boolean = true,
    ) {
        val showOnlineSearchSections: Boolean
            get() = sourceSections.any { it.foods.isNotEmpty() || it.isSearching || it.error != null } || (onlineSearchAttempted && sourceSections.all { it.noResults })
    }

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    // Online Search Manager
    private val onlineSearchManager = OnlineSearchManager(settingsRepo, scope)

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)

    init {
        performSearch(state.value.selectedMealType)
        scope.launch {
            _state.update { it.copy(sourcesConfigured = settingsRepo.getSourcePreferences().isNotEmpty()) }
        }
    }

    fun setQuery(value: String) {
        // Cancel any ongoing online searches when the query changes
        onlineSearchManager.clear()
        _state.update {
            it.copy(
                query = value,
                gramsById = emptyMap(),
                sourceSections = emptyList(),
                isSearchingOnline = false,
                onlineSearchAttempted = false,
                showGlobalNoResults = false,
            )
        }
        performSearch(state.value.selectedMealType)
    }

    fun setGrams(id: Long, grams: String) {
        _state.update { it.copy(gramsById = it.gramsById + (id to grams)) }
    }

    @Suppress("UNUSED") // Used by Swift
    fun refreshSourcesConfigured() {
        scope.launch {
            _state.update { it.copy(sourcesConfigured = settingsRepo.getSourcePreferences().isNotEmpty()) }
        }
    }

    fun setShowScanner(show: Boolean) {
        _state.update { it.copy(showScanner = show) }
    }

    private fun performSearch(mealType: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            val results = if (_state.value.query.isBlank()) {
                getDefaultFoods(mealType)
            } else {
                val foods = foodRepo.searchWithCounts(_state.value.query)
                val meals = mealRepo.searchMealsWithCounts(_state.value.query)
                val qNorm = normalize(_state.value.query.trim().lowercase())
                fun nameBonus(name: String): Int {
                    val n = normalize(name.lowercase())
                    return when {
                        n == qNorm -> 20
                        n.startsWith(qNorm) -> 10
                        else -> 0
                    }
                }
                val foodResults = foods.map { (food, count) ->
                    Pair(SearchResult.FoodResult(food), nameBonus(food.name) + count)
                }
                val mealResults = meals.map { (meal, count) ->
                    Pair(SearchResult.MealResult(meal), nameBonus(meal.name) + count)
                }
                (mealResults + foodResults).sortedByDescending { it.second }.map { it.first }
            }
            _state.update { it.copy(results = results) }
        }
    }

    private suspend fun getDefaultFoods(mealType: String): List<SearchResult> {
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
        return foods.map { SearchResult.FoodResult(it) }
    }

    fun defaultPortionGrams(food: Food) = portionService.defaultPortionForFood(food)
    suspend fun defaultPortionGrams(meal: Meal) = portionService.defaultPortionForMeal(meal.id)
    fun parseGrams(text: String) = NumberUtils.parseDecimal(text)

    suspend fun subtitleForMeal(meal: Meal, initialPortions: String): String {
        val portionG = defaultPortionGrams(meal)
        val portions = parseGrams(initialPortions)
        val grams = (portionG * portions).coerceAtLeast(0.0)
        return labelService.subtitleForMeal(meal.id, grams)
    }

    fun subtitleForFood(food: Food, initialGrams: String): String {
        val grams = parseGrams(initialGrams).coerceAtLeast(0.0)
        return labelService.subtitleForFood(food, grams)
    }

    fun logMealPortion(mealId: Long, portionG: Double) {
        scope.launch {
            intakeRepo.logOrUpdateMealIntake(mealId, portionG, mealType(), dateIso, refreshId = true)
        }
    }

    fun addMealByPortions(mealId: Long, portionsText: String, onSuccess: () -> Unit) {
        scope.launch {
            val portionG = portionService.defaultPortionForMeal(mealId)
            val portions = NumberUtils.parseDecimal(portionsText, min = 0.0)
            val grams = (portionG * portions).coerceAtLeast(0.0)
            if (grams > 0.0) {
                logMealPortion(mealId, grams)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    fun addFoodByGrams(foodId: Long, gramsText: String, onSuccess: () -> Unit) {
        val grams = NumberUtils.parseDecimal(gramsText, min = 0.0).coerceAtLeast(0.0)
        if (grams <= 0.0) return

        // Find source item from online search results if it's a transient item
        val sourceItem = onlineSearchManager.getSourceItem(foodId)
        val sourceId = onlineSearchManager.getSourceId(foodId)

        scope.launch {
            if (foodId < 0 && sourceItem != null) {
                try {
                    val newId = foodRepo.insertManual(
                        name = sourceItem.name,
                        energyKcalPer100g = sourceItem.energyKcalPer100g ?: 0.0,
                        servingSize = sourceItem.servingSize?.toString() ?: "100",
                        gtin13 = sourceItem.gtin13,
                        imageUrl = sourceItem.imageUrl,
                        productUrl = sourceItem.productUrl,
                        source = sourceId ?: "manual",
                    )
                    // Add Dutch name as alias if available
                    sourceItem.dutchName?.let { dutchName ->
                        if (dutchName.isNotBlank() && dutchName.lowercase() != sourceItem.name.lowercase()) {
                            foodRepo.addAlias(newId, dutchName, "locale:nl")
                        }
                    }
                    intakeRepo.logOrUpdateFoodIntake(newId, grams, mealType(), dateIso, refreshId = true)
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                intakeRepo.logOrUpdateFoodIntake(foodId, grams, mealType(), dateIso, refreshId = true)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    private fun mealType() = _state.value.selectedMealType

    fun searchOnline() {
        if (state.value.isSearchingOnline) {
            onlineSearchManager.cancel()
            updateOnlineState()
            return
        }

        _state.update { it.copy(onlineSearchAttempted = true) }
        onlineSearchManager.search(state.value.query) {
            updateOnlineState()
        }
    }

    private fun updateOnlineState() {
        _state.update {
            it.copy(
                isSearchingOnline = onlineSearchManager.isSearching,
                sourceSections = onlineSearchManager.sections,
                showGlobalNoResults = onlineSearchManager.showGlobalNoResults
            )
        }
    }
}
