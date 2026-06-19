package com.emilflach.lokcal.data.sources

import com.emilflach.lokcal.data.KrogerSearch
import com.emilflach.lokcal.data.OnlineFoodItem

/**
 * Kroger food source using the Kroger Products API.
 * Covers Kroger and its subsidiary banners (e.g. Fred Meyer, Ralphs, Harris Teeter).
 */
class KrogerFoodSource : FoodSource {
    override val id = "kroger"
    override val displayName = "Kroger"
    override val description = "US grocery chain with product nutrition data via the Kroger API"
    override val country = "US"
    override val type = SourceType.API
    override val rateLimitSeconds = 10

    private val search = KrogerSearch()

    override suspend fun search(query: String): List<OnlineFoodItem> = search.search(query)

    override suspend fun scrapeUrl(url: String): OnlineFoodItem? {
        val productId = search.productIdFromUrl(url) ?: return null
        return search.fetchById(productId)
    }
}
