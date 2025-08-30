package com.emilflach.lokcal.data

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.util.NumberUtils
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Centralizes subtitle label generation ("kcal • portions").
 */
class LabelService(private val intakeRepo: IntakeRepository? = null, private val portionService: PortionService? = null) {
    fun subtitleForFood(food: Food, grams: Double): String {
        requireNotNull(portionService) { "subtitleForMeal requires PortionService" }
        val kcal = food.energy_kcal_per_100g * grams / 100.0
        val portionG = portionService.defaultPortionForFood(food)
        return subtitleKcalPortions(kcal = kcal, grams = grams, portionGrams = portionG)
    }

    fun subtitleForMeal(mealId: Long, grams: Double): String {
        requireNotNull(intakeRepo) { "subtitleForMeal requires IntakeRepository" }
        requireNotNull(portionService) { "subtitleForMeal requires PortionService" }
        val (totalG, totalKcal) = intakeRepo.computeMealTotals(mealId)
        val kcal = if (totalG > 0.0 && grams > 0.0) totalKcal * (grams / totalG) else 0.0
        val portionG = portionService.defaultPortionForMeal(mealId)
        return subtitleKcalPortions(kcal = kcal, grams = grams, portionGrams = portionG)
    }

    fun subtitleForIntake(intake: Intake): String {
        requireNotNull(intakeRepo) { "subtitleForIntake requires IntakeRepository" }
        requireNotNull(portionService) { "subtitleForMeal requires PortionService" }
        val portionG = portionService.defaultPortionForIntake(intake)
        return subtitleKcalPortions(
            kcal = intake.energy_kcal_total,
            grams = intake.quantity_g,
            portionGrams = portionG
        )
    }

    fun subtitleKcalPortions(kcal: Double, grams: Double, portionGrams: Double): String {
        val portions = if (portionGrams > 0) grams / portionGrams else 0.0
        val portionsText = portionsLabel(portions)
        val kcal = kcalLabel(kcal)
        return "$kcal • $portionsText"
    }

    fun kcalLabel(value: Double): String = "${value.roundToInt()} kcal"

    fun portionsLabel(portions: Double): String {
        val valueStr = when {
            portions == 0.0 -> "0"
            portions < 0.01 -> "< 0.01"
            portions >= 10.0 -> portions.toInt().toString()
            portions == portions.toInt().toDouble() -> portions.toInt().toString()
            else -> {
                val rounded = round(portions * 100) / 100
                NumberUtils.formatDecimalTrimmed(rounded, maxFractionDigits = 2)
            }
        }
        val label = when {
            portions == 0.0 -> "portions"
            portions > 1.0 -> "portions"
            else -> "portion"
        }
        return "$valueStr $label"
    }


}

