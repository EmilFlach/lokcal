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
        // Separate online results and loading flags
        val ahFoods: List<Food> = emptyList(),
        val offFoods: List<Food> = emptyList(),
        val isSearchingAh: Boolean = false,
        val isSearchingOff: Boolean = false,
        // Errors and empty-state flags per section
        val ahError: String? = null,
        val offError: String? = null,
        val ahNoResults: Boolean = false,
        val offNoResults: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null
    private var onlineSearchJob: Job? = null

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)
    private val openFoodFactsSearch = OpenFoodFactsSearch()
    private val openFoodFactsResults = mutableMapOf<Long, OffItem>()
    private var openFoodFactsTempId = -200000L
    private val albertHeijnSearch = AlbertHeijnSearch()
    private val albertHeijnResults = mutableMapOf<Long, OffItem>()
    private var albertHeijnTempId = -100000L

    init {
        performSearch(state.value.selectedMealType)
    }

    fun setQuery(value: String) {
        // Cancel any ongoing online searches when the query changes
        cancelOnlineSearch()
        _state.value = _state.value.copy(query = value)
        openFoodFactsResults.clear()
        albertHeijnResults.clear()
        albertHeijnTempId = -100000L
        openFoodFactsTempId = -200000L
        // Clear sectioned online results when query changes
        _state.value = _state.value.copy(
            ahFoods = emptyList(),
            offFoods = emptyList(),
            isSearchingAh = false,
            isSearchingOff = false,
            isSearchingOnline = false,
            ahError = null,
            offError = null,
            ahNoResults = false,
            offNoResults = false,
        )
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
        } else if (foodId < 0 && albertHeijnResults.containsKey(foodId)) {
            val it = albertHeijnResults[foodId] ?: return
            try {
                val newId = foodRepo.insertManual(
                    name = it.name,
                    brandName = null,
                    energyKcalPer100g = it.energyKcalPer100g ?: 0.0,
                    productUrl = it.productUrl,
                    imageUrl = it.imageUrl,
                    gtin13 = it.gtin13,
                    servingSize = if (it.servingSize != null) it.servingSize.toString() else "100",
                    englishName = null,
                    dutchName = it.dutchName,
                    source = "ah",
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

    fun searchOnline() {
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        // If already searching, treat as cancel action
        if (_state.value.isSearchingOnline) {
            cancelOnlineSearch()
            return
        }
        _state.value = _state.value.copy(
            isSearchingOnline = true,
            isSearchingAh = true,
            isSearchingOff = true,
            ahFoods = emptyList(),
            offFoods = emptyList(),
            ahError = null,
            offError = null,
            ahNoResults = false,
            offNoResults = false,
        )
        // Cancel any previous job just in case
        onlineSearchJob?.cancel()
        onlineSearchJob = scope.launch {
            try {
                // Clear transient maps and counters
                openFoodFactsResults.clear()
                albertHeijnResults.clear()

                // Launch OFF search
                val offJob = launch {
                    try {
                        val offItems = withContext(Dispatchers.Default) { openFoodFactsSearch.search(q) }
                        val offTransient = offItems.map {
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
                                serving_size = it.servingSize?.toString(),
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
                        _state.value = _state.value.copy(
                            offFoods = offTransient,
                            isSearchingOff = false,
                            offError = null,
                            offNoResults = offTransient.isEmpty()
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        // Distinguish timeout/connectivity from other errors if desired; for now show connection error
                        _state.value = _state.value.copy(
                            isSearchingOff = false,
                            offError = "Could not connect to OpenFoodFacts",
                        )
                    } finally {
                        if (!_state.value.isSearchingAh) {
                            _state.value = _state.value.copy(isSearchingOnline = false)
                        }
                    }
                }

                // Launch AH search
                val ahJob = launch {
                    try {
                        val ahItems = withContext(Dispatchers.Default) { albertHeijnSearch.search(q) }
                        val ahTransient = ahItems.map {
                            val tempId = albertHeijnTempId--
                            albertHeijnResults[tempId] = it
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
                                serving_size = it.servingSize?.toString(),
                                gtin13 = it.gtin13,
                                image_url = it.imageUrl,
                                product_url = it.productUrl,
                                source = "ah",
                                label_id = null,
                                created_at_source = null,
                                updated_at_source = null,
                                on_hand = 0L,
                                raw_json = null,
                                created_at = ""
                            )
                        }
                        _state.value = _state.value.copy(
                            ahFoods = ahTransient,
                            isSearchingAh = false,
                            ahError = null,
                            ahNoResults = ahTransient.isEmpty()
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        _state.value = _state.value.copy(
                            isSearchingAh = false,
                            ahError = "Could not connect to Albert Heijn",
                        )
                    } finally {
                        if (!_state.value.isSearchingOff) {
                            _state.value = _state.value.copy(isSearchingOnline = false)
                        }
                    }
                }

                // Wait for both jobs to finish (or be cancelled)
                try {
                    offJob.join()
                    ahJob.join()
                } catch (_: CancellationException) {
                    // Propagate cancellation handling below
                }
            } catch (_: CancellationException) {
                // Search was cancelled: just turn off the loading flags, keep any partial results
                _state.value = _state.value.copy(
                    isSearchingOnline = false,
                    isSearchingAh = false,
                    isSearchingOff = false,
                )
            } catch (_: Throwable) {
                _state.value = _state.value.copy(
                    isSearchingOnline = false,
                    isSearchingAh = false,
                    isSearchingOff = false,
                )
            }
        }
    }

    private fun cancelOnlineSearch() {
        onlineSearchJob?.cancel()
        onlineSearchJob = null
        _state.value = _state.value.copy(
            isSearchingOnline = false,
            isSearchingAh = false,
            isSearchingOff = false,
            ahError = null,
            offError = null,
            ahNoResults = false,
            offNoResults = false,
        )
    }
}