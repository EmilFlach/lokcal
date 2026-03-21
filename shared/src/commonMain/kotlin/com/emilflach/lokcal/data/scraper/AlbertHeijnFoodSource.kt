package com.emilflach.lokcal.data.scraper

import com.emilflach.lokcal.data.AlbertHeijnScraper
import com.emilflach.lokcal.data.AlbertHeijnSearch
import com.emilflach.lokcal.data.OnlineFoodItem

/**
 * Albert Heijn food source implementation.
 * Searches the AH website and scrapes product pages.
 */
class AlbertHeijnFoodSource : FoodSource {
    override val id = "ah"
    override val displayName = "Albert Heijn"
    override val description = "Dutch supermarket chain with product information"
    override val country = "NL"
    override val type = SourceType.SCRAPER
    override val rateLimitSeconds = 10

    private val search = AlbertHeijnSearch()
    private val scraper = AlbertHeijnScraper()

    override suspend fun search(query: String): List<OnlineFoodItem> {
        return search.search(query)
    }

    override suspend fun canHandle(url: String): Boolean {
        return url.contains("ah.nl", ignoreCase = true)
    }

    override suspend fun scrapeUrl(url: String): OnlineFoodItem? {
        return try {
            val result = scraper.scrape(url)
            OnlineFoodItem(
                name = result.name ?: url,
                gtin13 = result.gtin13,
                energyKcalPer100g = result.kcalPer100g,
                servingSize = result.servingSizeGrams,
                productUrl = result.productUrl,
                imageUrl = result.imageUrl,
                dutchName = result.name
            )
        } catch (e: Exception) {
            null
        }
    }
}
