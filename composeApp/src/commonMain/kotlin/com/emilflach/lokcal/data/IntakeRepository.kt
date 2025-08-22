package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Intake

class IntakeRepository(private val database: Database) {
    private val foodQ = database.foodQueries
    private val mealQ = database.mealsQueries
    private val intakeQ = database.intakeQueries

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

    /**
     * Logs intake for a Meal by computing totals from its items at log time and storing a snapshot.
     * quantityG represents grams of the meal consumed. If the meal definition totals to T grams,
     * we compute per-gram from the meal definition and scale totals by quantityG/T.
     * mealType must be one of: BREAKFAST, LUNCH, DINNER, SNACK
     */
    fun logMealIntake(
        mealId: Long,
        quantityG: Double,
        timestamp: String,
        mealType: String,
        notes: String?
    ) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }

        val meal = mealQ.mealSelectById(mealId).executeAsOne()
        val items = mealQ.mealItemsForMealSimple(mealId).executeAsList()

        var totalGrams = 0.0
        var totalKcal = 0.0

        for (it in items) {
            val itemG = it.item_quantity_g
            totalGrams += itemG
            totalKcal += it.food_energy_kcal_per_100g * itemG / 100.0
        }

        val factor = if (totalGrams > 0.0) quantityG / totalGrams else 0.0
        val kcal = totalKcal * factor

        val itemName = meal.name

        intakeQ.logMealIntake(
            timestamp = timestamp,
            meal_type = mealType,
            source_meal_id = meal.id,
            quantity_g = quantityG,
            item_name = itemName,
            energy_kcal_total = kcal,
            notes = notes
        )
    }

    fun getIntakeByDateRange(startIso: String, endIso: String): List<Intake> {
        return intakeQ.selectIntakeByDateRange(startIso, endIso).executeAsList()
    }
}
