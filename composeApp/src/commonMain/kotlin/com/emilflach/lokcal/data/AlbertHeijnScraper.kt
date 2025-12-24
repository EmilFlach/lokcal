package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Scrapes an AH product page and extracts key fields using regex.
 * This keeps dependencies light and avoids coupling to AH's internal JSON structures.
 */
class AlbertHeijnScraper(
    private val client: HttpClient = defaultClient
) {
    companion object {
        private val defaultClient by lazy {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
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

    suspend fun fetchHtml(url: String): String {
        val resp = client.get(url)
        if (!resp.status.isSuccess()) error("Failed to fetch AH page: ${'$'}{resp.status}")
        return resp.body()
    }

    suspend fun scrape(url: String): FoodScrapeResult {
        val html = fetchHtml(url)
        val text = html

        val name = extractName(text)
        val kcal = extractKcal(text)
        val servingGrams = extractServingGrams(text)
        val image = extractImageUrl(text)
        val gtin = extractGtin(text)

        return FoodScrapeResult(
            name = name,
            kcalPer100g = kcal,
            servingSizeGrams = servingGrams,
            imageUrl = image,
            productUrl = url,
            gtin13 = gtin,
        )
    }

    private fun extractName(text: String): String? {
        // Try HTML <h1> first
        regexFind(text, "<h1[^>]*>(.*?)</h1>")?.let { return unescapeHtml(stripTags(it)) }
        // Try JSON-like fields
        regexGroup(text, "\\\"title\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")?.let { return unescapeHtml(it) }
        regexGroup(text, "\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")?.let { return unescapeHtml(it) }
        return null
    }

    private fun extractKcal(text: String): Double? {
        // Prefer explicit kcal tokens like "76 kcal" or "49.0 kcal" (supports comma or dot decimals)
        regexGroup(text, "([0-9]+(?:[.,][0-9]+)?)\\s*kcal", ignoreCase = true)
            ?.let { return it.replace(',', '.').toDoubleOrNull() }
        // Some AH JSON may hold perPortionKcal
        regexGroup(text, "\\\"kcal\\\"\\s*:\\s*([0-9.,]+)")
            ?.let { return it.replace(',', '.').toDoubleOrNull() }
        return null
    }

    private fun extractServingGrams(text: String): Double? {
        // Patterns like 125 g, 125 gram, 125-gram
//        regexGroup(text, "(\\d{2,4})\\s*-?\\s*(?:g|gram)", ignoreCase = true)?.let { return it.toDoubleOrNull() }
        // Try JSON fields like "servingSize":"125 g"
        regexGroup(text, "\\\"servingSizeDescription\\\"\\s*:\\s*\\\"(\\d{2,4})\\s*gram\\\"", ignoreCase = true)?.let { return it.toDoubleOrNull() }
        return 100.0
    }

    private fun extractImageUrl(text: String): String? {
        // Try to extract from Schema.org Product JSON structure
        regexGroup(text, "\"image\"\\s*:\\s*\"(https://static\\.ah\\.nl[^\"]+)\"")?.let { return it }
        // Escaped URL in JSON e.g., https:\u002F\u002Fstatic.ah.nl\u002Fdam\u002Fproduct\u002F...
        val raw = regexGroup(text, "https:(?:\\\\u002F){2}static\\.ah\\.nl[^\\\"]+")
        if (raw != null) return unescapeUrl(raw)
        // Non-escaped fallback
        regexGroup(text, "https://static\\.ah\\.nl[^\\\"]+")?.let { return it }
        return null
    }

    private fun unescapeUrl(url: String): String =
        url.replace("\\u002F", "/")

    private fun extractGtin(text: String): String? {
        // Try to extract from Schema.org Product JSON structure
        regexGroup(text, "\"gtin13\"\\s*:\\s*\"(\\d{13})\"")?.let { return it }
        // Fallback: explicit gtin field in other JSON structures
        regexGroup(text, "\\\"gtin\\\"\\s*:\\s*\\\"(\\d{13})\\\"")?.let { return it }
        // Last resort: any 13-digit sequence (GTIN-13)
        regexGroup(text, "(?<!\\d)(\\d{13})(?!\\d)")?.let { return it }
        return null
    }

    private fun stripTags(s: String): String =
        s.replace(Regex("<[^>]+>"), "").trim()

    private fun regexFind(text: String, pattern: String, ignoreCase: Boolean = true): String? {
        val re = if (ignoreCase) Regex(pattern, RegexOption.IGNORE_CASE) else Regex(pattern)
        val m = re.find(text) ?: return null
        return m.groupValues.getOrNull(1)
    }

    private fun regexGroup(text: String, pattern: String, ignoreCase: Boolean = false): String? {
        val re = Regex(pattern, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
        val m = re.find(text) ?: return null
        return m.groupValues.getOrNull(1)
    }

    private fun unescapeHtml(s: String): String {
        var out = s
        // Common named entities
        out = out.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")

        // Numeric decimal entities: &#38;
        out = Regex("&#(\\d+);", RegexOption.IGNORE_CASE).replace(out) { m ->
            val code = m.groupValues[1].toIntOrNull() ?: return@replace m.value
            codePointToStringOrNull(code) ?: m.value
        }

        // Numeric hex entities: &#x26;
        out = Regex("&#x([0-9a-fA-F]+);", RegexOption.IGNORE_CASE).replace(out) { m ->
            val code = m.groupValues[1].toIntOrNull(16) ?: return@replace m.value
            codePointToStringOrNull(code) ?: m.value
        }
        return out
    }

    private fun codePointToStringOrNull(codePoint: Int): String? {
        if (codePoint < 0 || codePoint > 0x10FFFF) return null
        return if (codePoint <= 0xFFFF) {
            codePoint.toChar().toString()
        } else {
            // Create surrogate pair
            val cpPrime = codePoint - 0x10000
            val high = 0xD800 + (cpPrime shr 10)
            val low = 0xDC00 + (cpPrime and 0x3FF)
            charArrayOf(high.toChar(), low.toChar()).concatToString()
        }
    }
}
