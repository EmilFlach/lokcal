package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class IntakeRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var repository: IntakeRepository
    private lateinit var foodRepository: FoodRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        repository = IntakeRepository(database)
        foodRepository = FoodRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testLogFoodIntake() = runTest {
        val foodId = foodRepository.insertManual("Apple", 52.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "BREAKFAST", "2024-01-15")

        val intakes = repository.getIntakeByMealAndDateRange("BREAKFAST", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, intakes.size)
        assertEquals("Apple", intakes[0].item_name)
        assertEquals(100.0, intakes[0].quantity_g)
        assertEquals(52.0, intakes[0].energy_kcal_total, 0.01)
        assertEquals("FOOD", intakes[0].source_type)
    }

    @Test
    fun testLogFoodIntakeMerges() = runTest {
        val foodId = foodRepository.insertManual("Banana", 89.0, null, null, null, null, null)

        // Log twice - should merge
        repository.logOrUpdateFoodIntake(foodId, 100.0, "LUNCH", "2024-01-15")
        repository.logOrUpdateFoodIntake(foodId, 50.0, "LUNCH", "2024-01-15")

        val intakes = repository.getIntakeByMealAndDateRange("LUNCH", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, intakes.size)
        assertEquals(150.0, intakes[0].quantity_g)
        assertEquals(133.5, intakes[0].energy_kcal_total, 0.01) // 89 * 1.5
    }

    @Test
    fun testLogMultipleDifferentFoods() = runTest {
        val appleId = foodRepository.insertManual("Apple", 52.0, null, null, null, null, null)
        val bananaId = foodRepository.insertManual("Banana", 89.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(appleId, 100.0, "SNACK", "2024-01-15")
        repository.logOrUpdateFoodIntake(bananaId, 120.0, "SNACK", "2024-01-15")

        val intakes = repository.getIntakeByMealAndDateRange("SNACK", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(2, intakes.size)
    }

    @Test
    fun testUpdateIntakeQuantity() = runTest {
        val foodId = foodRepository.insertManual("Orange", 47.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "BREAKFAST", "2024-01-15")
        val intakes = repository.getIntakeByMealAndDateRange("BREAKFAST", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        val intakeId = intakes[0].id

        repository.updateIntakeQuantity(intakeId, 200.0)

        val updated = repository.getIntakeByMealAndDateRange("BREAKFAST", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(200.0, updated[0].quantity_g)
        assertEquals(94.0, updated[0].energy_kcal_total, 0.01) // 47 * 2
    }

    @Test
    fun testDeleteIntake() = runTest {
        val foodId = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "DINNER", "2024-01-15")
        val intakes = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, intakes.size)

        repository.deleteIntakeById(intakes[0].id)

        val afterDelete = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testCreateMeal() = runTest {
        val food1 = foodRepository.insertManual("Pasta", 130.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Sauce", 50.0, null, null, null, null, null)

        val mealId = repository.createMeal(
            name = "Pasta Meal",
            totalPortions = 2.0,
            items = listOf(food1 to 200.0, food2 to 100.0)
        )

        val meal = repository.getMealById(mealId)
        assertNotNull(meal)
        assertEquals("Pasta Meal", meal.name)
        assertEquals(2.0, meal.total_portions)
    }

    @Test
    fun testComputeMealTotals() = runTest {
        val food1 = foodRepository.insertManual("Rice", 130.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Chicken", 165.0, null, null, null, null, null)

        val mealId = repository.createMeal(
            name = "Rice and Chicken",
            totalPortions = 1.0,
            items = listOf(food1 to 100.0, food2 to 150.0)
        )

        val (totalG, totalKcal) = repository.computeMealTotals(mealId)
        assertEquals(250.0, totalG)
        assertEquals(377.5, totalKcal, 0.01) // (130 * 1) + (165 * 1.5)
    }

    @Test
    fun testGetMealPortionGrams() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        val mealId = repository.createMeal(
            name = "Test Meal",
            totalPortions = 4.0,
            items = listOf(food1 to 400.0)
        )

        val portionGrams = repository.getMealPortionGrams(mealId)
        assertEquals(100.0, portionGrams) // 400g / 4 portions
    }

    @Test
    fun testLogMealIntake() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        val mealId = repository.createMeal(
            name = "Test Meal",
            totalPortions = 2.0,
            items = listOf(food1 to 200.0)
        )

        repository.logOrUpdateMealIntake(mealId, 100.0, "DINNER", "2024-01-15")

        val intakes = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, intakes.size)
        assertEquals("Test Meal", intakes[0].item_name)
        assertEquals("MEAL", intakes[0].source_type)
    }

    @Test
    fun testLogMealIntakeMerges() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        val mealId = repository.createMeal(
            name = "Test Meal",
            totalPortions = 1.0,
            items = listOf(food1 to 100.0)
        )

        repository.logOrUpdateMealIntake(mealId, 50.0, "LUNCH", "2024-01-15")
        repository.logOrUpdateMealIntake(mealId, 50.0, "LUNCH", "2024-01-15")

        val intakes = repository.getIntakeByMealAndDateRange("LUNCH", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, intakes.size)
        assertEquals(100.0, intakes[0].quantity_g)
    }

    @Test
    fun testSaveCurrentMealFromIntakes() = runTest {
        val food1 = foodRepository.insertManual("Ingredient1", 100.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Ingredient2", 200.0, null, null, null, null, null)

        // Log individual foods
        repository.logOrUpdateFoodIntake(food1, 100.0, "DINNER", "2024-01-15")
        repository.logOrUpdateFoodIntake(food2, 150.0, "DINNER", "2024-01-15")

        // Save as meal
        val mealId = repository.saveCurrentMealFromIntakes("DINNER", "New Meal", 2.0, "2024-01-15")

        assertTrue(mealId > 0)

        // Should have one meal intake now
        val intakes = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, intakes.size)
        assertEquals("MEAL", intakes[0].source_type)
    }

    @Test
    fun testCopyMealItemsIntoMealTime() = runTest {
        val food1 = foodRepository.insertManual("Food1", 100.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Food2", 200.0, null, null, null, null, null)

        val mealId = repository.createMeal(
            name = "Complex Meal",
            totalPortions = 2.0,
            items = listOf(food1 to 100.0, food2 to 200.0)
        )

        // Log the meal
        repository.logOrUpdateMealIntake(mealId, 150.0, "LUNCH", "2024-01-15")

        // Expand to individual foods
        repository.copyMealItemsIntoMealTime(mealId, "LUNCH", "2024-01-15")

        // Should now have individual food entries
        val intakes = repository.getIntakeByMealAndDateRange("LUNCH", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(2, intakes.size)
        assertTrue(intakes.all { it.source_type == "FOOD" })
    }

    @Test
    fun testLeftoverFlag() = runTest {
        val foodId = foodRepository.insertManual("Leftover Food", 100.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "DINNER", "2024-01-15")
        val intakes = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        val intakeId = intakes[0].id

        // Initially not leftover
        assertEquals(0L, intakes[0].leftover)

        // Mark as leftover
        repository.setLeftoverFlagById(intakeId, true)

        val updated = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1L, updated[0].leftover)
    }

    @Test
    fun testSetLeftoversForMealTypeOnDate() = runTest {
        val foodId = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "LUNCH", "2024-01-15")
        repository.logOrUpdateFoodIntake(foodId, 50.0, "DINNER", "2024-01-15")

        // Mark all LUNCH items as leftovers
        repository.setLeftoversForMealTypeOnDate("LUNCH", "2024-01-15", true)

        val lunchIntakes = repository.getIntakeByMealAndDateRange("LUNCH", "2024-01-15T00:00:00", "2024-01-15T23:59:59")
        val dinnerIntakes = repository.getIntakeByMealAndDateRange("DINNER", "2024-01-15T00:00:00", "2024-01-15T23:59:59")

        assertEquals(1L, lunchIntakes[0].leftover)
        assertEquals(0L, dinnerIntakes[0].leftover)
    }

    @Test
    fun testIsLeftoversMarkedForMealTypeOnDate() = runTest {
        val foodId = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "BREAKFAST", "2024-01-15")

        assertFalse(repository.isLeftoversMarkedForMealTypeOnDate("BREAKFAST", "2024-01-15"))

        repository.setLeftoversForMealTypeOnDate("BREAKFAST", "2024-01-15", true)

        assertTrue(repository.isLeftoversMarkedForMealTypeOnDate("BREAKFAST", "2024-01-15"))
    }

    @Test
    fun testGetAllLeftoverIntakes() = runTest {
        val foodId = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(foodId, 100.0, "LUNCH", "2024-01-15")
        repository.logOrUpdateFoodIntake(foodId, 50.0, "DINNER", "2024-01-15")

        repository.setLeftoversForMealTypeOnDate("LUNCH", "2024-01-15", true)

        val leftovers = repository.getAllLeftoverIntakesExcludingDate(null)
        assertEquals(1, leftovers.size)
    }

    @Test
    fun testGetFrequentFoods() = runTest {
        val food1 = foodRepository.insertManual("Often", 100.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Rare", 100.0, null, null, null, null, null)

        // Log food1 multiple times
        repository.logOrUpdateFoodIntake(food1, 100.0, "BREAKFAST", "2024-01-15")
        repository.logOrUpdateFoodIntake(food1, 100.0, "BREAKFAST", "2024-01-16")
        repository.logOrUpdateFoodIntake(food1, 100.0, "BREAKFAST", "2024-01-17")

        // Log food2 once
        repository.logOrUpdateFoodIntake(food2, 100.0, "BREAKFAST", "2024-01-15")

        val frequent = repository.getFrequentFoods("BREAKFAST", 10)
        assertTrue(frequent.size >= 2)
        assertEquals("Often", frequent[0].name) // Should be first due to frequency
    }

    @Test
    fun testGetTrackingCounts() = runTest {
        val food1 = foodRepository.insertManual("Food1", 100.0, null, null, null, null, null)

        repository.logOrUpdateFoodIntake(food1, 100.0, "LUNCH", "2024-01-15")
        repository.logOrUpdateFoodIntake(food1, 100.0, "LUNCH", "2024-01-16")

        val counts = repository.getTrackingCounts()
        assertTrue(counts.isNotEmpty())
        assertEquals(2L, counts["FOOD" to food1])
    }

    @Test
    fun testGetLatestIntakeId() = runTest {
        assertNull(repository.getLatestIntakeId())

        val foodId = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)
        repository.logOrUpdateFoodIntake(foodId, 100.0, "SNACK", "2024-01-15")

        val latestId = repository.getLatestIntakeId()
        assertNotNull(latestId)
        assertTrue(latestId > 0)
    }
}
