package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.LabelService
import com.emilflach.lokcal.data.PortionService
import com.emilflach.lokcal.util.NumberUtils.parseDecimal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class MealTimeViewModel(
    private val intakeRepo: IntakeRepository,
    val mealType: String,
    private val dateIso: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    data class IntakeUiState(
        val intake: Intake,
        val subtitle: String = "",
        val imageUrl: String? = null,
        val portionGrams: Double = 0.0,
        val productUrl: String? = null,
    )

    data class SuggestionUiState(
        val intake: Intake,
        val subtitle: String = "",
        val imageUrl: String? = null,
        val portionGrams: Double = 0.0,
        val productUrl: String? = null,
    )

    data class UiState(
        val items: List<IntakeUiState> = emptyList(),
        val totalKcal: Double = 0.0,
        val yesterdayItems: List<SuggestionUiState> = emptyList(),
        val leftoversItems: List<SuggestionUiState> = emptyList(),
        val isMarkedLeftover: Boolean = false,
        val suggestionInputs: Map<Long, String> = emptyMap(),
        val highlightedIntakeId: Long? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // Centralized services
    private val portionService = PortionService(intakeRepo)
    private val labelService = LabelService(intakeRepo, portionService)

    init {
        loadForSelectedDate()
    }

    fun loadForSelectedDate(shouldHighlightLatest: Boolean = false) {
        scope.launch {
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
            val yesterdayRaw = intakeRepo.getIntakeByMealAndDateRange(mealType, yStartIso, yEndIso)
                .let { yList ->
                    yList.filter { y ->
                        when {
                            y.source_food_id != null -> y.source_food_id !in todayFoodIds
                            y.source_meal_id != null -> y.source_meal_id !in todayMealIds
                            else -> true
                        }
                    }
                    .distinctBy { y -> y.source_food_id?.let { "F:$it" } ?: y.source_meal_id?.let { "M:$it" } }
                }

            val leftoversRaw = intakeRepo.getAllLeftoverIntakesExcludingDate()
            val yesterdayFoodIds = yesterdayRaw.mapNotNull { it.source_food_id }.toSet()
            val yesterdayMealIds = yesterdayRaw.mapNotNull { it.source_meal_id }.toSet()
            val leftoversFiltered = leftoversRaw
                .filter { y ->
                    when {
                        y.source_food_id != null -> y.source_food_id !in todayFoodIds && y.source_food_id !in yesterdayFoodIds
                        y.source_meal_id != null -> y.source_meal_id !in todayMealIds && y.source_meal_id !in yesterdayMealIds
                        else -> true
                    }
                }
                .distinctBy { y -> y.source_food_id?.let { "F:$it" } ?: y.source_meal_id?.let { "M:$it" } }

            val isMarked = intakeRepo.isLeftoversMarkedForMealTypeOnDate(mealType, dateIso)
            val highlightedId = if (shouldHighlightLatest) intakeRepo.getLatestIntakeId() else null

            val itemsUi = list.map { entry ->
                IntakeUiState(
                    intake = entry,
                    subtitle = labelService.subtitleForIntake(entry),
                    imageUrl = when {
                        entry.source_food_id != null -> intakeRepo.getFoodById(entry.source_food_id)?.image_url
                        entry.source_meal_id != null -> intakeRepo.getMealById(entry.source_meal_id)?.image_url
                        else -> null
                    },
                    portionGrams = portionService.defaultPortionForIntake(entry),
                    productUrl = entry.source_food_id?.let { intakeRepo.getFoodById(it)?.product_url }
                )
            }

            suspend fun toSuggestionUi(entry: Intake) = SuggestionUiState(
                intake = entry,
                imageUrl = when {
                    entry.source_food_id != null -> intakeRepo.getFoodById(entry.source_food_id)?.image_url
                    entry.source_meal_id != null -> intakeRepo.getMealById(entry.source_meal_id)?.image_url
                    else -> null
                },
                portionGrams = portionService.defaultPortionForIntake(entry),
                productUrl = entry.source_food_id?.let { intakeRepo.getFoodById(it)?.product_url }
            )

            val yesterdayUi = yesterdayRaw.map { toSuggestionUi(it) }
            val leftoversUi = leftoversFiltered.map { toSuggestionUi(it) }

            _state.value = _state.value.copy(
                items = itemsUi,
                totalKcal = total,
                yesterdayItems = yesterdayUi,
                leftoversItems = leftoversUi,
                isMarkedLeftover = isMarked,
                highlightedIntakeId = highlightedId,
            )
        }
    }

    fun updateSuggestionInput(keyId: Long, text: String) {
        _state.value = _state.value.copy(
            suggestionInputs = _state.value.suggestionInputs + (keyId to text)
        )
    }

    fun addSuggestion(intake: Intake, text: String, isLeftover: Boolean) {
        scope.launch {
            if (intake.source_meal_id != null) {
                val portions = parseDecimal(text, min = 0.0)
                val portionG = portionForMeal(intake.source_meal_id)
                val grams = (portions * portionG).coerceAtLeast(0.0)
                intakeRepo.logOrUpdateMealIntake(intake.source_meal_id, grams, mealType, dateIso)
            } else if (intake.source_food_id != null) {
                val grams = parseDecimal(text, min = 0.0)
                intakeRepo.logOrUpdateFoodIntake(intake.source_food_id, grams, mealType, dateIso)
            }

            if (isLeftover) {
                intakeRepo.setLeftoverFlagById(intake.id, false)
            }
            loadForSelectedDate(shouldHighlightLatest = true)
        }
    }

    fun deleteItem(id: Long) {
        scope.launch {
            intakeRepo.deleteIntakeById(id)
            loadForSelectedDate()
        }
    }

    fun updateQuantity(id: Long, newQuantityG: Double) {
        scope.launch {
            intakeRepo.updateIntakeQuantity(id, newQuantityG)
            loadForSelectedDate()
        }
    }

    suspend fun productUrlForFoodId(foodId: Long): String? = intakeRepo.getFoodById(foodId)?.product_url
    suspend fun imageUrlForFoodId(foodId: Long): String? = intakeRepo.getFoodById(foodId)?.image_url
    suspend fun imageUrlForMealId(mealId: Long): String? = intakeRepo.getMealById(mealId)?.image_url


    suspend fun portionForEntry(intake: Intake): Double = portionService.defaultPortionForIntake(intake)
    suspend fun portionForMeal(mealId: Long): Double = portionService.defaultPortionForMeal(mealId)

    suspend fun subtitleForIntake(intake: Intake): String = labelService.subtitleForIntake(intake)
    suspend fun subtitleForFoodSuggestion(foodId: Long, grams: Double): String {
        val food = intakeRepo.getFoodById(foodId) ?: return ""
        return labelService.subtitleForFood(food, grams)
    }
    suspend fun subtitleForMealSuggestion(mealId: Long, grams: Double): String = labelService.subtitleForMeal(mealId, grams)

    fun saveAsMealFromInputs(nameText: String, portionsText: String) {
        val name = nameText.trim().ifBlank { "Meal" }
        val portions = parseDecimal(portionsText, min = 0.0).takeIf { it > 0.0 } ?: 1.0
        scope.launch {
            intakeRepo.saveCurrentMealFromIntakes(mealType, name, portions, dateIso)
            loadForSelectedDate()
        }
    }

    fun copyMealItemsIntoMealTime(mealId: Long) {
        scope.launch {
            intakeRepo.copyMealItemsIntoMealTime(mealId, mealType, dateIso)
            loadForSelectedDate()
        }
    }

    fun updateQuantityByPortions(entryId: Long, portions: Double) {
        val entry = state.value.items.firstOrNull { it.intake.id == entryId } ?: return
        scope.launch {
            val portionGrams = portionForEntry(entry.intake)
            val grams = (portions * portionGrams).coerceAtLeast(0.0)
            intakeRepo.updateIntakeQuantity(entryId, grams)
            loadForSelectedDate()
        }
    }

    fun toggleLeftovers() {
        val current = state.value.isMarkedLeftover
        scope.launch {
            if (current)
                intakeRepo.setLeftoversForMealTypeOnDate(mealType, dateIso, false)
            else
                intakeRepo.setLeftoversForMealTypeOnDate(mealType, dateIso, true)
            loadForSelectedDate()
        }
    }

    fun clearHighlight() {
        _state.value = _state.value.copy(highlightedIntakeId = null)
    }
}