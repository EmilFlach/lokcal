package com.emilflach.lokcal.util

import kotlin.test.Test
import kotlin.test.assertEquals

class ExerciseMathTest {
    @Test
    fun kcal_formula() {
        // 600 kcal/hour => 10 kcal/min; for 30 min => 300 kcal
        assertEquals(300.0, ExerciseMath.kcal(600.0, 30.0))
        assertEquals(0.0, ExerciseMath.kcal(500.0, 0.0))
    }
}