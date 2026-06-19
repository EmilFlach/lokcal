package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

/**
 * Searches the Kroger Products API (https://developer.kroger.com/documentation/api-products/public/products).
 *
 * Nutrients in the Kroger response are per serving. To convert to per-100g we divide by serving
 * size (g or ml) and multiply by 100. If either value is unavailable, energyKcalPer100g is null.
 */
open class KrogerSearch(
    private val auth: KrogerAuth = KrogerAuth(),
    private val client: HttpClient = defaultClient,
) {
    companion object {
        private const val PRODUCTS_URL = "https://api.kroger.com/v1/products"
        private const val KROGER_BASE_URL = "https://www.kroger.com"
        private const val SEARCH_LIMIT = 10
        private const val OZ_TO_GRAMS = 28.3495

        // Matches the 13-digit productId at the end of a Kroger product URL path
        private val productIdFromUrlRegex = Regex("""/(\d{13})(?:\?.*)?$""")

        private val defaultClient by lazy {
            HttpClient {
                install(Logging) { level = LogLevel.ALL }
            }
        }
    }

    open suspend fun search(query: String): List<OnlineFoodItem> {
        if (query.isBlank()) return emptyList()
        val searchJson = fetchSearchJson(query)
        log("search response (first 500): ${searchJson.take(500)}")
        val basicProducts = extractBasicProducts(searchJson)
        log("data array size: ${basicProducts.size}")
        if (basicProducts.isEmpty()) return emptyList()

        return coroutineScope {
            basicProducts.map { (productId, fallback) ->
                async {
                    val detail = runCatching { fetchById(productId) }.getOrNull()
                    if (detail != null) {
                        log("  detail for '$productId': kcal=${detail.energyKcalPer100g}")
                        detail
                    } else {
                        log("  detail fetch failed for '$productId', using fallback")
                        fallback
                    }
                }
            }.awaitAll().filter { it.energyKcalPer100g != null }
        }
    }

    open suspend fun fetchById(productId: String): OnlineFoodItem? {
        val json = fetchProductJson(productId) ?: return null
        return parseSingleProductResponse(json)
    }

    fun productIdFromUrl(url: String): String? =
        productIdFromUrlRegex.find(url)?.groupValues?.getOrNull(1)

    protected open suspend fun fetchSearchJson(query: String): String {
        val token = auth.getAccessToken()
        val isBarcode = query.length == 13 && query.all { it.isDigit() }
        return client.get(PRODUCTS_URL) {
            header(HttpHeaders.Authorization, "Bearer $token")
            header(HttpHeaders.Accept, "application/json")
            if (isBarcode) {
                parameter("filter.productId", query)
            } else {
                parameter("filter.term", query)
                parameter("filter.limit", SEARCH_LIMIT)
            }
        }.body()
    }

    protected open suspend fun fetchProductJson(productId: String): String? {
        return try {
            val token = auth.getAccessToken()
            client.get("$PRODUCTS_URL/$productId") {
                header(HttpHeaders.Authorization, "Bearer $token")
                header(HttpHeaders.Accept, "application/json")
            }.body()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractBasicProducts(json: String): List<Pair<String, OnlineFoodItem>> {
        return try {
            val root = Json.parseToJsonElement(json).jsonObject
            val items = root["data"]?.jsonArray?.filterIsInstance<JsonObject>() ?: run {
                log("data is null or not an array, top-level keys: ${root.keys}")
                return emptyList()
            }
            items.mapNotNull { product ->
                val productId = product["productId"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val item = mapProduct(product) ?: return@mapNotNull null
                productId to item
            }
        } catch (e: Exception) {
            log("extractBasicProducts exception: $e")
            emptyList()
        }
    }

    private fun parseSingleProductResponse(json: String): OnlineFoodItem? {
        return try {
            log("detail JSON: $json")
            val root = Json.parseToJsonElement(json).jsonObject
            val data = root["data"]
            log("detail data type: ${data?.let { it::class.simpleName }}, keys: ${(data as? JsonObject)?.keys}")
            (data as? JsonObject)?.let { mapProduct(it) }
        } catch (e: Exception) {
            log("parseSingleProductResponse exception: $e")
            null
        }
    }

    private fun mapProduct(product: JsonObject): OnlineFoodItem? {
        val name = product["description"]?.jsonPrimitive?.contentOrNull
        if (name == null) {
            log("mapProduct: skipping product with no description, keys: ${product.keys}")
            return null
        }
        log("mapProduct: '$name'")

        val gtin13 = product["upc"]?.jsonPrimitive?.contentOrNull
            ?: product["productId"]?.jsonPrimitive?.contentOrNull

        val productPageUri = product["productPageURI"]?.jsonPrimitive?.contentOrNull
        val productUrl = productPageUri?.let { "$KROGER_BASE_URL${it.substringBefore("?")}" }

        val imageUrl = extractImageUrl(product)

        val nutritionRaw = product["nutritionInformation"]
        log("  nutritionInformation element type: ${nutritionRaw?.let { it::class.simpleName }}")
        val nutrition: JsonObject? = when (nutritionRaw) {
            is JsonObject -> nutritionRaw
            is JsonArray -> {
                log("  nutritionInformation is array[${nutritionRaw.size}], first: ${nutritionRaw.firstOrNull()?.toString()?.take(300)}")
                nutritionRaw.filterIsInstance<JsonObject>().firstOrNull()
            }
            else -> null
        }
        log("  nutrition present: ${nutrition != null}")
        val servingSizeG = nutrition?.let { extractServingSizeGrams(it) }
        val kcalPerServing = nutrition?.let { extractKcalPerServing(it) }
        log("  servingSizeG=$servingSizeG  kcalPerServing=$kcalPerServing")
        val kcalPer100g = if (kcalPerServing != null && servingSizeG != null && servingSizeG > 0) {
            (kcalPerServing / servingSizeG) * 100.0
        } else {
            null
        }

        return OnlineFoodItem(
            name = name,
            gtin13 = gtin13,
            energyKcalPer100g = kcalPer100g,
            servingSize = servingSizeG,
            productUrl = productUrl,
            imageUrl = imageUrl,
            dutchName = null,
        )
    }

    private fun extractImageUrl(product: JsonObject): String? {
        val images = product["images"]?.jsonArray?.filterIsInstance<JsonObject>() ?: return null
        val frontDefault = images.firstOrNull { img ->
            img["perspective"]?.jsonPrimitive?.contentOrNull == "front" &&
                img["default"]?.jsonPrimitive?.booleanOrNull == true
        } ?: images.firstOrNull()
        val sizes = frontDefault?.get("sizes")?.jsonArray?.filterIsInstance<JsonObject>() ?: return null
        val medium = sizes.firstOrNull { it["size"]?.jsonPrimitive?.contentOrNull == "medium" }
            ?: sizes.firstOrNull()
        return medium?.get("url")?.jsonPrimitive?.contentOrNull
    }

    private fun extractKcalPerServing(nutrition: JsonObject): Double? {
        val nutrients = nutrition["nutrients"]?.jsonArray?.filterIsInstance<JsonObject>() ?: run {
            log("  no nutrients array")
            return null
        }
        log("  nutrients count: ${nutrients.size}")
        nutrients.forEach { n ->
            val code = n["code"]?.jsonPrimitive?.contentOrNull
            val desc = n["description"]?.jsonPrimitive?.contentOrNull
            val unit = (n["unitOfMeasure"] as? JsonObject)?.get("abbreviation")?.jsonPrimitive?.contentOrNull
            val qty = n["quantity"]?.jsonPrimitive?.doubleOrNull
            log("    nutrient code=$code desc=$desc unit=$unit qty=$qty")
        }
        return nutrients.firstOrNull { nutrient ->
            val code = nutrient["code"]?.jsonPrimitive?.contentOrNull
            val unitObj = nutrient["unitOfMeasure"] as? JsonObject
            val unitAbbr = unitObj?.get("abbreviation")?.jsonPrimitive?.contentOrNull
            val unitName = unitObj?.get("name")?.jsonPrimitive?.contentOrNull
            val desc = nutrient["description"]?.jsonPrimitive?.contentOrNull
            // "ENER-" is the INFOODS code for total energy in kcal (most reliable)
            code == "ENER-"
                || unitAbbr?.lowercase() == "kcal"
                || unitName?.lowercase()?.contains("kilocalorie") == true
                || (desc?.lowercase()?.contains("calorie") == true && desc.lowercase().contains("fat") == false)
        }?.get("quantity")?.jsonPrimitive?.doubleOrNull
    }

    private fun extractServingSizeGrams(nutrition: JsonObject): Double? {
        val serving = nutrition["servingSize"] as? JsonObject ?: run {
            log("  no servingSize object")
            return null
        }
        val quantity = serving["quantity"]?.jsonPrimitive?.doubleOrNull ?: run {
            log("  servingSize has no quantity, keys: ${serving.keys}")
            return null
        }
        val unit = (serving["unitOfMeasure"] as? JsonObject)
            ?.get("abbreviation")?.jsonPrimitive?.contentOrNull?.lowercase()
        log("  servingSize: quantity=$quantity unit=$unit")
        return when (unit) {
            "g", "ml" -> quantity
            "oz" -> quantity * OZ_TO_GRAMS
            else -> {
                // Count-based units (pk, piece, H87, etc.) — estimate serving weight
                // from macro nutrients: fat + protein + carbs + fiber ≈ grams per serving
                val estimated = estimateServingGramsFromMacros(nutrition)
                if (estimated != null) {
                    log("  estimated serving from macros: ${estimated}g (unit='$unit')")
                } else {
                    log("  unrecognised serving unit '$unit', no macro fallback")
                }
                estimated
            }
        }
    }

    private fun estimateServingGramsFromMacros(nutrition: JsonObject): Double? {
        val nutrients = nutrition["nutrients"]?.jsonArray?.filterIsInstance<JsonObject>()
            ?: return null
        // Kroger INFOODS codes: FAT=fat, PRO-=protein, CHO-=carbs, FIBTSW/FIBTG=fiber
        val macroCodes = setOf("FAT", "PRO-", "CHO-", "FIBTSW", "FIBTG")
        var total = 0.0
        var found = 0
        for (nutrient in nutrients) {
            val code = nutrient["code"]?.jsonPrimitive?.contentOrNull ?: continue
            if (code !in macroCodes) continue
            val unitObj = nutrient["unitOfMeasure"] as? JsonObject
            val abbr = unitObj?.get("abbreviation")?.jsonPrimitive?.contentOrNull?.lowercase()
            val name = unitObj?.get("name")?.jsonPrimitive?.contentOrNull?.lowercase()
            val isGrams = abbr == "g" || name?.contains("gram") == true
            if (!isGrams) continue
            val qty = nutrient["quantity"]?.jsonPrimitive?.doubleOrNull ?: continue
            total += qty
            found++
        }
        return if (found > 0 && total > 0) total else null
    }

    private fun log(msg: String) = println("KrogerSearch: $msg")
}
