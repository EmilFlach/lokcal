package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.LabelService
import com.emilflach.lokcal.data.PortionService
import com.emilflach.lokcal.util.NumberUtils.parseDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class MealTimeViewModel(
    private val intakeRepo: IntakeRepository,
    val mealType: String,
    private val dateIso: String,
) {
    data class UiState(
        val items: List<Intake> = emptyList(),
        val totalKcal: Double = 0.0,
        val totalKcalLabel: String = LabelService().kcalLabel(0.0),
        val yesterdayItems: List<Intake> = emptyList(),
        val leftoversItems: List<Intake> = emptyList(),
        val isMarkedLeftover: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)

    init {
        loadForSelectedDate()
    }

    private fun loadForSelectedDate() {
        val startIso = "${dateIso}T00:00:00"
        val endIso = "${dateIso}T23:59:59"
        val list = intakeRepo.getIntakeByMealAndDateRange(mealType, startIso, endIso)
        val total = list.sumOf { it.energy_kcal_total }

        // Yesterday range for same meal type
        val yesterdayDate = LocalDate.parse(dateIso).plus(-1, DateTimeUnit.DAY).toString()
        val yStartIso = "${yesterdayDate}T00:00:00"
        val yEndIso = "${yesterdayDate}T23:59:59"

        val todayFoodIds = list.mapNotNull { it.source_food_id }.toSet()
        val todayMealIds = list.mapNotNull { it.source_meal_id }.toSet()
        val yesterdayList = intakeRepo.getIntakeByMealAndDateRange(mealType, yStartIso, yEndIso)
            .let { yList ->
                yList.filter { y ->
                    when {
                        y.source_food_id != null -> y.source_food_id !in todayFoodIds
                        y.source_meal_id != null -> y.source_meal_id !in todayMealIds
                        else -> true
                    }
                }
                // Also collapse duplicates from yesterday by source id to avoid multiple suggestion rows
                .distinctBy { y -> y.source_food_id?.let { "F:$it" } ?: y.source_meal_id?.let { "M:$it" } }
            }

        val leftoversRaw = intakeRepo.getAllLeftoverIntakesExcludingDate()
        val yesterdayFoodIds = yesterdayList.mapNotNull { it.source_food_id }.toSet()
        val yesterdayMealIds = yesterdayList.mapNotNull { it.source_meal_id }.toSet()
        val leftoversList = leftoversRaw
            .filter { y ->
                when {
                    y.source_food_id != null -> y.source_food_id !in todayFoodIds && y.source_food_id !in yesterdayFoodIds
                    y.source_meal_id != null -> y.source_meal_id !in todayMealIds && y.source_meal_id !in yesterdayMealIds
                    else -> true
                }
            }
            .distinctBy { y -> y.source_food_id?.let { "F:$it" } ?: y.source_meal_id?.let { "M:$it" } }

        val isMarked = intakeRepo.isLeftoversMarkedForMealTypeOnDate(mealType, dateIso)

        _state.value = UiState(
            items = list,
            totalKcal = total,
            totalKcalLabel = LabelService().kcalLabel(total),
            yesterdayItems = yesterdayList,
            leftoversItems = leftoversList,
            isMarkedLeftover = isMarked,
        )
    }

    fun deleteItem(id: Long) {
        intakeRepo.deleteIntakeById(id)
        loadForSelectedDate()
    }

    fun updateQuantity(id: Long, newQuantityG: Double) {
        intakeRepo.updateIntakeQuantity(id, newQuantityG)
        loadForSelectedDate()
    }

    fun imageUrlForFoodId(foodId: Long): String? = intakeRepo.getFoodById(foodId)?.image_url
    fun imageUrlForMealId(mealId: Long): String? = intakeRepo.getMealById(mealId)?.image_url

    fun portionForEntry(intake: Intake): Double = portionService.defaultPortionForIntake(intake)
    fun portionForMeal(mealId: Long): Double = portionService.defaultPortionForMeal(mealId)

    fun subtitleForIntake(intake: Intake): String = labelService.subtitleForIntake(intake)
    fun subtitleForFoodSuggestion(foodId: Long, grams: Double): String {
        val food = intakeRepo.getFoodById(foodId) ?: return ""
        return labelService.subtitleForFood(food, grams)
    }
    fun subtitleForMealSuggestion(mealId: Long, grams: Double): String = labelService.subtitleForMeal(mealId, grams)

    fun saveAsMealFromInputs(nameText: String, portionsText: String) {
        val name = nameText.trim().ifBlank { "Meal" }
        val portions = parseDecimal(portionsText, min = 0.0).takeIf { it > 0.0 } ?: 1.0
        intakeRepo.saveCurrentMealFromIntakes(mealType, name, portions, dateIso)
        loadForSelectedDate()
    }

    fun copyMealItemsIntoMealTime(mealId: Long) {
        intakeRepo.copyMealItemsIntoMealTime(mealId, mealType, dateIso)
        loadForSelectedDate()
    }

    fun updateQuantityByPortions(entryId: Long, portions: Double) {
        val intake = state.value.items.firstOrNull { it.id == entryId } ?: return
        val portionGrams = portionForEntry(intake)
        val grams = (portions * portionGrams).coerceAtLeast(0.0)
        updateQuantity(entryId, grams)
    }

    fun addFoodSuggestion(foodId: Long, gramsText: String) {
        val grams = parseDecimal(gramsText, min = 0.0)
        intakeRepo.logOrUpdateFoodIntake(foodId, grams, mealType, dateIso)
        loadForSelectedDate()
    }

    fun addFoodSuggestionFromLeftover(foodId: Long, gramsText: String, source: Intake) {
        val grams = parseDecimal(gramsText, min = 0.0)
        intakeRepo.logOrUpdateFoodIntake(foodId, grams, mealType, dateIso)
        // Clear leftover flag only for the selected leftover item
        intakeRepo.setLeftoverFlagById(source.id, false)
        loadForSelectedDate()
    }

    fun addMealSuggestion(mealId: Long, portionsText: String) {
        val portions = parseDecimal(portionsText, min = 0.0)
        val portionG = portionForMeal(mealId)
        val grams = (portions * portionG).coerceAtLeast(0.0)
        intakeRepo.logOrUpdateMealIntake(mealId, grams, mealType, dateIso)
        loadForSelectedDate()
    }

    fun addMealSuggestionFromLeftover(mealId: Long, portionsText: String, source: Intake) {
        val portions = parseDecimal(portionsText, min = 0.0)
        val portionG = portionForMeal(mealId)
        val grams = (portions * portionG).coerceAtLeast(0.0)
        intakeRepo.logOrUpdateMealIntake(mealId, grams, mealType, dateIso)
        // Clear leftover flag only for the selected leftover item
        intakeRepo.setLeftoverFlagById(source.id, false)
        loadForSelectedDate()
    }

    fun toggleLeftovers() {
        val current = state.value.isMarkedLeftover
        if (current) intakeRepo.setLeftoversForMealTypeOnDate(mealType, dateIso, false) else intakeRepo.setLeftoversForMealTypeOnDate(mealType, dateIso, true)
        loadForSelectedDate()
    }
}