package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Intake

class IntakeRepository(database: Database) {
    private val foodQ = database.foodQueries
    private val mealQ = database.mealsQueries
    private val intakeQ = database.intakeQueries

    fun getFoodById(id: Long): com.emilflach.lokcal.Food? = try {
        foodQ.selectById(id).executeAsOne()
    } catch (_: Exception) {
        null
    }

    fun getRecentFoods(limit: Long): List<com.emilflach.lokcal.Food> {
        return intakeQ.recentFoods(limit).executeAsList()
    }

    /**
     * Logs intake for a single Food by taking a snapshot of the nutritional totals at log time.
     * mealType must be one of: BREAKFAST, LUNCH, DINNER, SNACK
     */
    fun logFoodIntake(
        foodId: Long,
        quantityG: Double,
        timestamp: String,
        mealType: String,
        notes: String?
    ) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        val food = foodQ.selectById(foodId).executeAsOne()

        val qty = quantityG
        val kcal = food.energy_kcal_per_100g * qty / 100.0

        val itemName = food.name

        intakeQ.logFoodIntake(
            timestamp = timestamp,
            meal_type = mealType,
            source_food_id = food.id,
            quantity_g = qty,
            item_name = itemName,
            energy_kcal_total = kcal,
            notes = notes
        )
    }

    fun getIntakeByDateRange(startIso: String, endIso: String): List<Intake> {
        return intakeQ.selectIntakeByDateRange(startIso, endIso).executeAsList()
    }

    fun getIntakeByMealAndDateRange(mealType: String, startIso: String, endIso: String): List<Intake> {
        return intakeQ.selectIntakeByMealAndDateRange(mealType, startIso, endIso).executeAsList()
    }

    fun deleteIntakeById(id: Long) {
        intakeQ.deleteIntakeById(id)
    }

    fun updateIntakeQuantity(id: Long, newQuantityG: Double) {
        require(newQuantityG >= 0.0) { "newQuantityG must be >= 0" }
        // For MEAL entries, when previous quantity was 0, SQL proportional update cannot recompute kcal.
        // In that case, recompute from the meal definition in repository and set directly.
        val intake = try { intakeQ.selectIntakeById(id).executeAsOne() } catch (_: Exception) { null }
        if (intake != null && intake.source_type == "MEAL") {
            val mealId = intake.source_meal_id ?: return
            val (totalG, totalKcal) = computeMealTotals(mealId)
            val kcalForQty = if (totalG > 0) totalKcal * (newQuantityG / totalG) else 0.0
            intakeQ.updateIntakeQuantityDirect(kcalForQty, newQuantityG, id)
            return
        }
        // Default: proportional or food-based recompute in SQL
        intakeQ.updateIntakeQuantity(newQuantityG, newQuantityG, newQuantityG, id)
    }
    // ----- Meal (formerly "Dish") support -----
    fun createMeal(name: String, totalPortions: Double, items: List<Pair<Long, Double>>): Long {
        require(totalPortions > 0) { "totalPortions must be > 0" }
        var mealId = 0L
        mealQ.transaction {
            // Insert meal and fetch id on the same connection/transaction
            mealQ.mealInsertWithPortions(name, null, totalPortions)
            mealId = mealQ.lastInsertId().executeAsOne()
            // Insert items for this meal id
            for ((foodId, grams) in items) {
                require(grams >= 0.0) { "Item grams must be >= 0" }
                mealQ.mealItemInsert(meal_id = mealId, food_id = foodId, quantity_g = grams)
            }
        }
        return mealId
    }

    fun computeMealTotals(mealId: Long): Pair<Double, Double> {
        val row = mealQ.mealTotals(mealId).executeAsOne()
        return row.total_g to row.total_kcal
    }

    fun getMealPortionGrams(mealId: Long): Double {
        val portions = try { mealQ.mealSelectPortionsById(mealId).executeAsOne() } catch (_: Exception) { 1.0 }
        val (totalG, _) = computeMealTotals(mealId)
        return if (portions > 0) totalG / portions else totalG
    }

    fun logMealIntake(
        mealId: Long,
        quantityG: Double,
        timestamp: String,
        mealType: String,
        notes: String?
    ) {
        // Be resilient if the meal row isn't available for any reason
        val meal = try { mealQ.mealSelectById(mealId).executeAsOne() } catch (_: Exception) { null }

        // Compute totals once and guard against division by zero
        val (totalG, totalKcal) = computeMealTotals(mealId)
        val qty = quantityG
        val kcalForQty = when {
            totalG > 0 && qty > 0 -> totalKcal * (qty / totalG)
            else -> totalKcal
        }
        intakeQ.logMealIntake(
            timestamp = timestamp,
            meal_type = mealType,
            source_meal_id = mealId,
            quantity_g = qty,
            item_name = meal?.name ?: "Meal",
            energy_kcal_total = kcalForQty,
            notes = notes
        )
    }

    // --- Search meals for intake screen ---
    fun searchMeals(query: String): List<com.emilflach.lokcal.Meal> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val like = "%${q.lowercase()}%"
        return mealQ.mealSearchByAny(like, like).executeAsList()
    }

    // --- Meal edit APIs ---
    fun getMealById(id: Long): com.emilflach.lokcal.Meal? = try {
        mealQ.mealSelectById(id).executeAsOne()
    } catch (_: Exception) { null }

    data class MealEditorItem(
        val mealItemId: Long,
        val food: com.emilflach.lokcal.Food,
        val quantityG: Double,
    )

    fun getMealItemsWithFood(mealId: Long): List<MealEditorItem> {
        val rows = mealQ.mealItemsForMealFull(mealId).executeAsList()
        return rows.map { row ->
            MealEditorItem(
                mealItemId = row.meal_item_id,
                food = com.emilflach.lokcal.Food(
                    id = row.id, // f.id from f.* becomes id
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

    fun updateMealMeta(id: Long, name: String, imageUrl: String?, totalPortions: Double) {
        mealQ.updateMealMeta(name, imageUrl, totalPortions, id)
    }

    fun updateMealItemQuantity(itemId: Long, grams: Double) {
        require(grams >= 0.0)
        mealQ.updateMealItemQuantity(grams, itemId)
    }

    fun deleteMeal(mealId: Long) {
        mealQ.deleteMealById(mealId)
    }

    fun deleteMealItem(itemId: Long) {
        mealQ.deleteMealItemById(itemId)
    }
}
