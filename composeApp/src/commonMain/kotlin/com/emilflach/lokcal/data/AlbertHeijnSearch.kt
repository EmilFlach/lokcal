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
open class AlbertHeijnSearch(
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

    open suspend fun search(query: String): List<OnlineFoodItem> {
        if (query.isBlank()) return emptyList()
        val url = "$BASE/zoeken?query=" + query.encodeURLParameter()
        val html = fetchSearchHtml(url)

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

    protected open suspend fun fetchSearchHtml(url: String): String {
        return client.get(url) {
            accept(ContentType.Text.Html)
            header(HttpHeaders.Referrer, BASE)
        }.body<String>()
    }

    private fun extractTopProductLinks(html: String, max: Int = 5): List<String> {
        // Search results are rendered client-side and embedded in the HTML via JSON
        // Extract product links using regex across the entire HTML
        val results = mutableListOf<String>()

        // Try a simpler pattern first
        val pattern = """"webPath":"(/producten/product/wi\d+/[^"]+)""""
        val webPathRegex = Regex(pattern)

        println("[extractTopProductLinks] HTML length: ${html.length}")
        println("[extractTopProductLinks] Pattern: $pattern")
        println("[extractTopProductLinks] Contains test string: ${html.contains(""""webPath":"/producten/product/wi""")}")

        for (match in webPathRegex.findAll(html)) {
            val path = match.groupValues.getOrNull(1)
            println("[extractTopProductLinks] Found match: $path")
            if (path != null && !results.contains(path)) {
                results.add(path)
                if (results.size >= max) break
            }
        }

        println("[extractTopProductLinks] Total results: ${results.size}")
        return results
    }
}
