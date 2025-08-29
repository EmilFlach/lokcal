package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database

class MealRepository(database: Database) {
    private val mealQ = database.mealsQueries

    // Basic queries
    fun getMealById(id: Long) = tryExecute { mealQ.mealSelectById(id).executeAsOne() }

    // Meal editing
    data class MealEditorItem(
        val mealItemId: Long,
        val food: com.emilflach.lokcal.Food,
        val quantityG: Double,
    )

    fun getMealItemsWithFood(mealId: Long): List<MealEditorItem> {
        return mealQ.mealItemsForMealFull(mealId).executeAsList().map { row ->
            MealEditorItem(
                mealItemId = row.meal_item_id,
                food = com.emilflach.lokcal.Food(
                    id = row.id,
                    name = row.name,
                    description = row.description,
                    brand = row.brand,
                    category = row.category,
                    energy_kcal_per_100g = row.energy_kcal_per_100g,
                    unit = row.unit,
                    external_id = row.external_id,
                    plural_name = row.plural_name,
                    english_name = row.english_name,
                    dutch_name = row.dutch_name,
                    brand_name = row.brand_name,
                    serving_size = row.serving_size,
                    gtin13 = row.gtin13,
                    image_url = row.image_url,
                    product_url = row.product_url,
                    source = row.source,
                    label_id = row.label_id,
                    created_at_source = row.created_at_source,
                    updated_at_source = row.updated_at_source,
                    on_hand = row.on_hand,
                    raw_json = row.raw_json,
                    created_at = row.created_at
                ),
                quantityG = row.meal_item_quantity_g
            )
        }
    }

    fun updateMealMeta(id: Long, name: String, imageUrl: String?, totalPortions: Double) =
        mealQ.updateMealMeta(name, imageUrl, totalPortions, id)

    fun updateMealItemQuantity(itemId: Long, grams: Double) {
        require(grams >= 0.0) { "grams must be >= 0" }
        mealQ.updateMealItemQuantity(grams, itemId)
    }

    fun deleteMeal(mealId: Long) = mealQ.deleteMealById(mealId)
    fun deleteMealItem(itemId: Long) = mealQ.deleteMealItemById(itemId)

    private fun <T> tryExecute(block: () -> T): T? = try { block() } catch (_: Exception) { null }
}
