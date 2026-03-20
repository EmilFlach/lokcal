package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.AlbertHeijnSearch
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.LabelService
import com.emilflach.lokcal.data.OnlineFoodItem
import com.emilflach.lokcal.data.OpenFoodFactsSearch
import com.emilflach.lokcal.data.PortionService
import com.emilflach.lokcal.util.NumberUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IntakeViewModel(
    private val foodRepo: FoodRepository,
    private val intakeRepo: IntakeRepository,
    initialMealType: String,
    private val dateIso: String,
) {
    data class SearchSection(
        val foods: List<Food> = emptyList(),
        val isSearching: Boolean = false,
        val error: String? = null,
        val noResults: Boolean = false
    )

    data class UiState(
        val query: String = "",
        val meals: List<Meal> = emptyList(),
        val foods: List<Food> = emptyList(),
        val selectedMealType: String,
        val isSearchingOnline: Boolean = false,
        val ahSection: SearchSection = SearchSection(),
        val offSection: SearchSection = SearchSection(),
        val gramsById: Map<Long, String> = emptyMap(),
        val showScanner: Boolean = false,
    ) {
        val showOnlineSearchSections: Boolean
            get() = ahSection.foods.isNotEmpty() || offSection.foods.isNotEmpty() || ahSection.isSearching || offSection.isSearching
    }

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null
    private var onlineSearchJob: Job? = null

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)
    private val openFoodFactsSearch = OpenFoodFactsSearch()
    private val openFoodFactsResults = mutableMapOf<Long, OnlineFoodItem>()
    private var openFoodFactsTempId = -200000L
    private val albertHeijnSearch = AlbertHeijnSearch()
    private val albertHeijnResults = mutableMapOf<Long, OnlineFoodItem>()
    private var albertHeijnTempId = -100000L

    init {
        performSearch(state.value.selectedMealType)
    }

    fun setQuery(value: String) {
        // Cancel any ongoing online searches when the query changes
        cancelOnlineSearch()
        _state.value = _state.value.copy(
            query = value,
            gramsById = emptyMap(),
            ahSection = SearchSection(),
            offSection = SearchSection(),
            isSearchingOnline = false,
        )
        openFoodFactsResults.clear()
        albertHeijnResults.clear()
        albertHeijnTempId = -100000L
        openFoodFactsTempId = -200000L
        performSearch(state.value.selectedMealType)
    }

    fun setGrams(id: Long, grams: String) {
        val newMap = _state.value.gramsById.toMutableMap()
        newMap[id] = grams
        _state.value = _state.value.copy(gramsById = newMap)
    }

    fun setShowScanner(show: Boolean) {
        _state.value = _state.value.copy(showScanner = show)
    }

    private fun performSearch(mealType: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            val trackingCounts = intakeRepo.getTrackingCounts()
            val (meals, foods) = if (_state.value.query.isBlank()) {
                getDefaultFoods(mealType)
            } else {
                val foodTracking = trackingCounts.filterKeys { it.first == "FOOD" }.mapKeys { it.key.second }
                val mealTracking = trackingCounts.filterKeys { it.first == "MEAL" }.mapKeys { it.key.second }
                intakeRepo.searchMeals(_state.value.query, mealTracking) to foodRepo.search(_state.value.query, foodTracking)
            }
            _state.value = _state.value.copy(meals = meals, foods = foods)
        }
    }

    private suspend fun getDefaultFoods(mealType: String): Pair<List<Meal>, List<Food>> {
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

    fun logPortion(foodId: Long, portionG: Double) {
        scope.launch {
            intakeRepo.logOrUpdateFoodIntake(foodId, portionG, mealType(), dateIso, refreshId = true)
        }
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

        val sourceItem = openFoodFactsResults[foodId] ?: albertHeijnResults[foodId]
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
                        source = if (albertHeijnResults.containsKey(foodId)) "ah" else "manual",
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
        val q = _state.value.query.trim()
        if (q.isEmpty()) return
        // If already searching, treat as cancel action
        if (_state.value.isSearchingOnline) {
            cancelOnlineSearch()
            return
        }
        _state.value = _state.value.copy(
            isSearchingOnline = true,
            ahSection = SearchSection(isSearching = true),
            offSection = SearchSection(isSearching = true),
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
                            it.toFood(tempId, "off")
                        }
                        _state.value = _state.value.copy(
                            offSection = SearchSection(
                                foods = offTransient,
                                isSearching = false,
                                noResults = offTransient.isEmpty()
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        _state.value = _state.value.copy(
                            offSection = _state.value.offSection.copy(
                                isSearching = false,
                                error = "Could not connect to OpenFoodFacts"
                            )
                        )
                    } finally {
                        checkSearchFinished()
                    }
                }

                // Launch AH search
                val ahJob = launch {
                    try {
                        val ahItems = withContext(Dispatchers.Default) { albertHeijnSearch.search(q) }
                        val ahTransient = ahItems.map {
                            val tempId = albertHeijnTempId--
                            albertHeijnResults[tempId] = it
                            it.toFood(tempId, "ah")
                        }
                        _state.value = _state.value.copy(
                            ahSection = SearchSection(
                                foods = ahTransient,
                                isSearching = false,
                                noResults = ahTransient.isEmpty()
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        _state.value = _state.value.copy(
                            ahSection = _state.value.ahSection.copy(
                                isSearching = false,
                                error = "Could not connect to Albert Heijn"
                            )
                        )
                    } finally {
                        checkSearchFinished()
                    }
                }

                // Wait for both jobs to finish (or be cancelled)
                try {
                    offJob.join()
                    ahJob.join()
                } catch (_: CancellationException) {
                }
            } catch (_: CancellationException) {
                // Search was cancelled: just turn off the loading flags, keep any partial results
                _state.value = _state.value.copy(
                    isSearchingOnline = false,
                    ahSection = _state.value.ahSection.copy(isSearching = false),
                    offSection = _state.value.offSection.copy(isSearching = false),
                )
            } catch (_: Throwable) {
                _state.value = _state.value.copy(
                    isSearchingOnline = false,
                    ahSection = _state.value.ahSection.copy(isSearching = false),
                    offSection = _state.value.offSection.copy(isSearching = false),
                )
            }
        }
    }

    private fun checkSearchFinished() {
        if (!_state.value.ahSection.isSearching && !_state.value.offSection.isSearching) {
            _state.value = _state.value.copy(isSearchingOnline = false)
        }
    }

    private fun OnlineFoodItem.toFood(tempId: Long, source: String) = Food(
        id = tempId,
        name = name,
        energy_kcal_per_100g = energyKcalPer100g ?: 0.0,
        unit = "g",
        serving_size = servingSize?.toString(),
        gtin13 = gtin13,
        image_url = imageUrl,
        product_url = productUrl,
        source = source,
        created_at = ""
    )

    private fun cancelOnlineSearch() {
        onlineSearchJob?.cancel()
        onlineSearchJob = null
        _state.value = _state.value.copy(
            isSearchingOnline = false,
            ahSection = SearchSection(),
            offSection = SearchSection(),
        )
    }
}