package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food

class MealRepository(database: Database) {
    private val mealQ = database.mealsQueries

    // Basic queries
    suspend fun getMealById(id: Long) = try { mealQ.mealSelectById(id).awaitAsOne() } catch (_: Exception) { null }

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

    private fun <T> tryExecute(block: () -> T): T? = try { block() } catch (_: Exception) { null }
}
