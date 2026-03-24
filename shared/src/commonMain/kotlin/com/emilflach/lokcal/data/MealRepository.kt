package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.util.SearchUtils

class MealRepository(database: Database) {
    private val mealQ = database.mealsQueries

    // Basic queries
    suspend fun getMealById(id: Long) = try { mealQ.mealSelectById(id).awaitAsOne() } catch (_: Exception) { null }

    // Search
    /**
     * Search meals with order-agnostic, multi-field logic (name).
     * Steps and rationale:
     * - Trim and lowercase the query; if empty -> empty list.
     * - Multi-word: use the longest token for an initial LIKE to get candidates, require all tokens
     *   to be present across fields (order-agnostic). Rank by exact, prefix, tokensPosSum, then name.
     * - Single-word: fetch LIKE candidates; if none -> empty. For very short queries (<=3), keep only
     *   exact/prefix results to avoid noisy matches; otherwise rank by exact, prefix, containsPos, name.
     * - No fuzzy (Levenshtein) fallback for meals to prevent broad/irrelevant results.
     */
    suspend fun searchMeals(query: String, trackingCounts: Map<Long, Long> = emptyMap()): List<Meal> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()

        fun fields(m: Meal): List<String> = listOfNotNull(m.name)
        fun exactMatch(m: Meal): Boolean = SearchUtils.exactMatch(fields(m), q)
        fun prefixMatch(m: Meal): Boolean = SearchUtils.prefixMatch(fields(m), q)
        fun containsPos(m: Meal): Int = SearchUtils.containsPos(fields(m), qLower)
        fun trackingCount(m: Meal): Long = trackingCounts[m.id] ?: 0L

        // Order-agnostic multi-word search: ensure all tokens present across names
        val tokens = SearchUtils.tokenize(qLower)
        if (tokens.size >= 2) {
            val primary = SearchUtils.longestToken(tokens) ?: tokens.first()
            val likePrimary = "%$primary%"
            val initial = mealQ.mealSearchByAny(likePrimary).awaitAsList()
            val filtered = initial.filter { m ->
                SearchUtils.tokensPresent(fields(m), tokens)
            }
            if (filtered.isNotEmpty()) {
                return filtered.sortedWith(
                    compareByDescending<Meal> { trackingCount(it) > 0 }
                        .thenByDescending { trackingCount(it) }
                        .thenBy { if (exactMatch(it)) 0 else 1 }
                        .thenBy { if (prefixMatch(it)) 0 else 1 }
                        .thenBy { SearchUtils.tokensPosSum(fields(it), tokens) }
                        .thenBy { it.name.lowercase() }
                )
            }
            // If nothing matched all tokens, continue to single-token logic below
        }

        val like = "%$qLower%"
        val candidates = mealQ.mealSearchByAny(like).awaitAsList()
        if (candidates.isEmpty()) return emptyList()

        // For very short queries (<= 3), avoid overly broad results by keeping only prefix/exact matches
        if (qLower.length <= 3) {
            val strict = candidates.filter { exactMatch(it) || prefixMatch(it) }
            if (strict.isNotEmpty()) return strict.sortedWith(
                compareByDescending<Meal> { trackingCount(it) > 0 }
                    .thenByDescending { trackingCount(it) }
                    .thenBy { if (exactMatch(it)) 0 else 1 }
                    .thenBy { if (prefixMatch(it)) 0 else 1 }
                    .thenBy { it.name.lowercase() }
            )
            // If no strict matches, return empty to avoid noise
            return emptyList()
        }

        return candidates.sortedWith(
            compareByDescending<Meal> { trackingCount(it) > 0 }
                .thenByDescending { trackingCount(it) }
                .thenBy { if (exactMatch(it)) 0 else 1 }
                .thenBy { if (prefixMatch(it)) 0 else 1 }
                .thenBy { containsPos(it) }
                .thenBy { it.name.lowercase() }
        )
    }

    suspend fun listAllMeals(): List<Meal> {
        // Using LIKE with wildcards to return all meals ordered by name
        return mealQ.mealSearchByAny("%%").awaitAsList()
    }

    // Meal editing
    data class MealEditorItem(
        val mealItemId: Long,
        val food: Food,
        val quantityG: Double,
    )

    suspend fun getMealItemsWithFood(mealId: Long): List<MealEditorItem> {
        return mealQ.mealItemsForMealFull(mealId).awaitAsList().map { row ->
            MealEditorItem(
                mealItemId = row.meal_item_id,
                food = Food(
                    id = row.id,
                    name = row.name,
                    energy_kcal_per_100g = row.energy_kcal_per_100g,
                    unit = row.unit,
                    serving_size = row.serving_size,
                    gtin13 = row.gtin13,
                    image_url = row.image_url,
                    product_url = row.product_url,
                    source = row.source,
                    created_at = row.created_at
                ),
                quantityG = row.meal_item_quantity_g
            )
        }
    }

    suspend fun updateMealMeta(id: Long, name: String, imageUrl: String?, totalPortions: Double) =
        mealQ.updateMealMeta(name, imageUrl, totalPortions, id)

    suspend fun updateMealItemQuantity(itemId: Long, grams: Double) {
        require(grams >= 0.0) { "grams must be >= 0" }
        mealQ.updateMealItemQuantity(grams, itemId)
    }

    suspend fun deleteMeal(mealId: Long) = mealQ.deleteMealById(mealId)
    suspend fun deleteMealItem(itemId: Long) = mealQ.deleteMealItemById(itemId)
}
