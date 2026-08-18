package com.emilflach.lokcal.data

/**
 * Searches Albert Heijn and returns the top results as [OnlineFoodItem]s.
 *
 * Two requests per search: one relevance search for candidate ids, then one batched GraphQL call
 * that fetches nutrition, GTIN and imagery for all of them at once.
 */
open class AlbertHeijnSearch(
    private val api: AlbertHeijnApi = AlbertHeijnApi(),
    private val parser: AlbertHeijnProductParser = AlbertHeijnProductParser(),
) {
    companion object {
        /** How many results to surface. */
        private const val MAX_RESULTS = 5

        /**
         * How many to ask Albert Heijn for. Deliberately larger than [MAX_RESULTS]: virtual bundles
         * get filtered out, and for staples like "melk" they can crowd out most of a small page.
         */
        private const val SEARCH_SIZE = 20
    }

    open suspend fun search(query: String): List<OnlineFoodItem> {
        if (query.isBlank()) return emptyList()

        val hits = api.searchProducts(query, SEARCH_SIZE).take(MAX_RESULTS)
        if (hits.isEmpty()) return emptyList()

        val products = api.fetchProducts(hits.map { it.webshopId })

        // Keep Albert Heijn's relevance order rather than the map's.
        return hits.mapNotNull { hit ->
            products[hit.webshopId]?.let { toOnlineFoodItem(parser.parse(it), fallbackName = hit.title) }
        }
    }

    private fun toOnlineFoodItem(
        result: AlbertHeijnProductParser.FoodFetchResult,
        fallbackName: String,
    ) = OnlineFoodItem(
        name = result.name ?: fallbackName,
        gtin13 = result.gtin13,
        energyKcalPer100g = result.kcalPer100g,
        servingSize = result.servingSizeGrams,
        productUrl = result.productUrl,
        imageUrl = result.imageUrl,
        dutchName = result.name,
    )
}
