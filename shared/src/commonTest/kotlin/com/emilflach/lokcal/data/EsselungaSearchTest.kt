package com.emilflach.lokcal.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsselungaSearchTest {

    private val mockDetailJson = """
        {
          "displayableProduct": {
            "code": "721558",
            "description": "Test Product",
            "imageURL": "https://example.com/image.jpg",
            "barcode": "1234567890123",
            "sanitizeDescription": "test-product"
          },
          "informations": [
            {
              "label": "Valori nutrizionali",
              "value": "<table><tbody><tr><td>Energia</td><td>250 kcal</td></tr></tbody></table>",
              "type": "HTML"
            }
          ]
        }
    """.trimIndent()

    private fun fakeEntity(code: String, slug: String, name: String): JsonObject = buildJsonObject {
        put("code", code)
        put("sanitizeDescription", slug)
        put("description", name)
        put("imageURL", "https://images.services.esselunga.it/img/$code.jpg")
    }

    @Test
    fun testSearchParsesApiEntities() = runTest {
        val mockFetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String = mockDetailJson
        }
        val search = object : EsselungaSearch(fetcher = mockFetcher) {
            override suspend fun fetchSearchResults(query: String): List<JsonObject> = listOf(
                fakeEntity("721558", "esselunga-scamorza-250-g", "Esselunga Scamorza 250 g"),
                fakeEntity("737115", "monte-san-savino-salame-toscano", "Monte San Savino Salame Toscano"),
                fakeEntity("737106", "levoni-salame-ungherese", "Levoni Salame Ungherese"),
            )
        }
        val results = search.search("scamorza")
        assertEquals(3, results.size)
        assertTrue(results.all { it.energyKcalPer100g == 250.0 })
        // Name from detail API wins over the facet API description
        assertTrue(results.all { it.name == "Test Product" })
    }

    @Test
    fun testSearchBuildsCorrectProductUrl() = runTest {
        val capturedCodes = mutableListOf<String>()
        val mockFetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String {
                capturedCodes += code
                return mockDetailJson
            }
        }
        val search = object : EsselungaSearch(fetcher = mockFetcher) {
            override suspend fun fetchSearchResults(query: String): List<JsonObject> = listOf(
                fakeEntity("721558", "esselunga-scamorza-250-g", "Esselunga Scamorza 250 g")
            )
        }
        val results = search.search("scamorza")
        assertEquals(listOf("721558"), capturedCodes)
        assertEquals(
            "https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/721558/esselunga-scamorza-250-g",
            results.firstOrNull()?.productUrl
        )
    }

    @Test
    fun testSearchFallsBackToApiNameWhenFetchFails() = runTest {
        val mockFetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String = error("Network error")
        }
        val search = object : EsselungaSearch(fetcher = mockFetcher) {
            override suspend fun fetchSearchResults(query: String): List<JsonObject> = listOf(
                fakeEntity("721558", "esselunga-scamorza-250-g", "Esselunga Scamorza 250 g")
            )
        }
        val results = search.search("scamorza")
        assertEquals(1, results.size)
        assertEquals("Esselunga Scamorza", results[0].name)
        assertEquals(
            "https://spesaonline.esselunga.it/commerce/nav/supermercato/store/prodotto/721558/esselunga-scamorza-250-g",
            results[0].productUrl
        )
    }

    @Test
    fun testSearchEmptyQueryReturnsEmpty() = runTest {
        val search = object : EsselungaSearch() {
            override suspend fun fetchSearchResults(query: String): List<JsonObject> =
                error("Should not be called for blank query")
        }
        assertTrue(search.search("").isEmpty())
        assertTrue(search.search("   ").isEmpty())
    }

    @Test
    fun testSearchSkipsEntitiesWithMissingFields() = runTest {
        val mockFetcher = object : EsselungaWebFetcher() {
            override suspend fun fetchDetailJson(code: String): String = mockDetailJson
        }
        val search = object : EsselungaSearch(fetcher = mockFetcher) {
            override suspend fun fetchSearchResults(query: String): List<JsonObject> = listOf(
                fakeEntity("721558", "esselunga-scamorza-250-g", "Valid"),
                buildJsonObject { put("description", "Missing code and slug") }, // skipped
                fakeEntity("737115", "monte-san-savino-salame", "Also Valid"),
            )
        }
        val results = search.search("scamorza")
        assertEquals(2, results.size)
    }
}
