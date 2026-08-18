package com.emilflach.lokcal.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BarcodeUtilsTest {
    @Test
    fun accepts_the_three_food_barcode_lengths() {
        assertTrue(BarcodeUtils.isBarcode("96385074")) // GTIN-8 / EAN-8
        assertTrue(BarcodeUtils.isBarcode("012000001086")) // GTIN-12 / UPC-A
        assertTrue(BarcodeUtils.isBarcode("5000112637922")) // GTIN-13 / EAN-13
    }

    @Test
    fun tolerates_surrounding_whitespace() {
        assertTrue(BarcodeUtils.isBarcode(" 5000112637922 "))
    }

    @Test
    fun rejects_other_digit_counts() {
        assertFalse(BarcodeUtils.isBarcode("1234567")) // 7
        assertFalse(BarcodeUtils.isBarcode("123456789")) // 9
        assertFalse(BarcodeUtils.isBarcode("12345678901")) // 11
        assertFalse(BarcodeUtils.isBarcode("50001126379221")) // 14
        assertFalse(BarcodeUtils.isBarcode(""))
    }

    @Test
    fun rejects_anything_that_is_not_purely_digits() {
        assertFalse(BarcodeUtils.isBarcode("500011263792X"))
        assertFalse(BarcodeUtils.isBarcode("5000-112637922"))
        assertFalse(BarcodeUtils.isBarcode("5000 112637922"))
        assertFalse(BarcodeUtils.isBarcode("cola"))
    }

    @Test
    fun does_not_treat_plain_food_searches_as_barcodes() {
        assertFalse(BarcodeUtils.isBarcode("coca-cola"))
        assertFalse(BarcodeUtils.isBarcode("2 eggs"))
    }
}
