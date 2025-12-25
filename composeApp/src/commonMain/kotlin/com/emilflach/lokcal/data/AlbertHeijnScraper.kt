package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlin.math.floor

/**
 * Scrapes an AH product page and extracts key fields using regex.
 * This keeps dependencies light and avoids coupling to AH's internal JSON structures.
 */
open class AlbertHeijnScraper(
    private val client: HttpClient = defaultClient
) {
    companion object {
        private val defaultClient by lazy {
            HttpClient {
                install(Logging) {
                    level = LogLevel.INFO
                }
            }
        }
    }

    data class FoodScrapeResult(
        val name: String?,
        val kcalPer100g: Double?,
        val servingSizeGrams: Double?,
        val imageUrl: String?,
        val productUrl: String,
        val gtin13: String?,
    )

    open suspend fun fetchHtml(url: String): String {
        val resp = client.get(url)
        if (!resp.status.isSuccess()) error("Failed to fetch AH page: ${'$'}{resp.status}")
        return resp.body()
    }

    suspend fun scrape(url: String): FoodScrapeResult {
        val html = fetchHtml(url)

        val apolloState = extractApolloState(html)
        val productId = regexGroup(url, "product/(?:wi)?(\\d+)")
        val productJson = apolloState?.let { findProductInState(it, productId!!) }

        val name = extractName(productJson)
        val kcal = extractKcal(productJson)
        val servingGrams = extractServingSize(productJson)
        val image = extractImageUrl(productJson)
        val gtin = extractGtin(productJson)

        return FoodScrapeResult(
            name = name,
            kcalPer100g = kcal,
            servingSizeGrams = servingGrams,
            imageUrl = image,
            productUrl = url,
            gtin13 = gtin,
        )
    }

    private fun extractName(productJson: JsonObject?): String = unescapeHtml(productJson?.get("title")?.jsonPrimitive?.contentOrNull)

    private fun extractGtin(productJson: JsonObject?): String?  =
         productJson?.get("tradeItem")?.jsonObject?.get("gtin")?.jsonPrimitive?.contentOrNull?.removePrefix("0")

    private fun extractImageUrl(productJson: JsonObject?): String {
        return unescapeUrl(productJson?.get("imagePack")?.jsonArray?.firstOrNull()?.jsonObject?.get("small")?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull)
    }

    private fun extractKcal(product: JsonObject?): Double {
        if (product == null) return 0.0
        
        // Strategy 1: Look for tradeItem -> nutritions (New structure)
        product["tradeItem"]?.jsonObject?.get("nutritions")?.jsonArray?.let { nutritions ->
            val hundredEntry = nutritions.find {
                val basis = it.jsonObject["basisQuantity"]?.jsonPrimitive?.contentOrNull ?: ""
                basis.startsWith("100.0", ignoreCase = true)
            }?.jsonObject
            
            hundredEntry?.get("nutrients")?.jsonArray?.let { nutrients ->
                val energy = nutrients.find {
                    val type = it.jsonObject["type"]?.jsonPrimitive?.contentOrNull ?: ""
                    type == "ENER-"
                }?.jsonObject
                
                val value = energy?.get("value")?.jsonPrimitive?.contentOrNull
                if (value != null) {
                    return floor(regexGroup(value, "([0-9.]+)\\s*kcal", ignoreCase = true)?.toDoubleOrNull() ?: 0.0)
                }
            }
        }

        return 0.0
    }

    private fun extractServingSize(product: JsonObject?): Double {
        product?.get("tradeItem")?.jsonObject?.get("nutritions")?.jsonArray?.let { nutritions ->
            val nonHundredEntry = nutritions.find {
                val basis = it.jsonObject["basisQuantity"]?.jsonPrimitive?.contentOrNull ?: ""
                !basis.startsWith("100.0", ignoreCase = true)
            }?.jsonObject

            nonHundredEntry?.get("basisQuantity")?.jsonPrimitive?.contentOrNull?.let {
                return floor(regexGroup(it, "([0-9.]+)")?.toDoubleOrNull() ?: 100.0)
            }
        }
        return 100.0
    }

    private fun extractApolloState(html: String): JsonObject? {
        val startMarker = "window.__APOLLO_STATE__="
        val startIndex = html.indexOf(startMarker)
        if (startIndex == -1) {
            return null
        }

        val jsonStart = html.indexOf("{", startIndex)
        if (jsonStart == -1) {
            return null
        }

        // Find the end of the JSON object by balancing braces
        var braceCount = 0
        var jsonEnd = -1
        for (i in jsonStart until html.length) {
            when (html[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        jsonEnd = i + 1
                        break
                    }
                }
            }
        }

        if (jsonEnd == -1) {
            return null
        }
        val jsonString = html.substring(jsonStart, jsonEnd)

        return try {
            Json.decodeFromString<JsonObject>(jsonString)
        } catch (e: Exception) {
            println("[DEBUG_LOG] JSON parsing failed: ${e.message}")
            // Fallback to simpler regex if manual parsing fails for some reason
            regexGroup(html, "window\\.__APOLLO_STATE__\\s*=\\s*(\\{.*?\\});", ignoreCase = true)?.let {
                try { Json.decodeFromString<JsonObject>(it) } catch (_: Exception) { null }
            }
        }
    }

    private fun findProductInState(state: JsonObject, productId: String): JsonObject? {
        // Look for keys like "Product:194759"
        val key = "Product:$productId"
        if (state.containsKey(key)) {
            val result = state[key]?.jsonObject
            return result
        } else {
            return null
        }
    }

    private fun regexGroup(text: String, pattern: String, ignoreCase: Boolean = false): String? {
        val re = Regex(pattern, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
        val m = re.find(text) ?: return null
        return m.groupValues.getOrNull(1)
    }

    private fun unescapeUrl(url: String?): String =
        url?.replace("\\u002F", "/") ?: ""

    private fun unescapeHtml(s: String?): String {
        val out = s.orEmpty()
        // Common named entities
        return out.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
    }
}
