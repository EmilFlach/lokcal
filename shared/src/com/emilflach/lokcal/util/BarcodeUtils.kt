package com.emilflach.lokcal.util

/**
 * Recognises food barcodes in a search query.
 *
 * Food packaging uses GTIN-8 (EAN-8), GTIN-12 (UPC-A) and GTIN-13 (EAN-13), so a query is
 * treated as a barcode lookup only when it is exactly one of those digit counts and nothing
 * but digits. The scanned value is passed through untouched — callers match it as-is rather
 * than widening it to a common length.
 */
object BarcodeUtils {
    /** Digit counts of the food barcodes we accept: GTIN-8, GTIN-12, GTIN-13. */
    private val BARCODE_LENGTHS = setOf(8, 12, 13)

    /** True when [query] is exactly a food barcode: digits only, 8, 12 or 13 of them. */
    fun isBarcode(query: String): Boolean {
        val q = query.trim()
        return q.length in BARCODE_LENGTHS && q.all { it.isDigit() }
    }
}
