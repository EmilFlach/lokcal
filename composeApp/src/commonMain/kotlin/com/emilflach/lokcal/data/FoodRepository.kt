package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.util.SearchUtils
import com.emilflach.lokcal.util.levenshtein

class FoodRepository(database: Database) {
    private val queries = database.foodQueries

    fun getAll(): List<Food> = queries.selectAll().executeAsList()

    fun getById(id: Long): Food? = queries.selectById(id).executeAsOneOrNull()

    fun updateDetails(
        id: Long,
        name: String,
        brandName: String?,
        energyKcalPer100g: Double,
        productUrl: String?,
        imageUrl: String?,
        gtin13: String?,
        servingSize: String?,
        englishName: String?,
        dutchName: String?,
        source: String?,
    ) {
        queries.updateDetails(
            name = name,
            brand_name = brandName,
            energy_kcal_per_100g = energyKcalPer100g,
            product_url = productUrl,
            image_url = imageUrl,
            gtin13 = gtin13,
            serving_size = servingSize,
            english_name = englishName,
            dutch_name = dutchName,
            source = source,
            id = id
        )
    }

    fun insertManual(
        name: String,
        brandName: String?,
        energyKcalPer100g: Double,
        productUrl: String?,
        imageUrl: String?,
        gtin13: String?,
        servingSize: String?,
        englishName: String?,
        dutchName: String?,
        source: String?,
    ): Long {
        return queries.transactionWithResult {
            queries.insertManual(
                name = name,
                brand_name = brandName,
                energy_kcal_per_100g = energyKcalPer100g,
                product_url = productUrl,
                image_url = imageUrl,
                gtin13 = gtin13,
                serving_size = servingSize,
                english_name = englishName,
                dutch_name = dutchName,
                source = source
            )
            queries.selectLastInsertRowId().executeAsOne()
        }
    }

    fun delete(id: Long) {
        queries.deleteById(id)
    }

    /**
     * Search foods with order-agnostic, multi-field logic.
     * Steps and rationale:
     * - Trim and lowercase the query; if empty -> empty list (avoid noisy defaults in search mode).
     * - Multi-word: use the longest token for an initial LIKE to get a focused candidate set, then
     *   require that all tokens are present across any of the fields (name/english/dutch/brand),
     *   regardless of order. Rank by exact match, prefix match, token positions, Levenshtein, source, name.
     * - Single-word: fetch LIKE candidates across fields; if any, rank by exact, prefix, containsPos,
     *   Levenshtein distance, source priority, and name.
     * - Fuzzy fallback (Levenshtein over all rows) is only used when LIKE finds nothing AND query length >= 4.
     *   This guard avoids the previous issue where very short queries could show almost everything.
     */
    fun search(query: String): List<Food> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()

        // If the query looks like a GTIN-13 (EAN-13) barcode, try exact match on gtin13 first
        val digitsOnly = q.filter { it.isDigit() }
        if (digitsOnly.length == 13) {
            val byBarcode = queries.selectByGtin13(digitsOnly).executeAsList()
            if (byBarcode.isNotEmpty()) return byBarcode
        }

        fun sourcePriority(src: String?): Int = when (src?.lowercase()) {
            "manual" -> 0
            "nevo" -> 1
            "mealie" -> 2
            "ah" -> 3
            else -> 4
        }

        fun fields(f: Food): List<String> = listOfNotNull(
            f.name, f.english_name, f.dutch_name, f.brand_name
        )

        fun exactMatch(f: Food): Boolean = SearchUtils.exactMatch(fields(f), q)
        fun prefixMatch(f: Food): Boolean = SearchUtils.prefixMatch(fields(f), q)
        fun containsPos(f: Food): Int = SearchUtils.containsPos(fields(f), qLower)
        fun levScore(f: Food): Int {
            var best = Int.MAX_VALUE
            for (s in fields(f)) {
                val d = levenshtein(s.lowercase(), qLower)
                if (d < best) best = d
            }
            return best
        }

        // Multi-word, order-agnostic search: ensure all tokens are present in any order across fields
        val tokens = SearchUtils.tokenize(qLower)
        if (tokens.size >= 2) {
            val primary = SearchUtils.longestToken(tokens) ?: tokens.first()
            val likePrimary = "%$primary%"
            val initial = queries.searchByAny(likePrimary, likePrimary, likePrimary, likePrimary).executeAsList()
            fun tokensPosSum(f: Food): Int = SearchUtils.tokensPosSum(fields(f), tokens)
            val filtered = initial.filter { f ->
                SearchUtils.tokensPresent(fields(f), tokens)
            }
            if (filtered.isNotEmpty()) {
                return filtered.sortedWith(
                    compareBy<Food> { if (exactMatch(it)) 0 else 1 }
                        .thenBy { if (prefixMatch(it)) 0 else 1 }
                        .thenBy { tokensPosSum(it) }
                        .thenBy { levScore(it) }
                        .thenBy { sourcePriority(it.source) }
                        .thenBy { it.name.lowercase() }
                )
            }
            // If nothing matched all tokens, continue with normal flow and fuzzy fallback below
        }

        val like = "%$qLower%"
        // Fetch candidates across multiple fields (case-insensitive)
        val candidatesLike = queries.searchByAny(like, like, like, like).executeAsList()

        // If LIKE found candidates, use the existing relevance ordering
        if (candidatesLike.isNotEmpty()) {
            return candidatesLike.sortedWith(
                compareBy<Food> { if (exactMatch(it)) 0 else 1 }
                    .thenBy { if (prefixMatch(it)) 0 else 1 }
                    .thenBy { containsPos(it) }
                    .thenBy { levScore(it) }
                    .thenBy { sourcePriority(it.source) }
                    .thenBy { it.name.lowercase() }
            )
        }

        // Fuzzy fallback: when LIKE returns nothing (e.g., misspellings)
        val all = getAll()
        if (all.isEmpty()) return emptyList()
        return all.sortedWith(
            compareBy<Food> { levScore(it) }
                .thenBy { sourcePriority(it.source) }
                .thenBy { it.name.lowercase() }
        ).take(100)
    }

    fun insert(name: String, description: String?) {
        queries.insert(name = name, description = description)
    }
}
