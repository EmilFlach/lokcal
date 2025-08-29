package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.util.PortionsCalculator

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

    // --- UI helpers exposed to keep UI lean ---

    fun parseGrams(text: String): Double = NumberUtils.parseDecimal(text)

    fun addPortionText(currentText: String, intake: Intake): String {
        val current = parseGrams(currentText)
        val portion = portionForEntry(intake)
        val newVal = (current + portion).coerceAtLeast(0.0)
        return newVal.toInt().toString()
    }

    fun subtractPortionText(currentText: String, intake: Intake): String {
        val current = parseGrams(currentText)
        val portion = portionForEntry(intake)
        val newVal = (current - portion).coerceAtLeast(0.0)
        return newVal.toInt().toString()
    }

    fun getPortionsText(intake: Intake): String {
        val currentGrams = intake.quantity_g
        val portionSize = portionForEntry(intake)
        val p = PortionsCalculator.portions(currentGrams, portionSize)
        return PortionsCalculator.portionsLabel(p)
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

    fun saveAsMeal(name: String, totalPortions: Double) {
        val items = state.value.items
            .filter { it.source_food_id != null }
            .map { it.source_food_id!! to it.quantity_g }
        if (items.isEmpty()) return
        val mealId = intakeRepo.createMeal(name, totalPortions, items)
        // Sum grams
        val totalGrams = items.sumOf { it.second }
        // Log meal intake snapshot and replace items
        val date = currentDateIso()
        val timestamp = "${date}T00:00:00"
        // Delete only the food items that were saved into the meal
        state.value.items.filter { it.source_food_id != null }.forEach { intakeRepo.deleteIntakeById(it.id) }
        // Log new meal as a single entry
        intakeRepo.logMealIntake(
            mealId = mealId,
            quantityG = totalGrams,
            timestamp = timestamp,
            mealType = mealType,
            notes = null
        )
        loadToday()
    }
}