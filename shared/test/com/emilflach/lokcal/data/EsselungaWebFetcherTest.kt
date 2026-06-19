package com.emilflach.lokcal.data

import kotlinx.coroutines.test.runTest
import lokcal.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals

class EsselungaWebFetcherTest {

    private val detailJson = """
        {
          "displayableProduct": {
            "code": "721558",
            "description": "Test Scamorza",
            "imageURL": "https://example.com/image.jpg",
            "barcode": "1234567890123",
            "sanitizeDescription": "test-scamorza"
          },
          "informations": [
            {
              "label": "Valori nutrizionali",
              "value": "<table><thead><tr><th>Informazioni nutrizionali</th><th>per 100 g di prodotto</th><th>per 75 g di prodotto</th><th>%AR* per porzione (75 g)</th></tr></thead><tbody><tr><td>Energia</td><td>1299 kJ</td><td>974 kJ</td><td>12%</td></tr><tr><td></td><td>311 kcal</td><td>233 kcal</td><td></td></tr></tbody></table>",
              "type": "HTML"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun testFetchFromDetailJsonWithInjectedResponse() = runTest {
        val fetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String = detailJson
        }
        val result = fetcher.fetchProduct(
            "https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/721558/test-scamorza"
        )

        assertEquals("Test Scamorza", result.name)
        assertEquals(311.0, result.kcalPer100g)
        assertEquals(75.0, result.servingSizeGrams)
        assertEquals("1234567890123", result.gtin13)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
    }

    @Test
    fun testFetchExtractsCodeFromUrl() = runTest {
        val capturedCodes = mutableListOf<String>()
        val fetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String {
                capturedCodes += code
                return detailJson
            }
        }
        fetcher.fetchProduct(
            "https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/721558/esselunga-scamorza-250-g"
        )
        assertEquals(listOf("721558"), capturedCodes)
    }

    @Test
    fun testFetchStripsWeightFromName() = runTest {
        fun jsonWithName(name: String) = """
            {"displayableProduct":{"code":"1","description":"$name","imageURL":"","barcode":"","sanitizeDescription":""},"informations":[]}
        """.trimIndent()
        suspend fun fetchName(name: String): String? {
            val fetcher = object : EsselungaWebFetcher() {
                override suspend fun fetchDetailJson(code: String) = jsonWithName(name)
            }
            return fetcher.fetchProduct("https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/1/x").name
        }

        assertEquals("Esselunga Scamorza", fetchName("Esselunga Scamorza 250 g"))
        assertEquals("Golfera Golfetta", fetchName("Golfera Golfetta 100 g"))
        assertEquals("Olio Cuore", fetchName("Olio Cuore 1,5 kg"))
        assertEquals("Pasta Barilla", fetchName("Pasta Barilla 500g"))
        assertEquals("Yogurt Greco", fetchName("Yogurt Greco 2 kg"))
        assertEquals("Latte Intero", fetchName("Latte Intero")) // no weight — unchanged
    }

    @Test
    fun testFetchReturnsNullsWhenDetailApiFails() = runTest {
        val fetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String = error("Network error")
        }
        val result = fetcher.fetchProduct(
            "https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/721558/test"
        )
        assertEquals(null, result.name)
        assertEquals(null, result.kcalPer100g)
        assertEquals(null, result.gtin13)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testFetchFromRealJsonFile() = runTest {
        val realJson = try {
            Res.readBytes("files/esselunga/product.json").decodeToString()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Skipping test on this platform due to resource loading limitation: ${e.message}")
            return@runTest
        }

        val fetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String = realJson
        }
        val result = fetcher.fetchProduct(
            "https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/721558/esselunga-scamorza-250-g"
        )

        assertEquals("Esselunga Scamorza", result.name)
        assertEquals(311.0, result.kcalPer100g)
        assertEquals(50.0, result.servingSizeGrams)
        assertEquals("8002330014513", result.gtin13)
        assertEquals("https://images.services.esselunga.it/html/img_prodotti/esselunga/big/721558.jpg", result.imageUrl)
    }
}
