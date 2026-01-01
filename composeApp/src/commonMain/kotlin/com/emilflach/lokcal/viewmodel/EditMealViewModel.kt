package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.ui.dialogs.StealImageItem
import com.emilflach.lokcal.util.NumberUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditMealViewModel(
    private val repo: MealRepository,
    private val foodRepo: FoodRepository,
    private val intakeRepo: IntakeRepository,
    private val mealId: Long,
) {
    // Services for portion defaults and labels
    private val portionService = PortionService()
    private val labelService = LabelService(null, portionService)

    data class ItemUi(
        val mealItemId: Long,
        val food: Food,
        val quantityG: Double,
    )

    data class UiState(
        val meal: Meal? = null,
        val name: String = "",
        val imageUrl: String = "",
        val totalPortions: String = "1",
        val items: List<ItemUi> = emptyList(),
        // Steal image state
        val showStealDialog: Boolean = false,
        val stealSearchQuery: String = "",
        val stealResults: List<StealImageItem> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        val meal = repo.getMealById(mealId)
        val items = repo.getMealItemsWithFood(mealId).map {
            ItemUi(it.mealItemId, it.food, it.quantityG)
        }
        _state.value = UiState(
            meal = meal,
            name = meal?.name ?: "",
            imageUrl = meal?.image_url ?: "",
            totalPortions = NumberUtils.formatPortions(meal?.total_portions ?: 1.0),
            items = items,
        )
    }

    fun setName(value: String) {
        updateState { copy(name = value) }
        persistMeta()
    }

    fun setImageUrl(value: String) {
        updateState { copy(imageUrl = value) }
        persistMeta()
    }

    fun setTotalPortionsText(value: String) {
        updateState { copy(totalPortions = value) }
        persistMeta()
    }

    private fun updateState(update: UiState.() -> UiState) {
        _state.value = _state.value.update()
    }

    private fun persistMeta() {
        val st = _state.value
        val portions = NumberUtils.parseDecimal(st.totalPortions, min = 0.0).takeIf { it > 0.0 } ?: 1.0
        val name = st.name.ifBlank { st.meal?.name ?: "Meal" }
        val imageUrl = st.imageUrl.ifBlank { null }

        repo.updateMealMeta(mealId, name, imageUrl, portions)
        reload()
    }

    fun updateItemQuantity(itemId: Long, grams: Double) {
        repo.updateMealItemQuantity(itemId, grams.coerceAtLeast(0.0))
        reload()
    }

    fun deleteItem(itemId: Long) {
        repo.deleteMealItem(itemId)
        reload()
    }

    fun deleteMeal() = repo.deleteMeal(mealId)
    fun defaultPortionGrams(food: Food) = portionService.defaultPortionForFood(food)

    fun subtitleForFood(food: Food, initialGrams: Double): String {
        return labelService.subtitleForFood(food, initialGrams)
    }

    // Steal image logic
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var stealSearchJob: Job? = null
    fun openStealDialog() {
        updateState { copy(showStealDialog = true, stealSearchQuery = "", stealResults = emptyList()) }
    }
    fun closeStealDialog() { updateState { copy(showStealDialog = false) } }
    fun setStealSearchQuery(q: String) {
        updateState { copy(stealSearchQuery = q) }
        stealSearchJob?.cancel()
        stealSearchJob = scope.launch {
            val query = q.trim()
            if (query.length < 2) {
                updateState { copy(stealResults = emptyList()) }
                return@launch
            }
            val foods = foodRepo.search(query).map {
                StealImageItem(it.id, it.name, it.image_url, "FOOD")
            }
            val meals = intakeRepo.searchMeals(query).map {
                StealImageItem(it.id, it.name, it.image_url, "MEAL")
            }
            updateState { copy(stealResults = (foods + meals).sortedBy { it.name.lowercase() }) }
        }
    }
    fun stealImage(item: StealImageItem) {
        updateState { copy(imageUrl = item.imageUrl ?: "", showStealDialog = false) }
        persistMeta()
    }
}