package com.emilflach.lokcal.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlbertHeijnSearchTest {

    private val parser = AlbertHeijnProductParser()

    private fun product(json: String): JsonObject = Json.decodeFromString(json)

    /** A product with a full trade item: per-100 and per-serving nutrition, GTIN, image, webPath. */
    private val friteslijn = """
        {
          "id": 194759,
          "title": "Remia Friteslijn",
          "webPath": "/producten/product/wi194759/remia-friteslijn",
          "imagePack": [
            { "small": { "url": "https://static.ah.nl/dam/product/AHI_4354523130313437373539?revLabel=1&rendition=200x200_JPG_Q85&fileType=binary" } }
          ],
          "tradeItem": {
            "gtin": "08710448694632",
            "nutritions": [
              { "basisQuantity": "100.0 Milliliter", "nutrients": [ { "type": "ENER-", "value": "410.0 kJ (99.0 kcal)" } ] },
              { "basisQuantity": "15.0 Milliliter", "nutrients": [ { "type": "ENER-", "value": "62.0 kJ (15.0 kcal)" } ] }
            ]
          }
        }
    """.trimIndent()

    /** Non-food products (and incomplete listings) come back with tradeItem null. */
    private val noTradeItem = """
        {
          "id": 196986,
          "title": "Ariel Excel tabs alpine",
          "webPath": "/producten/product/wi196986/ariel-excel-tabs-alpine",
          "imagePack": [],
          "tradeItem": null
        }
    """.trimIndent()

    @Test
    fun parsesNutritionGtinImageAndUrl() {
        val result = parser.parse(product(friteslijn))

        assertEquals("Remia Friteslijn", result.name)
        assertEquals(99.0, result.kcalPer100g)
        assertEquals(15.0, result.servingSizeGrams)
        assertEquals(
            "https://static.ah.nl/dam/product/AHI_4354523130313437373539?revLabel=1&rendition=200x200_JPG_Q85&fileType=binary",
            result.imageUrl,
        )
        // The leading zero of the GTIN-14 is stripped to give a GTIN-13.
        assertEquals("8710448694632", result.gtin13)
        assertEquals("https://www.ah.nl/producten/product/wi194759/remia-friteslijn", result.productUrl)
    }

    @Test
    fun parsesProductWithoutTradeItemWithoutFailing() {
        val result = parser.parse(product(noTradeItem))

        assertEquals("Ariel Excel tabs alpine", result.name)
        assertEquals(0.0, result.kcalPer100g)
        assertEquals(100.0, result.servingSizeGrams)
        assertNull(result.gtin13)
        assertNull(result.imageUrl)
    }

    @Test
    fun fallsBackToIdWhenWebPathIsMissing() {
        val result = parser.parse(product("""{ "id": 194759, "title": "Remia Friteslijn" }"""))

        assertEquals("https://www.ah.nl/producten/product/wi194759", result.productUrl)
    }

    @Test
    fun searchDropsVirtualBundles() = runTest {
        // Bundles ("3-pack") carry no tradeItem, so they must never reach the results.
        val searchJson = """
            {
              "page": { "size": 20, "totalElements": 4, "totalPages": 1, "number": 0 },
              "products": [
                { "webshopId": 194759, "title": "Remia Friteslijn", "isVirtualBundle": false },
                { "webshopId": 609662, "title": "Remia Friteslijn 3-pack", "isVirtualBundle": true },
                { "webshopId": 608455, "title": "Remia Friteslijn 8-pack", "isVirtualBundle": true },
                { "webshopId": 196986, "title": "Ariel Excel tabs alpine" }
              ]
            }
        """.trimIndent()

        val api = object : AlbertHeijnApi() {
            override suspend fun fetchTokenJson(): String = """{"access_token":"t","expires_in":604798}"""
            override suspend fun fetchSearchJson(query: String, size: Int, token: String): String = searchJson
        }

        val hits = api.searchProducts("friteslijn")

        assertEquals(listOf(194759, 196986), hits.map { it.webshopId })
        assertEquals("Remia Friteslijn", hits.first().title)
    }

    @Test
    fun searchBatchesDetailAndKeepsRelevanceOrder() = runTest {
        var requestedIds: List<Int>? = null
        val api = object : AlbertHeijnApi() {
            override suspend fun searchProducts(query: String, size: Int) = listOf(
                SearchHit(194759, "Remia Friteslijn"),
                SearchHit(196986, "Ariel Excel tabs alpine"),
            )

            // Returned in the opposite order on purpose: search relevance must win, not map order.
            override suspend fun fetchProducts(ids: List<Int>): Map<Int, JsonObject> {
                requestedIds = ids
                return linkedMapOf(
                    196986 to product(noTradeItem),
                    194759 to product(friteslijn),
                )
            }
        }

        val results = AlbertHeijnSearch(api).search("friteslijn")

        assertEquals(listOf(194759, 196986), requestedIds, "detail must be fetched in one batched call")
        assertEquals(listOf("Remia Friteslijn", "Ariel Excel tabs alpine"), results.map { it.name })
        assertEquals(99.0, results.first().energyKcalPer100g)
        assertEquals("8710448694632", results.first().gtin13)
    }

    @Test
    fun searchCapsResultsAndSkipsProductsMissingFromDetail() = runTest {
        val hits = (1..12).map { AlbertHeijnApi.SearchHit(it, "Product $it") }
        var requestedIds: List<Int>? = null
        val api = object : AlbertHeijnApi() {
            override suspend fun searchProducts(query: String, size: Int) = hits

            override suspend fun fetchProducts(ids: List<Int>): Map<Int, JsonObject> {
                requestedIds = ids
                // Albert Heijn answered for only two of the five requested products.
                return mapOf(1 to product(friteslijn), 3 to product(noTradeItem))
            }
        }

        val results = AlbertHeijnSearch(api).search("melk")

        assertEquals(5, requestedIds?.size, "must not request detail for more than it shows")
        assertEquals(listOf("Remia Friteslijn", "Ariel Excel tabs alpine"), results.map { it.name })
    }

    @Test
    fun blankQueryMakesNoRequests() = runTest {
        val api = object : AlbertHeijnApi() {
            override suspend fun searchProducts(query: String, size: Int): List<SearchHit> =
                throw AssertionError("must not search for a blank query")
        }

        assertTrue(AlbertHeijnSearch(api).search("   ").isEmpty())
    }
}
