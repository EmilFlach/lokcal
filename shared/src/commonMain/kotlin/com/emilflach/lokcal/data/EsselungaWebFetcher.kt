package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.math.floor

/**
 * Fetches an Esselunga product by code via the internal detail API.
 * GET /commerce/resources/displayable/detail/code/{code} returns JSON with
 * displayableProduct (name, barcode, imageURL) and informations[] containing
 * nutrition HTML for kcal extraction.
 */
open class EsselungaWebFetcher(
    private val client: HttpClient = defaultClient
) {
    companion object {
        private const val BASE = "https://spesaonline.esselunga.it"
        private const val DETAIL_URL = "$BASE/commerce/resources/displayable/detail/code"
        private val defaultClient by lazy {
            HttpClient {
                install(Logging) { level = LogLevel.INFO }
            }
        }
        private val codeFromUrlRegex = Regex("""/prodotto/(\d+)/""")
    }

    data class FoodFetchResult(
        val name: String?,
        val kcalPer100g: Double?,
        val servingSizeGrams: Double?,
        val imageUrl: String?,
        val productUrl: String,
        val gtin13: String?,
    )

    open suspend fun fetchDetailJson(code: String): String {
        val url = "$DETAIL_URL/$code"
        val referer = "$BASE/commerce/nav/supermercato/store/prodotto/$code"
        val resp = client.get(url) {
            accept(ContentType.Application.Json)
            header("x-page-path", "supermercato")
            header(HttpHeaders.Referrer, referer)
        }
        if (!resp.status.isSuccess()) error("Failed to fetch Esselunga detail for code=$code: ${resp.status}")
        return resp.body()
    }

    suspend fun fetchProduct(url: String): FoodFetchResult {
        val code = codeFromUrlRegex.find(url)?.groupValues?.getOrNull(1)
            ?: error("Cannot extract product code from URL: $url")

        val json = runCatching { fetchDetailJson(code) }.getOrElse {
            return FoodFetchResult(null, null, null, null, url, null)
        }

        return parseDetailJson(json, url)
    }

    private fun parseDetailJson(json: String, url: String): FoodFetchResult {
        return try {
            val root = Json.parseToJsonElement(json).jsonObject
            val product = root["displayableProduct"]?.jsonObject
            val name = product?.get("description")?.jsonPrimitive?.contentOrNull?.let { stripWeight(it) }
            val gtin13 = product?.get("barcode")?.jsonPrimitive?.contentOrNull
            val imageUrl = product?.get("imageURL")?.jsonPrimitive?.contentOrNull

            val nutritionHtml = root["informations"]?.jsonArray
                ?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }
                ?.firstOrNull { it["label"]?.jsonPrimitive?.contentOrNull == "Valori nutrizionali" }
                ?.get("value")?.jsonPrimitive?.contentOrNull

            FoodFetchResult(
                name = name,
                kcalPer100g = nutritionHtml?.let { extractKcalFromHtml(it) },
                servingSizeGrams = nutritionHtml?.let { extractServingSizeFromHtml(it) },
                imageUrl = imageUrl,
                productUrl = url,
                gtin13 = gtin13,
            )
        } catch (e: Exception) {
            FoodFetchResult(null, null, null, null, url, null)
        }
    }

    private fun extractKcalFromHtml(html: String): Double? {
        val match = Regex("""(\d+(?:[.,]\d+)?)\s*kcal""", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.getOrNull(1)?.replace(",", ".")?.toDoubleOrNull()?.let { floor(it) }
    }

    private fun stripWeight(name: String): String =
        name.replace(Regex("""\s+\d+(?:[.,]\d+)?\s*(?:kg|g)\s*$""", RegexOption.IGNORE_CASE), "")

    private fun extractServingSizeFromHtml(html: String): Double? {
        // Matches "per porzione (50 g)" in the table header
        val match = Regex("""per porzione \((\d+(?:[.,]\d+)?)\s*g\)""", RegexOption.IGNORE_CASE).find(html)
        return match?.groupValues?.getOrNull(1)?.replace(",", ".")?.toDoubleOrNull()
    }
}
