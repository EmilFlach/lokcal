package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.MealRepository
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.util.PortionsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditMealViewModel(
    private val repo: MealRepository,
    private val mealId: Long,
) {
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
            totalPortions = formatPortions(meal?.total_portions ?: 1.0),
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
        val portions = parsePortions(st.totalPortions)
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

    // UI helpers - simplified to one-liners
    fun parseGrams(text: String) = NumberUtils.parseDecimal(text)
    fun defaultPortionGrams(food: Food) = PortionsCalculator.defaultPortion(food.serving_size)
    fun computeKcalFor(food: Food, grams: Double) = food.energy_kcal_per_100g * grams / 100.0

    fun getPortionsTextFor(food: Food, grams: Double): String {
        val portionSize = defaultPortionGrams(food)
        val portions = PortionsCalculator.portions(grams, portionSize)
        return PortionsCalculator.portionsLabel(portions)
    }

    private fun parsePortions(text: String) =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0

    private fun formatPortions(portions: Double) =
        if (portions == portions.toInt().toDouble()) portions.toInt().toString() else portions.toString()
}