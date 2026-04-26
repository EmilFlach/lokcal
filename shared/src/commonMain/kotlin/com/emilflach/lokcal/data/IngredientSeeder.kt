package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lokcal.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object IngredientSeeder {
    private const val META_KEY = "ingredients_seeded"

    @OptIn(ExperimentalResourceApi::class)
    suspend fun seedIfNeeded(database: Database, onProgress: ((Float) -> Unit)? = null) = withContext(Dispatchers.Default) {
        val metaQ = database.metaQueries
        if (metaQ.getMeta(META_KEY).awaitAsOneOrNull() != null) return@withContext

        onProgress?.invoke(0.05f)

        val foodCsv = try {
            Res.readBytes("files/seed_food.csv").decodeToString()
        } catch (_: Exception) {
            return@withContext
        }

        val aliasCsv = try {
            Res.readBytes("files/seed_food_alias.csv").decodeToString()
        } catch (_: Exception) {
            ""
        }

        onProgress?.invoke(0.10f)

        // Build map: csv food_id -> list of (alias, alias_type)
        val aliasMap = mutableMapOf<Long, MutableList<Pair<String, String>>>()
        aliasCsv.lines().drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val cols = parseCsvLine(line)
            if (cols.size < 4) return@forEach
            val foodId = cols[1].toLongOrNull() ?: return@forEach
            val alias = cols[2].trim()
            val aliasType = cols[3].trim()
            if (alias.isNotEmpty()) {
                aliasMap.getOrPut(foodId) { mutableListOf() }.add(alias to aliasType)
            }
        }

        val foodRepository = FoodRepository(database)
        val existingNames = foodRepository.getAll().map { it.name.lowercase() }.toSet()

        val foodLines = foodCsv.lines()
        val total = (foodLines.size - 1).coerceAtLeast(1)

        foodLines.drop(1).forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed
            if (index % 50 == 0) {
                onProgress?.invoke(0.10f + (index.toFloat() / total) * 0.85f)
            }

            val cols = parseCsvLine(line)
            if (cols.size < 2) return@forEachIndexed

            val csvId = cols[0].toLongOrNull() ?: return@forEachIndexed
            val name = cols[1].trim().takeIf { it.isNotEmpty() } ?: return@forEachIndexed
            if (existingNames.contains(name.lowercase())) return@forEachIndexed

            val kcal = cols.getOrNull(2)?.toDoubleOrNull() ?: 0.0
            // cols[3] is unit — skipped, DB defaults to 'g'
            val servingSize = cols.getOrNull(4)?.trim()?.ifEmpty { null }
            val gtin13 = cols.getOrNull(5)?.trim()?.ifEmpty { null }
            val imageUrl = cols.getOrNull(6)?.trim()?.ifEmpty { null }
            val productUrl = cols.getOrNull(7)?.trim()?.ifEmpty { null }
            val source = cols.getOrNull(8)?.trim()?.ifEmpty { null } ?: "seed"

            val dbId = foodRepository.insertManual(
                name = name,
                energyKcalPer100g = kcal,
                servingSize = servingSize,
                gtin13 = gtin13,
                imageUrl = imageUrl,
                productUrl = productUrl,
                source = source
            )

            aliasMap[csvId]?.forEach { (alias, type) ->
                foodRepository.addAlias(dbId, alias, type)
            }
        }

        metaQ.setMeta(META_KEY, "1")
        onProgress?.invoke(1.0f)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val current = StringBuilder()
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }
}
