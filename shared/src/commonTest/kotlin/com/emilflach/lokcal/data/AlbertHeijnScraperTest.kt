package com.emilflach.lokcal.data

import kotlinx.coroutines.test.runTest
import lokcal.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals

class AlbertHeijnScraperTest {

    private val htmlContent = """
        <!doctype html>
        <html>
            <body>
                <script>
                    window.__APOLLO_STATE__= {
                        "Product:194759": {
                            "__typename": "Product",
                            "id": 194759,
                            "title": "Remia Friteslijn",
                            "tradeItem": {
                                "gtin": "08710448694632",
                                "nutritions": [
                                    {
                                        "basisQuantity": "100.0 Milliliter",
                                        "nutrients": [
                                            {
                                                "type": "ENER-",
                                                "value": "410.0 kJ (99.0 kcal)"
                                            }
                                        ]
                                    },
                                    {
                                        "basisQuantity": "15.0 Milliliter",
                                        "nutrients": [
                                            {
                                                "type": "ENER-",
                                                "value": "62.0 kJ (15.0 kcal)"
                                            }
                                        ]
                                    }
                                ]
                            },
                            "imagePack": [
                                {
                                    "small": {
                                        "url": "https:\u002F\u002Fstatic.ah.nl\u002Fdam\u002Fproduct\u002FAHI_4354523130313437373539?revLabel=1&rendition=200x200_JPG_Q85&fileType=binary"
                                    }
                                }
                            ]
                        }
                    };
                </script>
            </body>
        </html>
    """.trimIndent()

    @Test
    fun testScrapeFromJsonWithInjectedHtml() = runTest {
        val scraperWithMock = object : AlbertHeijnScraper() {
            override suspend fun fetchHtml(url: String): String = htmlContent
        }
        val result = scraperWithMock.scrape("https://www.ah.nl/producten/product/wi194759/remia-friteslijn")

        assertEquals("Remia Friteslijn", result.name)
        assertEquals(99.0, result.kcalPer100g)
        assertEquals(15.0, result.servingSizeGrams)
        assertEquals("https://static.ah.nl/dam/product/AHI_4354523130313437373539?revLabel=1&rendition=200x200_JPG_Q85&fileType=binary", result.imageUrl)
        assertEquals("8710448694632", result.gtin13)
    }


    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testScrapeFromRealHtmlFile() = runTest {
        val realHtml = try {
            Res.readBytes("files/albertheijn/friteslijn.html").decodeToString()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Skipping test on this platform due to resource loading limitation: ${e.message}")
            return@runTest
        }
        
        val scraperWithMock = object : AlbertHeijnScraper() {
            override suspend fun fetchHtml(url: String): String = realHtml
        }
        val result = scraperWithMock.scrape("https://www.ah.nl/producten/product/wi194759/remia-friteslijn")

        assertEquals("Remia Friteslijn", result.name)
        assertEquals(99.0, result.kcalPer100g)
        assertEquals("8710448694632", result.gtin13)
        assertEquals("https://static.ah.nl/dam/product/AHI_4354523130313437373539?revLabel=1&rendition=200x200_JPG_Q85&fileType=binary", result.imageUrl)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testScrapeFritessausClassicFromRealHtmlFile() = runTest {
        val realHtml = try {
            Res.readBytes("files/albertheijn/fritessaus_classic.html").decodeToString()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Skipping test on this platform due to resource loading limitation: ${e.message}")
            return@runTest
        }

        val scraperWithMock = object : AlbertHeijnScraper() {
            override suspend fun fetchHtml(url: String): String = realHtml
        }
        val result = scraperWithMock.scrape("https://www.ah.nl/producten/product/wi194760/remia-fritessaus-classic")

        assertEquals("Remia Fritessaus classic", result.name)
        assertEquals(300.0, result.kcalPer100g)
        assertEquals("8710448677611", result.gtin13)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testSearchExtractsTopProductLinks() = runTest {
        val searchHtml = try {
            Res.readBytes("files/albertheijn/search_friteslijn.html").decodeToString()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Skipping test on this platform due to resource loading limitation: ${e.message}")
            return@runTest
        }

        // Mock the scraper to return product HTML
        val mockScraper = object : AlbertHeijnScraper() {
            override suspend fun fetchHtml(url: String): String = htmlContent
        }

        // Test the extractTopProductLinks function by creating a custom AlbertHeijnSearch class
        val testSearch = object : AlbertHeijnSearch(scraper = mockScraper) {
            override suspend fun fetchSearchHtml(url: String): String = searchHtml
        }

        val results = testSearch.search("friteslijn")

        // Should extract at least 4 products from the search results
        assertEquals(true, results.size >= 4, "Expected at least 4 results, got ${results.size}")
        assertEquals("Remia Friteslijn", results[0].name)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testSearchTortillaExtractsTopProductLinks() = runTest {
        val searchHtml = try {
            Res.readBytes("files/albertheijn/search_tortilla.html").decodeToString()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Skipping test on this platform due to resource loading limitation: ${e.message}")
            return@runTest
        }

        // Mock the scraper to return product HTML
        val mockScraper = object : AlbertHeijnScraper() {
            override suspend fun fetchHtml(url: String): String = htmlContent
        }

        // Test the extractTopProductLinks function by creating a custom AlbertHeijnSearch class
        val testSearch = object : AlbertHeijnSearch(scraper = mockScraper) {
            override suspend fun fetchSearchHtml(url: String): String = searchHtml
        }

        val results = testSearch.search("tortilla")

        // Should extract at least 5 products from the search results
        assertEquals(true, results.size >= 5, "Expected at least 5 results, got ${results.size}")
        assertEquals("Remia Friteslijn", results[0].name)
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun testSearchLaMorenaExtractsTopProductLinks() = runTest {
        val searchHtml = try {
            Res.readBytes("files/albertheijn/search_lamorena.html").decodeToString()
        } catch (e: Exception) {
            println("[DEBUG_LOG] Skipping test on this platform due to resource loading limitation: ${e.message}")
            return@runTest
        }

        println("[DEBUG] HTML length: ${searchHtml.length}")
        println("[DEBUG] Contains 'webPath': ${searchHtml.contains("webPath")}")
        println("[DEBUG] Contains product link: ${searchHtml.contains("/producten/product/wi450151")}")

        // Mock the scraper to return product HTML
        val mockScraper = object : AlbertHeijnScraper() {
            override suspend fun fetchHtml(url: String): String = htmlContent
        }

        // Test the extractTopProductLinks function by creating a custom AlbertHeijnSearch class
        val testSearch = object : AlbertHeijnSearch(scraper = mockScraper) {
            override suspend fun fetchSearchHtml(url: String): String = searchHtml
        }

        val results = testSearch.search("lamorena")

        // Should extract at least 5 products from the search results
        assertEquals(true, results.size >= 5, "Expected at least 5 results, got ${results.size}")
        // All products should be scraped with the mocked scraper returning Friteslijn data
        assertEquals("Remia Friteslijn", results[0].name)
    }
}
