package com.emilflach.lokcal.util

import kotlin.math.round

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
}