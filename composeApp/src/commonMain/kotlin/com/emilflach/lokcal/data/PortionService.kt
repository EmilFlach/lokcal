package com.emilflach.lokcal.data

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.util.NumberUtils

/**
 * Centralizes default portion resolution across foods, meals, and intake entries.
 */
class PortionService(private val intakeRepo: IntakeRepository? = null) {
    fun defaultPortionForFood(food: Food): Double =
        defaultPortion(food.serving_size)

    fun defaultPortionForMeal(mealId: Long): Double =
        intakeRepo?.getMealPortionGrams(mealId) ?: 100.0

    fun defaultPortionForIntake(intake: Intake): Double = when {
        intake.source_food_id != null -> {
            val food = intakeRepo?.getFoodById(intake.source_food_id)
            defaultPortion(food?.serving_size)
        }
        intake.source_meal_id != null -> intakeRepo?.getMealPortionGrams(intake.source_meal_id) ?: 100.0
        else -> 100.0
    }

    fun defaultPortion(foodServingSize: String?): Double {
        return foodServingSize?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
    }

    fun portions(quantityGrams: Double, portionGrams: Double): Double {
        if (portionGrams <= 0.0) return 0.0
        return quantityGrams / portionGrams
    }

    fun addPortionGrams(currentGramsText: String, portionGrams: Double): Pair<String, Double> {
        val newGrams = (NumberUtils.parseDecimal(currentGramsText) + portionGrams).coerceAtLeast(0.0)
        val newText = newGrams.toInt().toString()
        return newText to NumberUtils.parseDecimal(newText)
    }

    fun subtractPortionGrams(currentGramsText: String, portionGrams: Double): Pair<String, Double> {
        val newGrams = (NumberUtils.parseDecimal(currentGramsText) - portionGrams).coerceAtLeast(0.0)
        val newText = newGrams.toInt().toString()
        return newText to NumberUtils.parseDecimal(newText)
    }

    fun addPortionCount(currentPortionsText: String): Pair<String, Double> {
        val newP = (NumberUtils.parseDecimal(currentPortionsText) + 1.0).coerceAtLeast(0.0)
        val newText = NumberUtils.formatPortions(newP)
        return newText to NumberUtils.parseDecimal(newText)
    }

    fun subtractPortionCount(currentPortionsText: String): Pair<String, Double> {
        val newP = (NumberUtils.parseDecimal(currentPortionsText) - 1.0).coerceAtLeast(0.0)
        val newText = NumberUtils.formatPortions(newP)
        return newText to NumberUtils.parseDecimal(newText)
    }
}
