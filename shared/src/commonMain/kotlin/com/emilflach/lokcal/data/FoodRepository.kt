package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.FoodAlias
import com.emilflach.lokcal.util.levenshtein
import com.emilflach.lokcal.util.normalize

class FoodRepository(database: Database) {
    private val queries = database.foodQueries

    suspend fun getAll(): List<Food> = queries.selectAll().awaitAsList()

    suspend fun getById(id: Long): Food? = queries.selectById(id).awaitAsOneOrNull()

    suspend fun updateDetails(
        id: Long,
        name: String,
        energyKcalPer100g: Double,
        productUrl: String?,
        imageUrl: String?,
        gtin13: String?,
        servingSize: String?,
        source: String?,
    ) {
        queries.updateDetails(
            name = name,
            energy_kcal_per_100g = energyKcalPer100g,
            serving_size = servingSize,
            gtin13 = gtin13,
            image_url = imageUrl,
            product_url = productUrl,
            source = source,
            id = id
        )
    }

    suspend fun insertManual(
        name: String,
        energyKcalPer100g: Double,
        servingSize: String?,
        gtin13: String?,
        imageUrl: String?,
        productUrl: String?,
        source: String?,
    ): Long {
        return queries.transactionWithResult {
            queries.insertManual(
                name = name,
                energy_kcal_per_100g = energyKcalPer100g,
                serving_size = servingSize,
                gtin13 = gtin13,
                image_url = imageUrl,
                product_url = productUrl,
                source = source
            )
            queries.selectLastInsertRowId().awaitAsOne()
        }
    }

    suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    // Alias management methods
    suspend fun addAlias(foodId: Long, alias: String, type: String) {
        queries.insertAlias(foodId, alias, type)
    }

    suspend fun getAliases(foodId: Long): List<FoodAlias> {
        return queries.selectAliasesByFoodId(foodId).awaitAsList()
    }

    suspend fun deleteAlias(aliasId: Long) {
        queries.deleteAlias(aliasId)
    }

    /**
     * Returns foods ranked by relevance for the given query. SQL handles scoring and ordering;
     * Kotlin only filters for multi-word token presence and handles the Levenshtein fallback.
     */
    suspend fun search(query: String): List<Food> = searchWithCounts(query).map { it.first }

    /** Like [search] but also returns the track count for each result, avoiding a separate Intake scan. */
    suspend fun searchWithCounts(query: String): List<Pair<Food, Int>> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()
        val qNorm = normalize(qLower)

        // GTIN-13 barcode: try exact match first
        val digitsOnly = q.filter { it.isDigit() }
        if (digitsOnly.length > 5) {
            val byBarcode = queries.selectByGtin13(digitsOnly).awaitAsList()
            if (byBarcode.isNotEmpty()) return byBarcode.map { Pair(it, 0) }
        }

        // Normalize query for matching; drop punctuation-only tokens (e.g. lone "&")
        val tokens = qNorm.split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.any { c -> c.isLetterOrDigit() } }
        val primary = tokens.maxByOrNull { it.length } ?: qNorm
        val candidates = queries.searchRanked("%$primary%", qNorm).awaitAsList()

        if (candidates.isNotEmpty()) {
            val filtered = if (tokens.size >= 2)
                candidates.filter { row -> tokens.all { t -> normalize(row.name.lowercase()).contains(t) } }
            else
                candidates
            return filtered.map { row ->
                Pair(
                    Food(row.id, row.name, row.energy_kcal_per_100g, row.unit, row.serving_size,
                         row.gtin13, row.image_url, row.product_url, row.source, row.created_at),
                    row.track_count.toInt()
                )
            }
        }

        // Normalized full-scan: handles accented DB names that SQLite LIKE can't match
        val all = queries.selectAll().awaitAsList()
        val normalizedMatches = if (tokens.size >= 2) {
            all.filter { f -> tokens.all { t -> normalize(f.name.lowercase()).contains(t) } }
        } else {
            all.filter { f -> normalize(f.name.lowercase()).contains(qNorm) }
        }
        if (normalizedMatches.isNotEmpty()) return normalizedMatches.map { Pair(it, 0) }

        // Levenshtein fallback: edit distance on normalized strings
        return all.filter { levenshtein(normalize(it.name.lowercase()), qNorm) <= 2 }.map { Pair(it, 0) }
    }
}
