package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Intake

class IntakeRepository(database: Database) {
    private val foodQ = database.foodQueries
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
        // Adjust energy proportionally to the new quantity in SQL to support both FOOD and MEAL sources.
        // Handles quantity_g = 0 by recomputing from Food when possible.
        intakeQ.updateIntakeQuantity(newQuantityG, newQuantityG, newQuantityG, id)
    }
}
