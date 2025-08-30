package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.util.currentDateIso

class IntakeRepository(database: Database) {
    private val foodQ = database.foodQueries
    private val mealQ = database.mealsQueries
    private val intakeQ = database.intakeQueries

    // Basic queries
    fun getFoodById(id: Long) = tryExecute { foodQ.selectById(id).executeAsOne() }
    fun getRecentFoods(limit: Long) = intakeQ.recentFoods(limit).executeAsList()

    fun getIntakeByMealAndDateRange(mealType: String, startIso: String, endIso: String) =
        intakeQ.selectIntakeByMealAndDateRange(mealType, startIso, endIso).executeAsList()

    fun deleteIntakeById(id: Long) = intakeQ.deleteIntakeById(id)

    // Simplified logging with automatic merging
    fun logOrUpdateFoodIntake(foodId: Long, quantityG: Double, mealType: String) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        
        val today = todayRange()
        val existing = getIntakeByMealAndDateRange(mealType, today.first, today.second)
            .firstOrNull { it.source_type == "FOOD" && it.source_food_id == foodId }
            
        if (existing == null) {
            logFoodIntake(foodId, quantityG, nowIso(), mealType)
        } else {
            updateIntakeQuantity(existing.id, existing.quantity_g + quantityG)
        }
    }

    fun logOrUpdateMealIntake(mealId: Long, quantityG: Double, mealType: String) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        
        val today = todayRange()
        val existing = getIntakeByMealAndDateRange(mealType, today.first, today.second)
            .firstOrNull { it.source_type == "MEAL" && it.source_meal_id == mealId }
            
        if (existing == null) {
            logMealIntake(mealId, quantityG, nowIso(), mealType)
        } else {
            updateIntakeQuantity(existing.id, existing.quantity_g + quantityG)
        }
    }

    private fun logFoodIntake(foodId: Long, quantityG: Double, timestamp: String, mealType: String) {
        val food = foodQ.selectById(foodId).executeAsOne()
        val kcal = food.energy_kcal_per_100g * quantityG / 100.0
        
        intakeQ.logFoodIntake(
            timestamp = timestamp,
            meal_type = mealType,
            source_food_id = food.id,
            quantity_g = quantityG,
            item_name = food.name,
            energy_kcal_total = kcal,
            notes = null
        )
    }

    fun updateIntakeQuantity(id: Long, newQuantityG: Double) {
        require(newQuantityG >= 0.0) { "newQuantityG must be >= 0" }
        
        val intake = tryExecute { intakeQ.selectIntakeById(id).executeAsOne() } ?: return
        
        if (intake.source_type == "MEAL") {
            val mealId = intake.source_meal_id ?: return
            val (totalG, totalKcal) = computeMealTotals(mealId)
            val kcalForQty = if (totalG > 0) totalKcal * (newQuantityG / totalG) else 0.0
            intakeQ.updateIntakeQuantityDirect(kcalForQty, newQuantityG, id)
        } else {
            intakeQ.updateIntakeQuantity(newQuantityG, newQuantityG, newQuantityG, id)
        }
    }

    // Meal operations
    fun createMeal(name: String, totalPortions: Double, items: List<Pair<Long, Double>>): Long {
        require(totalPortions > 0) { "totalPortions must be > 0" }
        var mealId = 0L
        mealQ.transaction {
            mealQ.mealInsertWithPortions(name, null, totalPortions)
            mealId = mealQ.lastInsertId().executeAsOne()
            items.forEach { (foodId, grams) ->
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
        val portions = tryExecute { mealQ.mealSelectPortionsById(mealId).executeAsOne() } ?: 1.0
        val (totalG, _) = computeMealTotals(mealId)
        return if (portions > 0) totalG / portions else totalG
    }

    fun saveCurrentMealFromIntakes(mealType: String, name: String, totalPortions: Double): Long {
        val today = todayRange()
        val list = getIntakeByMealAndDateRange(mealType, today.first, today.second)
        val items = list.filter { it.source_food_id != null }.map { it.source_food_id!! to it.quantity_g }
        if (items.isEmpty()) return 0L
        val mealId = createMeal(name, totalPortions, items)
        val totalGrams = items.sumOf { it.second }
        // Delete only the food items that were saved into the meal
        list.filter { it.source_food_id != null }.forEach { deleteIntakeById(it.id) }
        // Log new meal as a single entry
        logOrUpdateMealIntake(mealId, totalGrams, mealType)
        return mealId
    }

    private fun logMealIntake(mealId: Long, quantityG: Double, timestamp: String, mealType: String) {
        val meal = tryExecute { mealQ.mealSelectById(mealId).executeAsOne() }
        val (totalG, totalKcal) = computeMealTotals(mealId)
        val kcalForQty = if (totalG > 0 && quantityG > 0) totalKcal * (quantityG / totalG) else totalKcal
        
        intakeQ.logMealIntake(
            timestamp = timestamp,
            meal_type = mealType,
            source_meal_id = mealId,
            quantity_g = quantityG,
            item_name = meal?.name ?: "Meal",
            energy_kcal_total = kcalForQty,
            notes = null
        )
    }

    // Search
    fun searchMeals(query: String): List<com.emilflach.lokcal.Meal> {
        if (query.trim().isEmpty()) return emptyList()
        val like = "%${query.trim().lowercase()}%"
        return mealQ.mealSearchByAny(like, like).executeAsList()
    }

    // Utilities
    private fun <T> tryExecute(block: () -> T): T? = try { block() } catch (_: Exception) { null }
    private fun nowIso() = currentDateIso() + "T12:00:00"
    private fun todayRange(): Pair<String, String> {
        val date = currentDateIso()
        return "${date}T00:00:00" to "${date}T23:59:59"
    }
}