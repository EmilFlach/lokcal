package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Seeds the Food table from bundled ingredients.json on first app launch.
 * Uses Meta table key "ingredients_seeded" to prevent reseeding.
 */
object IngredientSeeder {
    private const val META_KEY = "ingredients_seeded"

    fun seedIfNeeded(database: Database) {
        val metaQ = database.metaQueries
        val already = metaQ.getMeta(META_KEY).executeAsOneOrNull()
        if (already != null) return

        val jsonText = loadIngredientsJsonText() ?: return

        val json = try {
            Json.parseToJsonElement(jsonText)
        } catch (_: Throwable) {
            return
        }
        val arr = (json as? JsonArray) ?: return

        val foodQ = database.foodQueries
        foodQ.transaction {
            for (el in arr) {
                val o = (el as? JsonObject) ?: continue
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
                val externalId = o["id"]?.jsonPrimitive?.content
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
                val foodId = externalId?.let { eid -> foodQ.selectIdByExternalId(eid).executeAsOneOrNull() }
                if (foodId != null) {
                    val aliases = (o["aliases"] as? JsonArray)
                    if (aliases != null) {
                        for (al in aliases) {
                            val alias = (al as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim()
                            if (!alias.isNullOrEmpty()) {
                                database.foodQueries.foodAliasInsert(food_id = foodId, alias = alias)
                            }
                        }
                    }
                }
            }
        }
        metaQ.setMeta(META_KEY, "1")
    }
}

/**
 * Platform-specific loader for ingredients.json. Should return the JSON text when available,
 * or null if not found on the current platform/build.
 */
expect fun loadIngredientsJsonText(): String?
