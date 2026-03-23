package com.emilflach.lokcal.data.sources

import com.emilflach.lokcal.data.EsselungaSearch
import com.emilflach.lokcal.data.EsselungaWebFetcher
import com.emilflach.lokcal.data.OnlineFoodItem

/**
 * Esselunga food source implementation.
 * Searches the Esselunga Italy website and fetches product pages.
 */
class EsselungaFoodSource : FoodSource {
    override val id = "esselunga"
    override val displayName = "Esselunga"
    override val description = "Italian supermarket chain with product information"
    override val country = "IT"
    override val type = SourceType.WEB
    override val rateLimitSeconds = 10

    private val search = EsselungaSearch()
    private val fetcher = EsselungaWebFetcher()

    override suspend fun search(query: String): List<OnlineFoodItem> {
        return search.search(query)
    }

    override suspend fun scrapeUrl(url: String): OnlineFoodItem? {
        return try {
            val result = fetcher.fetchProduct(url)
            OnlineFoodItem(
                name = result.name ?: url,
                gtin13 = result.gtin13,
                energyKcalPer100g = result.kcalPer100g,
                servingSize = result.servingSizeGrams,
                productUrl = result.productUrl,
                imageUrl = result.imageUrl,
                dutchName = null
            )
        } catch (_: Exception) {
            null
        }
    }
}
