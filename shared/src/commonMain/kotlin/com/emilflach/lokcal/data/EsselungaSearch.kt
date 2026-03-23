package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

/**
 * Searches Esselunga Italy via their internal facet/search API.
 * POST /commerce/resources/search/facet returns product entities in
 * displayables.entities[], each with code + sanitizeDescription to build
 * product URLs, and description + imageURL as fallbacks.
 */
open class EsselungaSearch(
    private val fetcher: EsselungaWebFetcher = EsselungaWebFetcher(),
    private val client: HttpClient = defaultClient,
) {
    companion object {
        private const val BASE = "https://spesaonline.esselunga.it"
        private const val FACET_URL = "$BASE/commerce/resources/search/facet"
        private val defaultClient by lazy {
            HttpClient {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                install(Logging) { level = LogLevel.INFO }
            }
        }
    }

    open suspend fun search(query: String): List<OnlineFoodItem> {
        if (query.isBlank()) return emptyList()

        val entities = fetchSearchResults(query)
        if (entities.isEmpty()) return emptyList()

        return coroutineScope {
            entities.take(5)
                .mapNotNull { entity ->
                    val code = entity["code"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val slug = entity["sanitizeDescription"]?.jsonPrimitive?.contentOrNull
                        ?: return@mapNotNull null
                    entity to "$BASE/commerce/nav/supermercato/store/prodotto/$code/$slug"
                }
                .map { (entity, url) ->
                    async {
                        val fallbackName = entity["description"]?.jsonPrimitive?.contentOrNull
                            ?.replace(Regex("""\s+\d+(?:[.,]\d+)?\s*(?:kg|g)\s*$""", RegexOption.IGNORE_CASE), "")
                        val fallbackImage = entity["imageURL"]?.jsonPrimitive?.contentOrNull
                        val r = runCatching { fetcher.fetchProduct(url) }.getOrNull()
                        OnlineFoodItem(
                            name = r?.name ?: fallbackName ?: url,
                            gtin13 = r?.gtin13,
                            energyKcalPer100g = r?.kcalPer100g,
                            servingSize = r?.servingSizeGrams,
                            productUrl = url,
                            imageUrl = r?.imageUrl ?: fallbackImage,
                            dutchName = null,
                        )
                    }
                }
                .awaitAll()
        }
    }

    protected open suspend fun fetchSearchResults(query: String): List<JsonObject> {
        val requestBody = buildJsonObject {
            put("query", query)
            put("start", 0)
            put("length", 5)
            put("isLargeQuerySearch", true)
            putJsonArray("filters") {}
        }.toString()
        return try {
            val responseText = client.post(FACET_URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json")
                header("x-page-path", "supermercato")
                header(HttpHeaders.Origin, BASE)
                header(HttpHeaders.Referrer, "$BASE/commerce/nav/supermercato/store/ricerca/$query")
                setBody(requestBody)
            }.body<String>()
            Json.parseToJsonElement(responseText)
                .jsonObject["displayables"]
                ?.jsonObject?.get("entities")
                ?.jsonArray
                ?.filterIsInstance<JsonObject>()
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
