package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json

/**
 * Searches the Albert Heijn website and returns up to the top 3 product results as OffItem models.
 * For each result, we scrape the product page using the existing AlbertHeijnScraper to obtain
 * detailed fields (name, kcal/100g, serving size, image, gtin13).
 */
class AlbertHeijnSearch(
    private val scraper: AlbertHeijnScraper = AlbertHeijnScraper(),
    private val client: HttpClient = defaultClient,
) {
    companion object {
        private const val BASE = "https://www.ah.nl"
        private val defaultClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                install(Logging) { level = LogLevel.INFO }
                install(HttpTimeout) {
                    requestTimeoutMillis = 10000
                    connectTimeoutMillis = 10000
                    socketTimeoutMillis = 10000
                }
            }
        }
    }

    suspend fun search(query: String): List<OnlineFoodItem> {
        if (query.isBlank()) return emptyList()
        val url = "$BASE/zoeken?query=" + query.encodeURLQueryComponent()
        val html = client.get(url) {
            accept(ContentType.Text.Html)
            header(HttpHeaders.Referrer, BASE)
        }.body<String>()

        val links = extractTopProductLinks(html)
        if (links.isEmpty()) return emptyList()

        return coroutineScope {
            // scrape in parallel but keep ordering of top results
            links.map { link ->
                async {
                    val abs = if (link.startsWith("http")) link else BASE + link
                    runCatching { scraper.scrape(abs) }.getOrNull()?.let { s ->
                        OnlineFoodItem(
                            name = s.name ?: abs,
                            gtin13 = s.gtin13,
                            energyKcalPer100g = s.kcalPer100g,
                            servingSize = s.servingSizeGrams,
                            productUrl = s.productUrl,
                            imageUrl = s.imageUrl,
                            dutchName = s.name,
                        )
                    }
                }
            }.mapNotNull { it.await() }
        }
    }

    private fun extractTopProductLinks(html: String, max: Int = 5): List<String> {
        // Limit parsing strictly to the real search results lane to avoid Smart Promotions, etc.
        // Prefer the first occurrence of data-testhook="search-lane"; fall back to id="search-lane".
        val searchHookIdx = html.indexOf("data-testhook=\"search-lane\"")
        val searchIdIdx = html.indexOf("id=\"search-lane\"")
        val startIdx = listOf(searchHookIdx, searchIdIdx)
            .filter { it >= 0 }
            .minOrNull() ?: return emptyList()

        // End right before any non-search recommendation/promotions lanes if present to avoid grabbing unrelated cards
        val smartPromotionsIdx = html.indexOf("data-testid=\"smart-promotions\"", startIdx)
        val similarProductsIdx = html.indexOf("data-testid=\"similar-products-recommendations-lane\"", startIdx)
        val boundaries = listOf(smartPromotionsIdx, similarProductsIdx).filter { it >= 0 }
        val endIdx = if (boundaries.isEmpty()) html.length else boundaries.minOrNull() ?: html.length

        val scoped = html.substring(startIdx, endIdx)

        val results = mutableListOf<String>()
        val articleRegex = Regex("<article[^>]*class=\"[^\"]*product-card-portrait_root__[^\"]*\"[\\s\\S]*?</article>", RegexOption.IGNORE_CASE)
        val hrefRegex = Regex("<a[^>]+href=\"(/producten/product/wi[0-9]+/[^\"]*)\"", RegexOption.IGNORE_CASE)

        for (match in articleRegex.findAll(scoped)) {
            val block = match.value
            val href = hrefRegex.find(block)?.groupValues?.getOrNull(1)
            if (href != null) {
                results.add(href)
                if (results.size >= max) break
            }
        }
        return results
    }
}
