package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.LabelService
import com.emilflach.lokcal.data.PortionService
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
        val totalKcalLabel: String = LabelService().kcalLabel(0.0),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)

    init {
        loadToday()
    }

    fun loadToday() {
        val date = currentDateIso()
        val startIso = "${date}T00:00:00"
        val endIso = "${date}T23:59:59"
        val list = intakeRepo.getIntakeByMealAndDateRange(mealType, startIso, endIso)
        val total = list.sumOf { it.energy_kcal_total }
        _state.value = UiState(
            items = list,
            totalKcal = total,
            totalKcalLabel = LabelService().kcalLabel(total)
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

    fun portionForEntry(intake: Intake): Double = portionService.defaultPortionForIntake(intake)

    fun subtitleForIntake(intake: Intake): String = labelService.subtitleForIntake(intake)

    fun saveAsMeal(name: String, totalPortions: Double) {
        intakeRepo.saveCurrentMealFromIntakes(mealType, name, totalPortions)
        loadToday()
    }

    fun saveAsMealFromInputs(nameText: String, portionsText: String) {
        val displayName = nameText.trim().ifBlank { "Meal" }
        val portions = com.emilflach.lokcal.util.NumberUtils.parseDecimal(portionsText, min = 0.0)
            .takeIf { it > 0.0 } ?: 1.0
        saveAsMeal(displayName, portions)
    }

    fun copyMealItemsIntoMealTime(mealId: Long) {
        intakeRepo.copyMealItemsIntoMealTime(mealId, mealType)
        loadToday()
    }

    fun updateQuantityByPortions(entryId: Long, portions: Double) {
        val intake = state.value.items.firstOrNull { it.id == entryId } ?: return
        val portionGrams = portionForEntry(intake)
        val grams = (portions * portionGrams).coerceAtLeast(0.0)
        updateQuantity(entryId, grams)
    }
}