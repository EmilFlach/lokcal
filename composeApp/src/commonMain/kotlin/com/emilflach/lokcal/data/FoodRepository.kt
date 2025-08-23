package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.util.levenshtein

class FoodRepository(database: Database) {
    private val queries = database.foodQueries

    fun getAll(): List<Food> = queries.selectAll().executeAsList()

    fun search(query: String): List<Food> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()
        val like = "%$qLower%"
        // Fetch candidates across multiple fields (case-insensitive)
        val candidatesLike = queries.searchByAny(like, like, like, like).executeAsList()

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

        fun exactMatch(f: Food): Boolean = fields(f).any { it.equals(q, ignoreCase = true) }
        fun prefixMatch(f: Food): Boolean = fields(f).any { it.startsWith(q, ignoreCase = true) }
        fun containsPos(f: Food): Int {
            var best = Int.MAX_VALUE
            for (s in fields(f)) {
                val idx = s.lowercase().indexOf(qLower)
                if (idx >= 0 && idx < best) best = idx
            }
            return if (best == Int.MAX_VALUE) 9999 else best
        }
        fun levScore(f: Food): Int {
            var best = Int.MAX_VALUE
            for (s in fields(f)) {
                val d = levenshtein(s.lowercase(), qLower)
                if (d < best) best = d
            }
            return best
        }

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

        // Fuzzy fallback: when LIKE returns nothing (e.g., misspellings), search all by Levenshtein
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
