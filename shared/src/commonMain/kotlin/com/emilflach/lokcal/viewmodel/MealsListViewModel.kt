package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.AllItemFrequencies
import com.emilflach.lokcal.ItemsMissingImage
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.IntakeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MealsListViewModel(
    private val intakeRepo: IntakeRepository
) {
    enum class Tab {
        ALL, MISSING_IMAGES
    }

    private val _selectedTab = MutableStateFlow(Tab.ALL)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _itemFrequencies = MutableStateFlow<Map<Pair<String, Long>, Long>>(emptyMap())
    val itemFrequencies: StateFlow<Map<Pair<String, Long>, Long>> = _itemFrequencies.asStateFlow()

    private val _missingImages = MutableStateFlow<List<ItemsMissingImage>>(emptyList())
    val missingImages: StateFlow<List<ItemsMissingImage>> = _missingImages.asStateFlow()

    private val _allListState = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val allListState = _allListState.asStateFlow()

    private val _missingListState = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val missingListState = _missingListState.asStateFlow()

    fun saveListState(tab: Tab, index: Int, offset: Int) {
        if (tab == Tab.ALL) {
            _allListState.value = mapOf(index to offset)
        } else {
            _missingListState.value = mapOf(index to offset)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    init {
        reloadMeals()
        loadMissingImages()
        loadFrequencies()
    }

    fun refresh() {
        reloadMeals()
        loadMissingImages()
        loadFrequencies()
    }

    fun loadFrequencies() {
        scope.launch {
            val freqs = intakeRepo.getAllItemFrequencies()
            _itemFrequencies.value = freqs.associate { item: AllItemFrequencies ->
                val id = when (item.source_type) {
                    "FOOD" -> item.source_food_id
                    "MEAL" -> item.source_meal_id
                    else -> null
                }
                (item.source_type to (id ?: -1L)) to item.frequency
            }
        }
    }

    fun setSelectedTab(tab: Tab) {
        _selectedTab.value = tab
    }

    fun setSearch(value: String) {
        _search.value = value
        reloadMeals()
    }

    fun loadMissingImages() {
        scope.launch {
            _missingImages.value = intakeRepo.getItemsMissingImage().filter { it.source_type == "MEAL" }
        }
    }

    fun reloadMeals() {
        searchJob?.cancel()
        searchJob = scope.launch {
            val q = _search.value.trim()
            val result = if (q.isBlank()) {
                intakeRepo.listAllMeals().sortedBy { it.name.lowercase() }
            } else {
                intakeRepo.searchMeals(q)
            }
            _meals.value = result
        }
    }

    suspend fun computeMealTotals(mealId: Long): Pair<Double, Double> {
        return intakeRepo.computeMealTotals(mealId)
    }
}
