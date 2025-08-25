package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.data.IntakeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditMealViewModel(
    private val repo: IntakeRepository,
    private val mealId: Long,
) {
    data class ItemUi(
        val mealItemId: Long,
        val food: com.emilflach.lokcal.Food,
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
        val items = repo.getMealItemsWithFood(mealId).map { ItemUi(it.mealItemId, it.food, it.quantityG) }
        _state.value = UiState(
            meal = meal,
            name = meal?.name ?: "",
            imageUrl = meal?.image_url ?: "",
            totalPortions = (meal?.total_portions ?: 1.0).let { if (it == it.toInt().toDouble()) it.toInt().toString() else it.toString() },
            items = items,
        )
    }

    fun setName(value: String) {
        _state.value = _state.value.copy(name = value)
        persistMeta()
    }

    fun setImageUrl(value: String) {
        _state.value = _state.value.copy(imageUrl = value)
        persistMeta()
    }

    fun setTotalPortionsText(value: String) {
        _state.value = _state.value.copy(totalPortions = value)
        persistMeta()
    }

    private fun persistMeta() {
        val st = _state.value
        val portions = st.totalPortions.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
        val name = st.name.ifBlank { st.meal?.name ?: "Meal" }
        repo.updateMealMeta(mealId, name, st.imageUrl.ifBlank { null }, portions)
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

    fun deleteMeal() {
        repo.deleteMeal(mealId)
    }

    // UI helpers
    fun parseGrams(text: String): Double = text.trim().replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    fun defaultPortionGrams(food: com.emilflach.lokcal.Food): Double {
        return food.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
    }

    fun computeKcalFor(food: com.emilflach.lokcal.Food, totalGrams: Double): Double {
        return (food.energy_kcal_per_100g * totalGrams / 100.0)
    }

    fun getPortionsTextFor(food: com.emilflach.lokcal.Food, grams: Double): String {
        val portionSize = defaultPortionGrams(food)
        val portions = if (portionSize > 0) grams / portionSize else 0.0
        val value = when {
            portions == 0.0 -> "0"
            portions < 0.01 -> "< 0.01"
            portions >= 10 -> portions.toInt().toString()
            portions == portions.toInt().toDouble() -> portions.toInt().toString()
            else -> {
                val rounded = kotlin.math.round(portions * 100) / 100
                val str = rounded.toString()
                if (str.contains('.')) {
                    str.trimEnd('0').trimEnd('.')
                } else {
                    str
                }
            }
        }
        val label = when {
            portions == 0.0 -> "portions"
            portions > 1 -> "portions"
            else -> "portion"
        }
        return "$value $label"
    }
}
