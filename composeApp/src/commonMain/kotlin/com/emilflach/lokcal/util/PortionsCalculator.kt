package com.emilflach.lokcal.util

import kotlin.math.round
import kotlin.math.roundToInt

object PortionsCalculator {
    fun defaultPortion(foodServingSize: String?): Double {
        return foodServingSize?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
    }

    fun portions(quantityGrams: Double, portionGrams: Double): Double {
        if (portionGrams <= 0.0) return 0.0
        return quantityGrams / portionGrams
    }

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

    // Merged from QuantityLogic
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
        val newText = formatPortions(newP)
        return newText to NumberUtils.parseDecimal(newText)
    }

    fun subtractPortionCount(currentPortionsText: String): Pair<String, Double> {
        val newP = (NumberUtils.parseDecimal(currentPortionsText) - 1.0).coerceAtLeast(0.0)
        val newText = formatPortions(newP)
        return newText to NumberUtils.parseDecimal(newText)
    }

    fun portionsFromGrams(grams: Double, portionGrams: Double): Double =
        if (portionGrams > 0) grams / portionGrams else 0.0

    fun portionsLabelFromGrams(grams: Double, portionGrams: Double): String =
        portionsLabel(portionsFromGrams(grams, portionGrams))

    fun subtitleKcalPortions(kcal: Double, grams: Double, portionGrams: Double): String {
        val portions = portionsLabelFromGrams(grams, portionGrams)
        val kcal = formatKcalLabel(kcal)
        return "$kcal • $portions"
    }

    fun formatKcalLabel(value: Double): String = "${value.roundToInt()} kcal"

    private fun formatPortions(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}