package com.emilflach.lokcal.data.sources

import com.emilflach.lokcal.data.AlbertHeijnApi
import com.emilflach.lokcal.data.AlbertHeijnProductParser
import com.emilflach.lokcal.data.AlbertHeijnSearch
import com.emilflach.lokcal.data.OnlineFoodItem

/**
 * Albert Heijn food source, backed by the Appie mobile API (see [AlbertHeijnApi]).
 *
 * [api] is injectable so the nightly liveness check can exercise this exact path with wider request
 * spacing.
 */
class AlbertHeijnFoodSource(
    private val api: AlbertHeijnApi = AlbertHeijnApi(),
) : FoodSource {
    companion object {
        /** Matches the numeric product id in an ah.nl product URL, with or without the `wi` prefix. */
        private val productIdFromUrlRegex = Regex("""product/(?:wi)?(\d+)""")
    }

    override val id = "ah"
    override val displayName = "Albert Heijn"
    override val description = "Dutch supermarket chain with product information"
    override val country = "NL"
    override val type = SourceType.API
    override val rateLimitSeconds = 10

    private val parser = AlbertHeijnProductParser()

    // Shares [api] with search so the anonymous token is fetched once per process, not per call.
    private val search = AlbertHeijnSearch(api, parser)

    override suspend fun search(query: String): List<OnlineFoodItem> {
        return search.search(query)
    }

    override suspend fun scrapeUrl(url: String): OnlineFoodItem? {
        val productId = productIdFromUrlRegex.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return null
        return try {
            val result = api.fetchProduct(productId)?.let { parser.parse(it) } ?: return null
            OnlineFoodItem(
                name = result.name ?: url,
                gtin13 = result.gtin13,
                energyKcalPer100g = result.kcalPer100g,
                servingSize = result.servingSizeGrams,
                productUrl = result.productUrl,
                imageUrl = result.imageUrl,
                dutchName = result.name,
            )
        } catch (_: Exception) {
            null
        }
    }
}
