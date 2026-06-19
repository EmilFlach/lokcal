package com.emilflach.lokcal.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.floor

/**
 * Fetches an AH product page and extracts key fields using regex.
 * This keeps dependencies light and avoids coupling to AH's internal JSON structures.
 */
open class AlbertHeijnWebFetcher(
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

    data class FoodFetchResult(
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

    suspend fun fetchProduct(url: String): FoodFetchResult {
        val html = fetchHtml(url)

        val apolloState = extractApolloState(html)
        val ldJson = extractLdJson(html)
        val productId = regexGroup(url, "product/(?:wi)?(\\d+)")
        val productJson = apolloState?.let { findProductInState(it, productId!!) }

        val name = extractName(productJson) ?: ldJson?.get("name")?.jsonPrimitive?.contentOrNull
        val kcal = extractKcal(productJson, html)
        val servingGrams = extractServingSize(productJson)
        val image = extractImageUrl(productJson) ?: ldJson?.get("image")?.jsonPrimitive?.contentOrNull?.let { unescapeUrl(it) }
        val gtin = extractGtin(productJson) ?: ldJson?.get("gtin13")?.jsonPrimitive?.contentOrNull

        return FoodFetchResult(
            name = cleanName(unescapeHtml(name)),
            kcalPer100g = kcal,
            servingSizeGrams = servingGrams,
            imageUrl = image,
            productUrl = url,
            gtin13 = gtin,
        )
    }

    private fun extractName(productJson: JsonObject?): String? = 
        productJson?.get("title")?.jsonPrimitive?.contentOrNull?.let { cleanName(it) }

    private fun cleanName(name: String): String =
        name.removeSuffix(" bestellen | Albert Heijn").trim()

    private fun extractGtin(productJson: JsonObject?): String?  =
         productJson?.get("tradeItem")?.jsonObject?.get("gtin")?.jsonPrimitive?.contentOrNull?.removePrefix("0")

    private fun extractImageUrl(productJson: JsonObject?): String? {
        return productJson?.get("imagePack")?.jsonArray?.firstOrNull()?.jsonObject?.get("small")?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull?.let { unescapeUrl(it) }
    }

    private fun extractKcal(product: JsonObject?, html: String): Double {
        // Strategy 1: Look for tradeItem -> nutritions (Apollo state structure)
        product?.get("tradeItem")?.jsonObject?.get("nutritions")?.jsonArray?.let { nutritions ->
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
                    val kcalValue = regexGroup(value, "([0-9.]+)\\s*kcal", ignoreCase = true)?.toDoubleOrNull()
                    if (kcalValue != null) return floor(kcalValue)
                }
            }
        }

        // Strategy 2: Direct regex from HTML (useful if Apollo state parsing fails or isn't there)
        // Look for common patterns like "410.0 kJ (99.0 kcal)"
        // We look for patterns where it specifies 100g/ml if possible, but fallback to any kcal
        val kcalMatch = regexGroup(html, "([0-9.]+)\\s*kcal", ignoreCase = true)
        if (kcalMatch != null) {
            return floor(kcalMatch.toDoubleOrNull() ?: 0.0)
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

    private fun extractLdJson(html: String): JsonObject? {
        val startMarker = "<script type=\"application/ld+json\">"
        val startIndex = html.indexOf(startMarker)
        if (startIndex == -1) {
            // Try with single quotes or extra spaces
            val altStartMarker = "<script type='application/ld+json'>"
            val altStartIndex = html.indexOf(altStartMarker)
            if (altStartIndex == -1) return null
            
            val jsonStart = altStartIndex + altStartMarker.length
            val jsonEnd = html.indexOf("</script>", jsonStart)
            if (jsonEnd == -1) return null
            val jsonString = html.substring(jsonStart, jsonEnd)
            return try {
                Json.decodeFromString<JsonObject>(jsonString)
            } catch (e: Exception) {
                null
            }
        }
        
        val jsonStart = startIndex + startMarker.length
        val jsonEnd = html.indexOf("</script>", jsonStart)
        if (jsonEnd == -1) return null
        
        val jsonString = html.substring(jsonStart, jsonEnd)
        return try {
            Json.decodeFromString<JsonObject>(jsonString)
        } catch (e: Exception) {
            null
        }
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
