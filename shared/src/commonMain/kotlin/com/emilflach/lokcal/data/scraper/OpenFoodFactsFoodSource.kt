package com.emilflach.lokcal.data.scraper

import com.emilflach.lokcal.data.OnlineFoodItem
import com.emilflach.lokcal.data.OpenFoodFactsSearch

/**
 * OpenFoodFacts API food source implementation.
 * Global, open-source food database.
 */
class OpenFoodFactsFoodSource : FoodSource {
    override val id = "off"
    override val displayName = "OpenFoodFacts"
    override val description = "Open database with food products from around the world"
    override val country = "Global"  // Global
    override val type = SourceType.API
    override val rateLimitSeconds = 10

    private val search = OpenFoodFactsSearch()

    override suspend fun search(query: String): List<OnlineFoodItem> {
        return search.search(query)
    }

    override suspend fun canHandle(url: String): Boolean {
        return url.contains("openfoodfacts.org", ignoreCase = true) ||
               url.contains("openfoodfacts.net", ignoreCase = true)
    }

    override suspend fun scrapeUrl(url: String): OnlineFoodItem? {
        // OpenFoodFacts doesn't need URL scraping - use barcode from URL if available
        val barcode = url.split("/").lastOrNull { it.all { c -> c.isDigit() } }
        return if (barcode != null && barcode.length == 13) {
            search.search(barcode).firstOrNull()
        } else {
            null
        }
    }
}
