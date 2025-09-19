package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.util.currentDateIso

class IntakeRepository(database: Database) { 
    private val foodQ = database.foodQueries
    private val mealQ = database.mealsQueries
    private val intakeQ = database.intakeQueries

    // Basic queries
    fun getFoodById(id: Long) = tryExecute { foodQ.selectById(id).executeAsOne() }
    fun getMealById(id: Long) = tryExecute { mealQ.mealSelectById(id).executeAsOne() }
    fun getFrequentFoods(mealType: String, limit: Long) = intakeQ.frequentFoods(mealType, limit).executeAsList()

    fun getIntakeByMealAndDateRange(mealType: String, startIso: String, endIso: String) =
        intakeQ.selectIntakeByMealAndDateRange(mealType, startIso, endIso).executeAsList()

    fun deleteIntakeById(id: Long) = intakeQ.deleteIntakeById(id)

    fun setLeftoversForMealTypeOnDate(mealType: String, dateIso: String, enabled: Boolean) {
        val (start, end) = todayRange(dateIso)
        val todays = getIntakeByMealAndDateRange(mealType, start, end)
        todays.forEach { row ->
            intakeQ.updateLeftoverById(if (enabled) 1 else 0, row.id)
        }
    }

    fun isLeftoversMarkedForMealTypeOnDate(mealType: String, dateIso: String): Boolean {
        val (start, end) = todayRange(dateIso)
        val todays = getIntakeByMealAndDateRange(mealType, start, end)
        return todays.any { it.leftover != 0L }
    }

    fun getAllLeftoverIntakesExcludingDate(dateIso: String? = null): List<Intake> {
        val all = intakeQ.selectAllLeftovers().executeAsList()
        if(dateIso == null) return all
        return all.filterNot { it.timestamp.startsWith(dateIso) }
    }


    // Simplified logging with automatic merging
    fun logOrUpdateFoodIntake(foodId: Long, quantityG: Double, mealType: String, dateIso: String) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        
        val today = todayRange(dateIso)
        val existing = getIntakeByMealAndDateRange(mealType, today.first, today.second)
            .firstOrNull { it.source_type == "FOOD" && it.source_food_id == foodId }
            
        if (existing == null) {
            logFoodIntake(foodId, quantityG, nowIso(dateIso), mealType)
        } else {
            updateIntakeQuantity(existing.id, existing.quantity_g + quantityG)
        }
    }

    fun logOrUpdateMealIntake(mealId: Long, quantityG: Double, mealType: String, dateIso: String) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        
        val today = todayRange(dateIso)
        val existing = getIntakeByMealAndDateRange(mealType, today.first, today.second)
            .firstOrNull { it.source_type == "MEAL" && it.source_meal_id == mealId }
            
        if (existing == null) {
            logMealIntake(mealId, quantityG, nowIso(dateIso), mealType)
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

    fun getMealPortions(mealId: Long): Double {
        return tryExecute { mealQ.mealSelectPortionsById(mealId).executeAsOne() } ?: 1.0
    }

    fun saveCurrentMealFromIntakes(mealType: String, name: String, totalPortions: Double, dateIso: String): Long {
        val today = todayRange(dateIso)
        val list = getIntakeByMealAndDateRange(mealType, today.first, today.second)
        val items = list.filter { it.source_food_id != null }.map { it.source_food_id!! to it.quantity_g }
        if (items.isEmpty()) return 0L
        val mealId = createMeal(name, totalPortions, items)
        val totalGrams = items.sumOf { it.second }
        // Delete only the food items that were saved into the meal
        list.filter { it.source_food_id != null }.forEach { deleteIntakeById(it.id) }
        // Log new meal as a single entry
        logOrUpdateMealIntake(mealId, totalGrams, mealType, dateIso)
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
    fun searchMeals(query: String): List<Meal> {
        if (query.trim().isEmpty()) return emptyList()
        val like = "%${query.trim().lowercase()}%"
        return mealQ.mealSearchByAny(like, like).executeAsList()
    }

    fun listAllMeals(): List<Meal> {
        // Using LIKE with wildcards to return all meals ordered by name
        return mealQ.mealSearchByAny("%%", "%%").executeAsList()
    }

    // Expand a logged meal into separate food intakes and remove the original meal entry
    fun copyMealItemsIntoMealTime(mealId: Long, mealType: String, dateIso: String) {
        val (start, end) = todayRange(dateIso)
        // 1) Find the specific intake entry for this mealId in the given mealType today
        val entries = getIntakeByMealAndDateRange(mealType, start, end)
        val intake = entries.firstOrNull { it.source_type == "MEAL" && it.source_meal_id == mealId } ?: return

        // 2) Portion amount = intake.quantity_g / portionGramsPerMeal
        val portionGrams = getMealPortionGrams(mealId)
        val portionAmount = if (portionGrams > 0.0) intake.quantity_g / portionGrams else 0.0
        val totalPortions = getMealPortions(mealId)

        // 3) Fetch meal items with their base grams
        val items = mealQ.mealItemsForMealFull(mealId).executeAsList()

        // 4) Add each as a FOOD intake with grams scaled by portionAmount (merge with existing if present)
        items.forEach { row ->
            val foodId = row.id // Food id from joined row
            val scaledGrams = ((row.meal_item_quantity_g * portionAmount) / totalPortions).coerceAtLeast(0.0)
            logOrUpdateFoodIntake(foodId, scaledGrams, mealType, dateIso)
        }

        // 5) Remove the original meal intake entry
        deleteIntakeById(intake.id)
    }

    // Utilities
    private fun <T> tryExecute(block: () -> T): T? = try { block() } catch (_: Exception) { null }
    private fun nowIso(dateIso: String? = null) = (dateIso ?: currentDateIso()) + "T12:00:00"
    private fun todayRange(dateIso: String? = null): Pair<String, String> {
        val date = dateIso ?: currentDateIso()
        return "${date}T00:00:00" to "${date}T23:59:59"
    }

    // Flip leftover for a specific intake row
    fun setLeftoverFlagById(id: Long, enabled: Boolean) {
        intakeQ.updateLeftoverById(if (enabled) 1 else 0, id)
    }
}