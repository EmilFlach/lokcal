package com.emilflach.lokcal.util

object ExerciseMath {
    /**
     * Calculates kcal burned given a type's kcal/hour and minutes.
     */
    fun kcal(typeKcalPerHour: Double, minutes: Double): Double {
        return (typeKcalPerHour / 60.0) * minutes
    }
}