package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.AllItemFrequencies
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.MealRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MealsListViewModel(
    private val intakeRepo: IntakeRepository,
    private val mealRepo: MealRepository
) {
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    private val _itemFrequencies = MutableStateFlow<Map<Pair<String, Long>, Long>>(emptyMap())
    val itemFrequencies: StateFlow<Map<Pair<String, Long>, Long>> = _itemFrequencies.asStateFlow()

    private val _filterMissingImages = MutableStateFlow(false)
    val filterMissingImages: StateFlow<Boolean> = _filterMissingImages.asStateFlow()

    private val _listState = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val listState = _listState.asStateFlow()

    fun saveListState(index: Int, offset: Int) {
        _listState.value = mapOf(index to offset)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var searchJob: Job? = null

    init {
        reloadMeals()
        loadFrequencies()
    }

    fun refresh() {
        reloadMeals()
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

    fun toggleMissingImagesFilter() {
        _filterMissingImages.value = !_filterMissingImages.value
    }

    fun setSearch(value: String) {
        _search.value = value
        reloadMeals()
    }

    fun reloadMeals() {
        searchJob?.cancel()
        searchJob = scope.launch {
            val q = _search.value.trim()
            val result = if (q.isBlank()) {
                mealRepo.listAllMeals().sortedBy { it.name.lowercase() }
            } else {
                mealRepo.searchMeals(q)
            }
            _meals.value = result
        }
    }

    suspend fun computeMealTotals(mealId: Long): Pair<Double, Double> {
        return intakeRepo.computeMealTotals(mealId)
    }
}
