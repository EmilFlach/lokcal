package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import lokcal.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Seeds the Food table from bundled ingredients.json on first app launch.
 * Uses Meta table key "ingredients_seeded" to prevent reseeding.
 */
object IngredientSeeder {
    private const val META_KEY = "ingredients_seeded"

    // Optional platform override to supply JSON text (e.g., Android assets)
    var provideJsonText: (() -> String?)? = null

    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedIfNeeded(database: Database, onProgress: ((Float) -> Unit)? = null) = withContext(Dispatchers.Default) {
        val metaQ = database.metaQueries
        val already = metaQ.getMeta(META_KEY).awaitAsOneOrNull()
        if (already != null) return@withContext

        var jsonText = provideJsonText?.invoke()

        if (jsonText == null) {
            try {
                onProgress?.invoke(0.1f)
                jsonText = Res.readBytes("files/ingredients.json").decodeToString()
            } catch (_: Exception) {
                // Fallback or log
            }
        }

        if (jsonText == null) return@withContext

        val json = try {
            onProgress?.invoke(0.2f)
            Json.parseToJsonElement(jsonText)
        } catch (_: Throwable) {
            return@withContext
        }
        val arr = (json as? JsonArray) ?: return@withContext

        val foodQ = database.foodQueries
        val existingExternalIds = foodQ.selectAllExternalIds().awaitAsList().toSet()

        val total = arr.size

        for ((index, el) in arr.withIndex()) {
            if (index % 50 == 0) {
                onProgress?.invoke(0.2f + (index.toFloat() / total) * 0.8f)
            }
            val o = (el as? JsonObject) ?: continue
            val externalId = o["id"]?.jsonPrimitive?.content
            
            // Skip if already exists
            if (externalId != null && existingExternalIds.contains(externalId)) {
                continue
            }

            val name = o["name"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val desc = o["description"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() }

            val extras = o["extras"] as? JsonObject
            val kcalStr = extras?.get("kcal")?.jsonPrimitive?.content
            val kcal = kcalStr?.toDoubleOrNull()

            val englishName = extras?.get("englishName")?.jsonPrimitive?.content
            val dutchName = extras?.get("dutchName")?.jsonPrimitive?.content
            val brandName = extras?.get("brandName")?.jsonPrimitive?.content
            val servingSize = extras?.get("servingSize")?.jsonPrimitive?.content
            val gtin13 = extras?.get("gtin13")?.jsonPrimitive?.content
            val imageUrl = extras?.get("imageUrl")?.jsonPrimitive?.content
            val productUrl = extras?.get("productUrl")?.jsonPrimitive?.content
            val source = extras?.get("source")?.jsonPrimitive?.content

            val rawJson = o.toString()
            val pluralName = o["pluralName"]?.jsonPrimitive?.content
            val labelId = o["labelId"]?.jsonPrimitive?.content
            val createdAtSrc = o["createdAt"]?.jsonPrimitive?.content
            val updatedAtSrc = o["updatedAt"]?.jsonPrimitive?.content
            val onHand = o["onHand"]?.jsonPrimitive?.content?.lowercase() == "true"

            // Insert structured fields
            val energy = kcal ?: 0.0

            foodQ.insertFromSeed(
                name = name,
                description = desc,
                energy_kcal_per_100g = energy,
                raw_json = rawJson,
                external_id = externalId,
                plural_name = pluralName,
                label_id = labelId,
                created_at_source = createdAtSrc,
                updated_at_source = updatedAtSrc,
                on_hand = if (onHand) 1 else 0,
                english_name = englishName,
                dutch_name = dutchName,
                brand_name = brandName,
                serving_size = servingSize,
                gtin13 = gtin13,
                image_url = imageUrl,
                product_url = productUrl,
                source = source
            )

            // Insert aliases
            val currentFoodId = if (externalId != null) {
                foodQ.selectIdByExternalId(externalId).awaitAsOneOrNull()
            } else null

            if (currentFoodId != null) {
                val aliases = (o["aliases"] as? JsonArray)
                if (aliases != null) {
                    for (al in aliases) {
                        val alias = (al as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim()
                        if (!alias.isNullOrEmpty()) {
                            foodQ.foodAliasInsert(food_id = currentFoodId, alias = alias)
                        }
                    }
                }
            }
        }
        metaQ.setMeta(META_KEY, "1")
    }
}
