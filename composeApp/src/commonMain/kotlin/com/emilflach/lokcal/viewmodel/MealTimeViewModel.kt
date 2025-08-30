package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.PortionsCalculator
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MealTimeViewModel(
    private val intakeRepo: IntakeRepository,
    val mealType: String,
) {
    data class UiState(
        val items: List<Intake> = emptyList(),
        val totalKcal: Double = 0.0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadToday()
    }

    fun loadToday() {
        val date = currentDateIso()
        val startIso = "${date}T00:00:00"
        val endIso = "${date}T23:59:59"
        val list = intakeRepo.getIntakeByMealAndDateRange(mealType, startIso, endIso)
        _state.value = UiState(
            items = list,
            totalKcal = list.sumOf { it.energy_kcal_total }
        )
    }

    fun deleteItem(id: Long) {
        intakeRepo.deleteIntakeById(id)
        loadToday()
    }

    fun updateQuantity(id: Long, newQuantityG: Double) {
        intakeRepo.updateIntakeQuantity(id, newQuantityG)
        loadToday()
    }


    fun imageUrlForFoodId(foodId: Long): String? = intakeRepo.getFoodById(foodId)?.image_url

    fun defaultPortionGramsForFoodId(foodId: Long): Double {
        val food = intakeRepo.getFoodById(foodId)
        return PortionsCalculator.defaultPortion(food?.serving_size)
    }

    fun portionForEntry(intake: Intake): Double {
        return when {
            intake.source_food_id != null -> defaultPortionGramsForFoodId(intake.source_food_id)
            intake.source_meal_id != null -> intakeRepo.getMealPortionGrams(intake.source_meal_id)
            else -> 100.0
        }
    }

    fun subtitleForIntake(intake: Intake): String {
        return PortionsCalculator.subtitleKcalPortions(
            kcal = intake.energy_kcal_total,
            grams = intake.quantity_g,
            portionGrams = portionForEntry(intake)
        )
    }

    fun saveAsMeal(name: String, totalPortions: Double) {
        val items = state.value.items
            .filter { it.source_food_id != null }
            .map { it.source_food_id!! to it.quantity_g }
        if (items.isEmpty()) return
        val mealId = intakeRepo.createMeal(name, totalPortions, items)
        // Sum grams
        val totalGrams = items.sumOf { it.second }
        // Delete only the food items that were saved into the meal
        state.value.items.filter { it.source_food_id != null }.forEach { intakeRepo.deleteIntakeById(it.id) }
        // Log new meal as a single entry
        intakeRepo.logOrUpdateMealIntake(
            mealId = mealId,
            quantityG = totalGrams,
            mealType = mealType,
        )
        loadToday()
    }
}