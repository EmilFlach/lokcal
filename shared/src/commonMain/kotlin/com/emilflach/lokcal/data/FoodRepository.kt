package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.FoodAlias
import com.emilflach.lokcal.util.SearchUtils
import com.emilflach.lokcal.util.levenshtein

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

    suspend fun getAliasesByType(foodId: Long, type: String): List<FoodAlias> {
        return queries.selectAliasesByType(foodId, type).awaitAsList()
    }

    suspend fun deleteAlias(aliasId: Long) {
        queries.deleteAlias(aliasId)
    }

    suspend fun updateAlias(aliasId: Long, alias: String, type: String) {
        queries.updateAlias(alias, type, aliasId)
    }

    /**
     * Search foods with order-agnostic, multi-field logic.
     * Now searches across food names and aliases stored in FoodAlias table.
     * The SQL query with LEFT JOIN handles the alias matching, so we just use the name field for sorting.
     */
    suspend fun search(query: String, trackingCounts: Map<Long, Long> = emptyMap()): List<Food> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()

        // If the query looks like a GTIN-13 (EAN-13) barcode, try exact match on gtin13 first
        val digitsOnly = q.filter { it.isDigit() }
        if (digitsOnly.length == 13) {
            val byBarcode = queries.selectByGtin13(digitsOnly).awaitAsList()
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        fun sourcePriority(src: String?): Int = when (src?.lowercase()) {
            "manual" -> 0
            "nevo" -> 1
            "mealie" -> 2
            "ah" -> 3
            else -> 4
        }

        fun fields(f: Food): List<String> = listOf(f.name)
        fun exactMatch(f: Food): Boolean = SearchUtils.exactMatch(fields(f), q)
        fun prefixMatch(f: Food): Boolean = SearchUtils.prefixMatch(fields(f), q)
        fun containsPos(f: Food): Int = SearchUtils.containsPos(fields(f), qLower)
        fun trackingCount(f: Food): Long = trackingCounts[f.id] ?: 0L
        fun levScore(f: Food): Int {
            var best = Int.MAX_VALUE
            for (s in fields(f)) {
                val d = levenshtein(s.lowercase(), qLower)
                if (d < best) best = d
            }
            return best
        }

        // Multi-word, order-agnostic search
        val tokens = SearchUtils.tokenize(qLower)
        if (tokens.size >= 2) {
            val primary = SearchUtils.longestToken(tokens) ?: tokens.first()
            val likePrimary = "%$primary%"
            val initial = queries.searchByAny(likePrimary, likePrimary, q).awaitAsList()
            fun tokensPosSum(f: Food): Int = SearchUtils.tokensPosSum(fields(f), tokens)
            val filtered = initial.filter { f ->
                SearchUtils.tokensPresent(fields(f), tokens)
            }
            if (filtered.isNotEmpty()) {
                return filtered.sortedWith(
                    compareByDescending<Food> { trackingCount(it) > 0 }
                        .thenBy { if (exactMatch(it)) 0 else 1 }
                        .thenBy { if (prefixMatch(it)) 0 else 1 }
                        .thenBy { tokensPosSum(it) }
                        .thenByDescending { trackingCount(it) }
                        .thenBy { levScore(it) }
                        .thenBy { sourcePriority(it.source) }
                        .thenBy { it.name.lowercase() }
                )
            }
        }

        val like = "%$qLower%"
        val candidatesLike = queries.searchByAny(like, like, q).awaitAsList()

        if (candidatesLike.isNotEmpty()) {
            return candidatesLike.sortedWith(
                compareByDescending<Food> { trackingCount(it) > 0 }
                    .thenBy { if (exactMatch(it)) 0 else 1 }
                    .thenBy { if (prefixMatch(it)) 0 else 1 }
                    .thenBy { containsPos(it) }
                    .thenByDescending { trackingCount(it) }
                    .thenBy { levScore(it) }
                    .thenBy { sourcePriority(it.source) }
                    .thenBy { it.name.lowercase() }
            )
        }

        // Fuzzy fallback
        val all = getAll()
        if (all.isEmpty()) return emptyList()
        return all.sortedWith(
            compareByDescending<Food> { trackingCount(it) > 0 }
                .thenBy { levScore(it) }
                .thenByDescending { trackingCount(it) }
                .thenBy { sourcePriority(it.source) }
                .thenBy { it.name.lowercase() }
        ).take(100)
    }
}
