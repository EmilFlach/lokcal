package com.emilflach.lokcal.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KrogerSearchTest {

    private val searchResponseWithNutrition = """
        {
          "data": [
            {
              "productId": "0001111041700",
              "productPageURI": "/p/kroger-2-reduced-fat-milk/0001111041700?cid=test",
              "description": "Kroger 2% Reduced Fat Milk",
              "brand": "Kroger",
              "upc": "0001111041700",
              "images": [
                {
                  "perspective": "front",
                  "default": true,
                  "sizes": [
                    { "size": "thumbnail", "url": "https://www.kroger.com/product/images/thumbnail/front/0001111041700" },
                    { "size": "medium", "url": "https://www.kroger.com/product/images/medium/front/0001111041700" }
                  ]
                }
              ],
              "nutritionInformation": {
                "servingSize": {
                  "quantity": 240,
                  "unitOfMeasure": { "abbreviation": "ml", "code": "MLT", "name": "Millilitre" }
                },
                "nutrients": [
                  {
                    "code": "ENER-",
                    "description": "calories",
                    "displayName": "Calories",
                    "quantity": 130,
                    "unitOfMeasure": { "abbreviation": "kcal", "code": "KCAL" }
                  },
                  {
                    "code": "FAT",
                    "description": "total fat",
                    "displayName": "Total Fat",
                    "quantity": 5,
                    "unitOfMeasure": { "abbreviation": "g", "code": "GRM" }
                  }
                ]
              }
            }
          ],
          "meta": {}
        }
    """.trimIndent()

    private val searchResponseNoNutrition = """
        {
          "data": [
            {
              "productId": "0001234567890",
              "description": "Some Product",
              "upc": "0001234567890"
            }
          ],
          "meta": {}
        }
    """.trimIndent()

    // Matches real Kroger API format: nutritionInformation is an array, nutrients have no
    // "abbreviation" on unitOfMeasure, and serving size is in oz.
    private val singleProductResponse = """
        {
          "data": {
            "productId": "0001111041700",
            "productPageURI": "/p/kroger-2-reduced-fat-milk/0001111041700",
            "description": "Kroger 2% Reduced Fat Milk",
            "upc": "0001111041700",
            "images": [
              {
                "perspective": "front",
                "default": true,
                "sizes": [
                  { "size": "medium", "url": "https://www.kroger.com/product/images/medium/front/0001111041700" }
                ]
              }
            ],
            "nutritionInformation": [
              {
                "servingSize": {
                  "quantity": 1,
                  "unitOfMeasure": { "abbreviation": "oz", "code": "ONZ", "name": "Ounce" }
                },
                "nutrients": [
                  {
                    "code": "ENER-",
                    "description": "energy; method of determination unknown or variable",
                    "displayName": "Calories",
                    "quantity": 100,
                    "unitOfMeasure": { "code": "E14", "name": "Kilocalorie (international table)" }
                  },
                  {
                    "code": "ENERPF",
                    "description": "energy, percent contributed by fat",
                    "displayName": "Calories from Fat",
                    "quantity": 70,
                    "unitOfMeasure": { "code": "E14", "name": "Kilocalorie (international table)" }
                  }
                ]
              }
            ]
          },
          "meta": {}
        }
    """.trimIndent()

    // Pack/piece serving unit — serving size must be estimated from macros
    private val packServingProductResponse = """
        {
          "data": {
            "productId": "0003400008788",
            "description": "KIT KAT Snack Size",
            "upc": "0003400008788",
            "nutritionInformation": [
              {
                "servingSize": {
                  "quantity": 2,
                  "unitOfMeasure": { "abbreviation": "pk", "code": "PK", "name": "Pack" }
                },
                "nutrients": [
                  {
                    "code": "ENER-",
                    "displayName": "Calories",
                    "quantity": 140,
                    "unitOfMeasure": { "code": "E14", "name": "Kilocalorie (international table)" }
                  },
                  {
                    "code": "FAT",
                    "displayName": "Total Fat",
                    "quantity": 7,
                    "unitOfMeasure": { "code": "GRM", "name": "Gram" }
                  },
                  {
                    "code": "PRO-",
                    "displayName": "Protein",
                    "quantity": 1,
                    "unitOfMeasure": { "code": "GRM", "name": "Gram" }
                  },
                  {
                    "code": "CHO-",
                    "displayName": "Total Carbohydrate",
                    "quantity": 19,
                    "unitOfMeasure": { "code": "GRM", "name": "Gram" }
                  }
                ]
              }
            ]
          },
          "meta": {}
        }
    """.trimIndent()

    private fun makeSearch(
        searchJson: String = searchResponseWithNutrition,
        productJson: String? = singleProductResponse,
    ) = object : KrogerSearch() {
        override suspend fun fetchSearchJson(query: String): String = searchJson
        override suspend fun fetchProductJson(productId: String): String? = productJson
    }

    @Test
    fun testSearchReturnsProducts() = runTest {
        val results = makeSearch().search("milk")
        assertEquals(1, results.size)
        assertEquals("Kroger 2% Reduced Fat Milk", results[0].name)
    }

    @Test
    fun testSearchConvertsKcalPer100g() = runTest {
        val results = makeSearch().search("milk")
        // 100 kcal per 1 oz serving; 1 oz = 28.3495 g → 100 / 28.3495 * 100
        val expected = (100.0 / (1.0 * 28.3495)) * 100.0
        assertEquals(expected, results[0].energyKcalPer100g)
    }

    @Test
    fun testSearchExtractsServingSize() = runTest {
        val results = makeSearch().search("milk")
        // 1 oz → 28.3495 g
        assertEquals(1.0 * 28.3495, results[0].servingSize)
    }

    @Test
    fun testSearchExtractsGtin13() = runTest {
        val results = makeSearch().search("milk")
        assertEquals("0001111041700", results[0].gtin13)
    }

    @Test
    fun testSearchExtractsProductUrl() = runTest {
        val results = makeSearch().search("milk")
        assertEquals("https://www.kroger.com/p/kroger-2-reduced-fat-milk/0001111041700", results[0].productUrl)
    }

    @Test
    fun testSearchExtractsMediumImageUrl() = runTest {
        val results = makeSearch().search("milk")
        assertEquals("https://www.kroger.com/product/images/medium/front/0001111041700", results[0].imageUrl)
    }

    @Test
    fun testSearchFiltersOutNoNutritionResults() = runTest {
        // Products with no kcal data are filtered from results
        val results = makeSearch(searchJson = searchResponseNoNutrition, productJson = null).search("product")
        assertEquals(0, results.size)
    }

    @Test
    fun testSearchEmptyQueryReturnsEmpty() = runTest {
        val search = makeSearch()
        assertTrue(search.search("").isEmpty())
        assertTrue(search.search("   ").isEmpty())
    }

    @Test
    fun testFetchByIdReturnsSingleProduct() = runTest {
        val result = makeSearch().fetchById("0001111041700")
        assertEquals("Kroger 2% Reduced Fat Milk", result?.name)
        val expected = (100.0 / (1.0 * 28.3495)) * 100.0
        assertEquals(expected, result?.energyKcalPer100g)
    }

    @Test
    fun testFetchByIdReturnsNullOnFailure() = runTest {
        val result = makeSearch(productJson = null).fetchById("0001111041700")
        assertNull(result)
    }

    @Test
    fun testProductIdFromUrl() {
        val search = makeSearch()
        assertEquals(
            "0001111041700",
            search.productIdFromUrl("https://www.kroger.com/p/kroger-milk/0001111041700")
        )
        assertEquals(
            "0001111041700",
            search.productIdFromUrl("https://www.kroger.com/p/kroger-milk/0001111041700?cid=test")
        )
        assertNull(search.productIdFromUrl("https://www.kroger.com/p/some-product/"))
    }

    @Test
    fun testBarcodeSearchUsesBarcodeParam() = runTest {
        var capturedQuery: String? = null
        val search = object : KrogerSearch() {
            override suspend fun fetchSearchJson(query: String): String {
                capturedQuery = query
                return searchResponseWithNutrition
            }
            override suspend fun fetchProductJson(productId: String): String? = null
        }
        search.search("0001111041700")
        assertEquals("0001111041700", capturedQuery)
    }

    @Test
    fun testPicksEnergyNutrientNotCaloriesFromFat() = runTest {
        // ENER- (100 kcal) must be chosen, not ENERPF (70 kcal "calories from fat")
        val result = makeSearch().fetchById("0001111041700")
        val expected = (100.0 / (1.0 * 28.3495)) * 100.0
        assertEquals(expected, result?.energyKcalPer100g)
    }

    @Test
    fun testStripsQueryParamsFromProductUrl() = runTest {
        val results = makeSearch().search("milk")
        // productPageURI has ?cid=test — should be stripped
        assertEquals("https://www.kroger.com/p/kroger-2-reduced-fat-milk/0001111041700", results[0].productUrl)
    }

    @Test
    fun testPackServingUnitFallsBackToMacroEstimate() = runTest {
        // serving unit is "pk" (pack) — estimate from fat(7) + protein(1) + carbs(19) = 27g
        val result = makeSearch(productJson = packServingProductResponse).fetchById("0003400008788")
        val estimatedServingG = 7.0 + 1.0 + 19.0  // fat + protein + carbs
        val expectedKcal = (140.0 / estimatedServingG) * 100.0
        assertEquals(estimatedServingG, result?.servingSize)
        assertEquals(expectedKcal, result?.energyKcalPer100g)
    }
}
