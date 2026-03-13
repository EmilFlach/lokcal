package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.util.SearchUtils
import com.emilflach.lokcal.util.currentDateIso

class IntakeRepository(database: Database) { 
    private val foodQ = database.foodQueries
    private val mealQ = database.mealsQueries
    private val intakeQ = database.intakeQueries

    // Basic queries
    suspend fun getFoodById(id: Long) = try { foodQ.selectById(id).awaitAsOne() } catch (_: Exception) { null }
    suspend fun getMealById(id: Long) = try { mealQ.mealSelectById(id).awaitAsOne() } catch (_: Exception) { null }
    suspend fun getFrequentFoods(mealType: String, limit: Long) = intakeQ.frequentFoods(mealType, limit).awaitAsList()

    suspend fun getIntakeByMealAndDateRange(mealType: String, startIso: String, endIso: String) =
        intakeQ.selectIntakeByMealAndDateRange(mealType, startIso, endIso).awaitAsList()

    suspend fun getLatestIntakeId(): Long? = intakeQ.selectLatestIntakeId().awaitAsOneOrNull()

    suspend fun deleteIntakeById(id: Long) = intakeQ.deleteIntakeById(id)

    suspend fun setLeftoversForMealTypeOnDate(mealType: String, dateIso: String, enabled: Boolean) {
        val (start, end) = todayRange(dateIso)
        val todays = getIntakeByMealAndDateRange(mealType, start, end)
        todays.forEach { row ->
            intakeQ.updateLeftoverById(if (enabled) 1 else 0, row.id)
        }
    }

    suspend fun isLeftoversMarkedForMealTypeOnDate(mealType: String, dateIso: String): Boolean {
        val (start, end) = todayRange(dateIso)
        val todays = getIntakeByMealAndDateRange(mealType, start, end)
        return todays.any { it.leftover != 0L }
    }

    suspend fun getAllLeftoverIntakesExcludingDate(dateIso: String? = null): List<Intake> {
        val all = intakeQ.selectAllLeftovers().awaitAsList()
        if(dateIso == null) return all
        return all.filterNot { it.timestamp.startsWith(dateIso) }
    }


    // Simplified logging with automatic merging
    suspend fun logOrUpdateFoodIntake(foodId: Long, quantityG: Double, mealType: String, dateIso: String, refreshId: Boolean = false) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        
        val today = todayRange(dateIso)
        val existing = getIntakeByMealAndDateRange(mealType, today.first, today.second)
            .firstOrNull { it.source_type == "FOOD" && it.source_food_id == foodId }
            
