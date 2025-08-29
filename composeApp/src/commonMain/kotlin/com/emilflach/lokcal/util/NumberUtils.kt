package com.emilflach.lokcal.util

import kotlin.math.pow

object NumberUtils {
    /**
     * Parses a localized decimal string to Double.
     * - Trims whitespace
     * - Normalizes comma to dot
     * - Returns [min] when parsing fails
     * - Coerces to be at least [min]
     */
    fun parseDecimal(text: String, min: Double = 0.0): Double {
        val v = text.trim().replace(",", ".").toDoubleOrNull()
        return (v ?: min).coerceAtLeast(min)
    }

    /**
     * Sanitizes decimal user input.
     * - Allows only digits and a single decimal separator (dot or comma)
     * - Normalizes decimal separator to dot
     * - Truncates to [maxLength]
     * - Trims leading zeros like "00" unless it's the "0." case
     */
    fun sanitizeDecimalInput(text: String, maxLength: Int = 6): String {
        val filtered = buildString {
            var hasSep = false
            for (ch in text) {
                when {
                    ch.isDigit() -> append(ch)
                    (ch == '.' || ch == ',') && !hasSep -> {
                        append('.')
                        hasSep = true
                    }
                }
                if (length >= maxLength) break
            }
        }
        return if (filtered.startsWith("00")) filtered.trimStart('0').ifEmpty { "0" } else filtered
    }

    /**
     * Formats a decimal value with up to [maxFractionDigits] fraction digits, then trims trailing zeros and dot.
     */
    fun formatDecimalTrimmed(value: Double, maxFractionDigits: Int = 2): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()
        // Round to the requested fraction digits
        val scale = 10.0.pow(maxFractionDigits)
        val rounded = kotlin.math.round(value * scale) / scale
        var s = rounded.toString()
        if (s.contains('.')) {
            s = s.trimEnd('0').trimEnd('.')
        }
        return s
    }
}