package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal

class MealRepository(database: Database) {
    private val mealQ = database.mealsQueries

    // Basic queries
    suspend fun getMealById(id: Long) = try { mealQ.mealSelectById(id).awaitAsOne() } catch (_: Exception) { null }

    // Search
    /**
     * Returns meals ranked by relevance for the given query. SQL handles scoring and ordering.
     * Kotlin only applies the short-query guard (<=3 chars) and returns the pre-sorted list.
     * No fuzzy fallback for meals to prevent broad/irrelevant results.
     */
    suspend fun searchMeals(query: String): List<Meal> = searchMealsWithCounts(query).map { it.first }

    /** Like [searchMeals] but also returns the track count for each result. */
    suspend fun searchMealsWithCounts(query: String): List<Pair<Meal, Int>> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()

        val tokens = qLower.split(Regex("\\s+")).filter { it.isNotBlank() }
        val primary = tokens.maxByOrNull { it.length } ?: qLower
        val candidates = mealQ.mealSearchRanked("%$primary%", qLower).awaitAsList()

        if (candidates.isEmpty()) return emptyList()

        // For very short queries (<= 3), avoid overly broad results by keeping only prefix/exact matches
        val filtered = if (qLower.length <= 3) {
            candidates.filter { it.name.equals(q, ignoreCase = true) || it.name.startsWith(q, ignoreCase = true) }
        } else {
            candidates
        }

        return filtered.map { row ->
            Pair(
                Meal(row.id, row.name, row.description, row.image_url, row.total_portions, row.created_at),
                row.track_count.toInt()
            )
        }
    }

    suspend fun listAllMeals(): List<Meal> {
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
