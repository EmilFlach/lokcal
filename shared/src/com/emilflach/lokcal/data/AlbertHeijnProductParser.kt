package com.emilflach.lokcal.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.floor

/**
 * Turns one Albert Heijn `product` object — as returned by [AlbertHeijnApi.fetchProducts] — into the
 * fields this app cares about.
 *
 * Pure and synchronous: everything here is JSON tree navigation, so it is directly unit-testable
 * without a network stub. Every step casts with `as?` rather than the `json*` accessors, because
 * Albert Heijn returns explicit nulls for whole branches — `tradeItem` is null on non-food products
 * and `nutritions` is null on plenty of food ones.
 */
class AlbertHeijnProductParser {

    data class FoodFetchResult(
        val name: String?,
        val kcalPer100g: Double?,
        val servingSizeGrams: Double?,
        val imageUrl: String?,
        val productUrl: String,
        val gtin13: String?,
    )

    fun parse(product: JsonObject): FoodFetchResult = FoodFetchResult(
        name = extractName(product),
        kcalPer100g = extractKcal(product),
        servingSizeGrams = extractServingSize(product),
        imageUrl = extractImageUrl(product),
        productUrl = extractProductUrl(product),
        gtin13 = extractGtin(product),
    )

    private fun extractName(product: JsonObject): String? =
        product["title"]?.jsonPrimitive?.contentOrNull?.let { cleanName(unescapeHtml(it)) }

    private fun cleanName(name: String): String =
        name.removeSuffix(" bestellen | Albert Heijn").trim()

    /** Albert Heijn reports a GTIN-14; dropping the leading zero yields the GTIN-13 the app stores. */
    private fun extractGtin(product: JsonObject): String? =
        tradeItem(product)?.get("gtin")?.jsonPrimitive?.contentOrNull?.removePrefix("0")

    private fun extractImageUrl(product: JsonObject): String? =
        (product["imagePack"] as? JsonArray)?.firstOrNull()
            ?.let { it as? JsonObject }
            ?.let { it["small"] as? JsonObject }
            ?.get("url")?.jsonPrimitive?.contentOrNull

    /** Canonical storefront URL, from the product's own `webPath`. */
    private fun extractProductUrl(product: JsonObject): String {
        val webPath = product["webPath"]?.jsonPrimitive?.contentOrNull
        if (webPath != null) return AlbertHeijnApi.WEB_BASE + webPath
        val id = product["id"]?.jsonPrimitive?.intOrNull
        return "${AlbertHeijnApi.WEB_BASE}/producten/product/wi$id"
    }

    /**
     * Energy per 100 g/ml. Albert Heijn reports it as one string per nutrient, e.g.
     * `"410.0 kJ (99.0 kcal)"`, under the nutrition entry whose basis is 100.
     * Returns 0.0 when the product carries no nutrition at all.
     */
    private fun extractKcal(product: JsonObject): Double {
        val perHundred = nutritions(product)?.find { basisQuantity(it).startsWith("100.0", ignoreCase = true) }
        val energy = (perHundred as? JsonObject)?.let { it["nutrients"] as? JsonArray }
            ?.find { (it as? JsonObject)?.get("type")?.jsonPrimitive?.contentOrNull == "ENER-" }
        val value = (energy as? JsonObject)?.get("value")?.jsonPrimitive?.contentOrNull ?: return 0.0
        val kcal = regexGroup(value, "([0-9.]+)\\s*kcal", ignoreCase = true)?.toDoubleOrNull() ?: return 0.0
        return floor(kcal)
    }

    /**
     * Serving size, taken from the first nutrition entry whose basis is not 100 — Albert Heijn lists
     * the per-serving column alongside the per-100 one. Falls back to 100.0.
     */
    private fun extractServingSize(product: JsonObject): Double {
        val serving = nutritions(product)?.find { !basisQuantity(it).startsWith("100.0", ignoreCase = true) }
            ?: return 100.0
        return floor(regexGroup(basisQuantity(serving), "([0-9.]+)")?.toDoubleOrNull() ?: 100.0)
    }

    private fun tradeItem(product: JsonObject): JsonObject? = product["tradeItem"] as? JsonObject

    private fun nutritions(product: JsonObject): JsonArray? = tradeItem(product)?.get("nutritions") as? JsonArray

    private fun basisQuantity(nutrition: JsonElement): String =
        (nutrition as? JsonObject)?.get("basisQuantity")?.jsonPrimitive?.contentOrNull ?: ""

    private fun regexGroup(text: String, pattern: String, ignoreCase: Boolean = false): String? {
        val re = Regex(pattern, if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet())
        return re.find(text)?.groupValues?.getOrNull(1)
    }

    private fun unescapeHtml(s: String): String =
        s.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
}
