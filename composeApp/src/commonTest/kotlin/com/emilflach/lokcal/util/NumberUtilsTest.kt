package com.emilflach.lokcal.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberUtilsTest {
    @Test
    fun parseDecimal_basic_and_min() {
        assertEquals(12.0, NumberUtils.parseDecimal("12"))
        assertEquals(12.5, NumberUtils.parseDecimal("12,5"))
        assertEquals(12.5, NumberUtils.parseDecimal(" 12.5 "))
        assertEquals(0.0, NumberUtils.parseDecimal("abc"))
        assertEquals(0.0, NumberUtils.parseDecimal("-5")) // coerced to min 0
        assertEquals(-2.0, NumberUtils.parseDecimal("-2", min = -5.0)) // min respected
    }

    @Test
    fun sanitizeDecimalInput_rules() {
        assertEquals("0", NumberUtils.sanitizeDecimalInput("00"))
        assertEquals("12", NumberUtils.sanitizeDecimalInput("0012"))
        assertEquals("1.23", NumberUtils.sanitizeDecimalInput("1,23"))
        assertEquals("12.3", NumberUtils.sanitizeDecimalInput("12..3"))
        assertEquals("123456", NumberUtils.sanitizeDecimalInput("1234567"))
    }

    @Test
    fun formatDecimalTrimmed_behavior() {
        assertEquals("2", NumberUtils.formatDecimalTrimmed(2.0))
        assertEquals("2.5", NumberUtils.formatDecimalTrimmed(2.5))
        assertEquals("2.35", NumberUtils.formatDecimalTrimmed(2.345))
        assertEquals("0", NumberUtils.formatDecimalTrimmed(0.0))
    }
}