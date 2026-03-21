package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.LabelService
import com.emilflach.lokcal.data.OnlineFoodItem
import com.emilflach.lokcal.data.PortionService
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.scraper.AlbertHeijnFoodSource
import com.emilflach.lokcal.data.scraper.FoodSource
import com.emilflach.lokcal.data.scraper.OpenFoodFactsFoodSource
import com.emilflach.lokcal.data.scraper.RateLimiter
import com.emilflach.lokcal.data.scraper.SourceRegistry
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
    private val settingsRepo: SettingsRepository,
    initialMealType: String,
    private val dateIso: String,
) {
    data class SearchSection(
        val scraperId: String = "",
        val scraperName: String = "",
        val foods: List<Food> = emptyList(),
        val isSearching: Boolean = false,
        val error: String? = null,
        val noResults: Boolean = false,
        val remainingCooldown: Int = 0
    )

    data class UiState(
        val query: String = "",
        val meals: List<Meal> = emptyList(),
        val foods: List<Food> = emptyList(),
        val selectedMealType: String,
        val isSearchingOnline: Boolean = false,
        val scraperSections: List<SearchSection> = emptyList(),
        val gramsById: Map<Long, String> = emptyMap(),
        val showScanner: Boolean = false,
    ) {
        val showOnlineSearchSections: Boolean
            get() = scraperSections.any { it.foods.isNotEmpty() || it.isSearching }
    }

    private val _state = MutableStateFlow(UiState(selectedMealType = initialMealType))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null
    private var onlineSearchJob: Job? = null

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)

    // Source registry and rate limiting
    private val sourceRegistry = SourceRegistry().apply {
        register(AlbertHeijnFoodSource())
        register(OpenFoodFactsFoodSource())
    }
    private val rateLimiter = RateLimiter()
    private val scraperResults = mutableMapOf<String, MutableMap<Long, OnlineFoodItem>>()
    private val scraperTempIds = mutableMapOf<String, Long>()

    init {
        performSearch(state.value.selectedMealType)
    }

    fun setQuery(value: String) {
        // Cancel any ongoing online searches when the query changes
        cancelOnlineSearch()
        _state.value = _state.value.copy(
            query = value,
            gramsById = emptyMap(),
            scraperSections = emptyList(),
            isSearchingOnline = false,
        )
        scraperResults.clear()
        scraperTempIds.clear()
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

        // Find source item from any scraper results
        val sourceItem = scraperResults.values.firstNotNullOfOrNull { it[foodId] }
        val sourceScraperId = scraperResults.entries.firstOrNull { it.value.containsKey(foodId) }?.key

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
                        source = sourceScraperId ?: "manual",
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

        scope.launch {
            val sources = getPreferredSources()
            if (sources.isEmpty()) return@launch

            // Initialize sections with rate limit info
            val sections = sources.map { source ->
                val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
                SearchSection(
                    scraperId = source.id,
                    scraperName = source.displayName,
                    isSearching = cooldown == 0,
                    remainingCooldown = cooldown
                )
            }

            _state.value = _state.value.copy(
                isSearchingOnline = true,
                scraperSections = sections
            )

            // Clear transient maps
            scraperResults.clear()
            scraperTempIds.clear()

            // Cancel any previous job
            onlineSearchJob?.cancel()
            onlineSearchJob = scope.launch {
                try {
                    val jobs = sources.mapIndexed { index, source ->
                        launch {
                            searchWithSource(source, q, index)
                        }
                    }
                    jobs.forEach { it.join() }
                } catch (_: CancellationException) {
                    // Search was cancelled
                    _state.value = _state.value.copy(
                        isSearchingOnline = false,
                        scraperSections = _state.value.scraperSections.map { it.copy(isSearching = false) }
                    )
                } catch (_: Throwable) {
                    _state.value = _state.value.copy(
                        isSearchingOnline = false,
                        scraperSections = _state.value.scraperSections.map { it.copy(isSearching = false) }
                    )
                }
            }
        }
    }

    private suspend fun searchWithSource(source: FoodSource, query: String, sectionIndex: Int) {
        // Check rate limit
        if (!rateLimiter.canRequest(source.id, source.rateLimitSeconds)) {
            val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
            updateSourceSection(sectionIndex) {
                it.copy(
                    isSearching = false,
                    error = "Please wait $cooldown seconds",
                    remainingCooldown = cooldown
                )
            }
            return
        }

        try {
            // Record request and perform search
            rateLimiter.recordRequest(source.id)
            val items = withContext(Dispatchers.Default) { source.search(query) }

            // Get or initialize temp ID counter for this source
            val startId = scraperTempIds.getOrPut(source.id) { -100000L * (sectionIndex + 1) }
            val results = scraperResults.getOrPut(source.id) { mutableMapOf() }

            val transient = items.map { item ->
                val tempId = scraperTempIds[source.id]!! - 1
                scraperTempIds[source.id] = tempId
                results[tempId] = item
                item.toFood(tempId, source.id)
            }

            updateSourceSection(sectionIndex) {
                it.copy(
                    foods = transient,
                    isSearching = false,
                    noResults = transient.isEmpty()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            updateSourceSection(sectionIndex) {
                it.copy(
                    isSearching = false,
                    error = "Could not connect to ${source.displayName}"
                )
            }
        } finally {
            checkSearchFinished()
        }
    }

    private fun updateSourceSection(index: Int, update: (SearchSection) -> SearchSection) {
        val sections = _state.value.scraperSections.toMutableList()
        if (index in sections.indices) {
            sections[index] = update(sections[index])
            _state.value = _state.value.copy(scraperSections = sections)
        }
    }

    private fun checkSearchFinished() {
        if (_state.value.scraperSections.all { !it.isSearching }) {
            _state.value = _state.value.copy(isSearchingOnline = false)
        }
    }

    private suspend fun getPreferredSources(): List<FoodSource> {
        val prefs = settingsRepo.getSourcePreferences()
        return if (prefs.isNotEmpty()) {
            sourceRegistry.getByIds(prefs)
        } else {
            // Default: use only OpenFoodFacts
            sourceRegistry.getById("off")?.let { listOf(it) } ?: emptyList()
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
            scraperSections = emptyList(),
        )
    }
}