        if (existing == null) {
            logFoodIntake(foodId, quantityG, nowIso(dateIso), mealType)
        } else {
            updateIntakeQuantity(existing.id, existing.quantity_g + quantityG, refreshId)
        }
    }

    suspend fun logOrUpdateMealIntake(mealId: Long, quantityG: Double, mealType: String, dateIso: String, refreshId: Boolean = false) {
        require(quantityG >= 0.0) { "quantityG must be >= 0" }
        
        val today = todayRange(dateIso)
        val existing = getIntakeByMealAndDateRange(mealType, today.first, today.second)
            .firstOrNull { it.source_type == "MEAL" && it.source_meal_id == mealId }
            
        if (existing == null) {
            logMealIntake(mealId, quantityG, nowIso(dateIso), mealType)
        } else {
            updateIntakeQuantity(existing.id, existing.quantity_g + quantityG, refreshId)
        }
    }

    private suspend fun logFoodIntake(foodId: Long, quantityG: Double, timestamp: String, mealType: String) {
        val food = foodQ.selectById(foodId).awaitAsOne()
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

    suspend fun updateIntakeQuantity(id: Long, newQuantityG: Double, refreshId: Boolean = false) {
        require(newQuantityG >= 0.0) { "newQuantityG must be >= 0" }
        
        val intake = try { intakeQ.selectIntakeById(id).awaitAsOne() } catch (_: Exception) { null } ?: return
        
        if (intake.source_type == "MEAL") {
            val mealId = intake.source_meal_id ?: return
            val (totalG, totalKcal) = computeMealTotals(mealId)
            val kcalForQty = if (totalG > 0) totalKcal * (newQuantityG / totalG) else 0.0
            if (refreshId) {
                intakeQ.updateIntakeQuantityAndIdDirect(kcalForQty, newQuantityG, id)
            } else {
                intakeQ.updateIntakeQuantityDirect(kcalForQty, newQuantityG, id)
            }
        } else {
            val foodId = intake.source_food_id ?: return
            val food = try { foodQ.selectById(foodId).awaitAsOne() } catch (_: Exception) { null } ?: return
            val kcalPer100g = food.energy_kcal_per_100g
            val totalKcal = (kcalPer100g * newQuantityG) / 100.0
            
            if (refreshId) {
                intakeQ.updateIntakeQuantityAndIdDirect(totalKcal, newQuantityG, id)
            } else {
                intakeQ.updateIntakeQuantityDirect(totalKcal, newQuantityG, id)
            }
        }
    }

    // Meal operations
    suspend fun createMeal(name: String, totalPortions: Double, items: List<Pair<Long, Double>>): Long {
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

    suspend fun computeMealTotals(mealId: Long): Pair<Double, Double> {
        val row = mealQ.mealTotals(mealId).awaitAsOne()
        return row.total_g to row.total_kcal
    }

    suspend fun getMealPortionGrams(mealId: Long): Double {
        val portions = try { mealQ.mealSelectPortionsById(mealId).awaitAsOne() } catch (_: Exception) { null } ?: 1.0
        val (totalG, _) = computeMealTotals(mealId)
        return if (portions > 0) totalG / portions else totalG
    }

    suspend fun getMealPortions(mealId: Long): Double {
        return try { mealQ.mealSelectPortionsById(mealId).awaitAsOne() } catch (_: Exception) { null } ?: 1.0
    }

    suspend fun saveCurrentMealFromIntakes(mealType: String, name: String, totalPortions: Double, dateIso: String): Long {
        val today = todayRange(dateIso)
        val list = getIntakeByMealAndDateRange(mealType, today.first, today.second)
        val items = list.filter { it.source_food_id != null }.map { it.source_food_id!! to it.quantity_g }
        if (items.isEmpty()) return 0L
        val mealId = createMeal(name, totalPortions, items)
        val totalGrams = items.sumOf { it.second }
        // Delete only the food items that were saved into the meal
        list.filter { it.source_food_id != null }.forEach { deleteIntakeById(it.id) }
        // Log new meal as a single entry
        logOrUpdateMealIntake(mealId, totalGrams, mealType, dateIso, refreshId = false)
        return mealId
    }

    private suspend fun logMealIntake(mealId: Long, quantityG: Double, timestamp: String, mealType: String) {
        val meal = try { mealQ.mealSelectById(mealId).awaitAsOne() } catch (_: Exception) { null }
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
    suspend fun searchMeals(query: String): List<Meal> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val qLower = q.lowercase()

        fun fields(m: Meal): List<String> = listOfNotNull(m.name)
        fun exactMatch(m: Meal): Boolean = SearchUtils.exactMatch(fields(m), q)
        fun prefixMatch(m: Meal): Boolean = SearchUtils.prefixMatch(fields(m), q)
        fun containsPos(m: Meal): Int = SearchUtils.containsPos(fields(m), qLower)

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
                    compareBy<Meal> { if (exactMatch(it)) 0 else 1 }
                        .thenBy { if (prefixMatch(it)) 0 else 1 }
                        .thenBy { SearchUtils.tokensPosSum(fields(it), tokens) }
                        .thenBy { it.name.lowercase() }
                )
            }
            // If nothing matched all tokens, continue to single-token logic below
        }

        val like = "%$qLower%"
        val candidates = mealQ.mealSearchByAny(like,).awaitAsList()
        if (candidates.isEmpty()) return emptyList()

        // For very short queries (<= 3), avoid overly broad results by keeping only prefix/exact matches
        if (qLower.length <= 3) {
            val strict = candidates.filter { exactMatch(it) || prefixMatch(it) }
            if (strict.isNotEmpty()) return strict.sortedWith(
                compareBy<Meal> { if (exactMatch(it)) 0 else 1 }
                    .thenBy { if (prefixMatch(it)) 0 else 1 }
                    .thenBy { it.name.lowercase() }
            )
            // If no strict matches, return empty to avoid noise
            return emptyList()
        }

        return candidates.sortedWith(
            compareBy<Meal> { if (exactMatch(it)) 0 else 1 }
                .thenBy { if (prefixMatch(it)) 0 else 1 }
                .thenBy { containsPos(it) }
                .thenBy { it.name.lowercase() }
        )
    }

    suspend fun listAllMeals(): List<Meal> {
        // Using LIKE with wildcards to return all meals ordered by name
        return mealQ.mealSearchByAny("%%").awaitAsList()
    }

    // Expand a logged meal into separate food intakes and remove the original meal entry
    suspend fun copyMealItemsIntoMealTime(mealId: Long, mealType: String, dateIso: String) {
        val (start, end) = todayRange(dateIso)
        // 1) Find the specific intake entry for this mealId in the given mealType today
        val entries = getIntakeByMealAndDateRange(mealType, start, end)
        val intake = entries.firstOrNull { it.source_type == "MEAL" && it.source_meal_id == mealId } ?: return

        // 2) Portion amount = intake.quantity_g / portionGramsPerMeal
        val portionGrams = getMealPortionGrams(mealId)
        val portionAmount = if (portionGrams > 0.0) intake.quantity_g / portionGrams else 0.0
        val totalPortions = getMealPortions(mealId)

        // 3) Fetch meal items with their base grams
        val items = mealQ.mealItemsForMealFull(mealId).awaitAsList()

        // 4) Add each as a FOOD intake with grams scaled by portionAmount (merge with existing if present)
        items.forEach { row ->
            val foodId = row.id // Food id from joined row
            val scaledGrams = ((row.meal_item_quantity_g * portionAmount) / totalPortions).coerceAtLeast(0.0)
            logOrUpdateFoodIntake(foodId, scaledGrams, mealType, dateIso, refreshId = false)
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
    suspend fun setLeftoverFlagById(id: Long, enabled: Boolean) {
        intakeQ.updateLeftoverById(if (enabled) 1 else 0, id)
    }

    suspend fun getMostEatenByWeight(startIso: String, endIso: String) =
        intakeQ.statsMostEatenByWeight(startIso, endIso, startIso, endIso).awaitAsList()

    suspend fun getDailyKcal(startIso: String, endIso: String) =
        intakeQ.statsDailyKcal(startIso, endIso).awaitAsList()

    suspend fun countDaysWithInformation() =
        intakeQ.countDaysWithInformation().awaitAsOne()

    suspend fun getDaysWithInformation() =
        intakeQ.getDaysWithInformation().awaitAsList()

    suspend fun getTotalKcalEaten(startIso: String, endIso: String) =
        intakeQ.getTotalKcalEaten(startIso, endIso).awaitAsOne().SUM ?: 0.0

    suspend fun getTotalWeightEatenG(startIso: String, endIso: String) =
        intakeQ.getTotalWeightEatenG(startIso, endIso).awaitAsOne().SUM ?: 0.0

    suspend fun getCountTrackedIntakes(startIso: String, endIso: String) =
        intakeQ.getCountTrackedIntakes(startIso, endIso).awaitAsOne()

    suspend fun getAllItemFrequencies() = intakeQ.allItemFrequencies().awaitAsList()

    suspend fun getItemsMissingImage() = intakeQ.itemsMissingImage().awaitAsList()
}