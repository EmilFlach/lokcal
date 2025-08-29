package com.emilflach.lokcal.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortionsCalculatorTest {
    @Test
    fun defaultPortion_parsing_and_fallback() {
        assertEquals(150.0, PortionsCalculator.defaultPortion("150"))
        assertEquals(100.0, PortionsCalculator.defaultPortion(null))
        assertEquals(100.0, PortionsCalculator.defaultPortion(""))
        assertEquals(100.0, PortionsCalculator.defaultPortion("-20"))
        assertEquals(100.0, PortionsCalculator.defaultPortion("abc"))
    }

    @Test
    fun portions_basic() {
        assertEquals(0.0, PortionsCalculator.portions(0.0, 100.0))
        assertEquals(1.5, PortionsCalculator.portions(150.0, 100.0))
        assertEquals(0.0, PortionsCalculator.portions(100.0, 0.0)) // guard
    }

    @Test
    fun portionsLabel_boundaries_and_pluralization() {
        assertEquals("0 portions", PortionsCalculator.portionsLabel(0.0))
        assertEquals("< 0.01 portion", PortionsCalculator.portionsLabel(0.005))
        assertEquals("1 portion", PortionsCalculator.portionsLabel(1.0))
        assertEquals("1.23 portions", PortionsCalculator.portionsLabel(1.234))
        assertEquals("10 portions", PortionsCalculator.portionsLabel(10.0))
        val s = PortionsCalculator.portionsLabel(2.5)
        assertTrue(s.startsWith("2.5"))
        assertTrue(s.endsWith("portions"))
    }
}