package com.emilflach.lokcal.data

import kotlinx.coroutines.test.runTest
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
}
