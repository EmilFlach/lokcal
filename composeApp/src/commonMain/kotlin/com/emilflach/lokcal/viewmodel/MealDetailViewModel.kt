package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MealDetailViewModel(
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
    fun sanitizeGramsInput(text: String, maxDigits: Int = 5): String = text.filter { it.isDigit() }.take(maxDigits)

    fun parseGrams(text: String): Double = text.trim().replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    fun addPortionText(currentText: String, intake: Intake): String {
        val current = parseGrams(currentText)
        val portion = portionForEntry(intake)
        val newVal = (current + portion).coerceAtLeast(0.0)
        return newVal.toInt().toString()
    }

    fun imageUrlForFoodId(foodId: Long): String? = intakeRepo.getFoodById(foodId)?.image_url

    fun defaultPortionGramsForFoodId(foodId: Long): Double {
        val food = intakeRepo.getFoodById(foodId)
        return food?.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
    }

    fun portionForEntry(intake: Intake): Double {
        return intake.source_food_id?.let { defaultPortionGramsForFoodId(it) } ?: 100.0
    }
}