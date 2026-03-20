package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MealRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var repository: MealRepository
    private lateinit var intakeRepository: IntakeRepository
    private lateinit var foodRepository: FoodRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        // Enable foreign key constraints for SQLite
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        database = Database(driver)
        repository = MealRepository(database)
        intakeRepository = IntakeRepository(database)
        foodRepository = FoodRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testGetMealById() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)
        val mealId = intakeRepository.createMeal("Test Meal", 1.0, listOf(food1 to 100.0))

        val meal = repository.getMealById(mealId)
        assertNotNull(meal)
        assertEquals("Test Meal", meal.name)
        assertEquals(1.0, meal.total_portions)
    }

    @Test
    fun testGetMealItemsWithFood() = runTest {
        val food1 = foodRepository.insertManual("Rice", 130.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Beans", 80.0, null, null, null, null, null)

        val mealId = intakeRepository.createMeal(
            "Rice and Beans",
            2.0,
            listOf(food1 to 200.0, food2 to 150.0)
        )

        val items = repository.getMealItemsWithFood(mealId)
        assertEquals(2, items.size)
        assertEquals("Rice", items[0].food.name)
        assertEquals(200.0, items[0].quantityG)
        assertEquals("Beans", items[1].food.name)
        assertEquals(150.0, items[1].quantityG)
    }

    @Test
    fun testUpdateMealMeta() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)
        val mealId = intakeRepository.createMeal("Original Name", 1.0, listOf(food1 to 100.0))

        repository.updateMealMeta(mealId, "New Name", "https://example.com/image.jpg", 3.0)

        val meal = repository.getMealById(mealId)
        assertNotNull(meal)
        assertEquals("New Name", meal.name)
        assertEquals("https://example.com/image.jpg", meal.image_url)
        assertEquals(3.0, meal.total_portions)
    }

    @Test
    fun testUpdateMealItemQuantity() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)
        val mealId = intakeRepository.createMeal("Meal", 1.0, listOf(food1 to 100.0))

        val items = repository.getMealItemsWithFood(mealId)
        val mealItemId = items[0].mealItemId

        repository.updateMealItemQuantity(mealItemId, 250.0)

        val updated = repository.getMealItemsWithFood(mealId)
        assertEquals(250.0, updated[0].quantityG)
    }

    @Test
    fun testDeleteMeal() = runTest {
        val food1 = foodRepository.insertManual("Food", 100.0, null, null, null, null, null)
        val mealId = intakeRepository.createMeal("Meal to Delete", 1.0, listOf(food1 to 100.0))

        assertNotNull(repository.getMealById(mealId))

        repository.deleteMeal(mealId)

        assertNull(repository.getMealById(mealId))
    }

    @Test
    fun testDeleteMealItem() = runTest {
        val food1 = foodRepository.insertManual("Food1", 100.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Food2", 100.0, null, null, null, null, null)

        val mealId = intakeRepository.createMeal("Meal", 1.0, listOf(food1 to 100.0, food2 to 100.0))

        val items = repository.getMealItemsWithFood(mealId)
        assertEquals(2, items.size)

        repository.deleteMealItem(items[0].mealItemId)

        val remaining = repository.getMealItemsWithFood(mealId)
        assertEquals(1, remaining.size)
    }

    @Test
    fun testDeleteMealCascadesToItems() = runTest {
        val food1 = foodRepository.insertManual("Food1", 100.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Food2", 100.0, null, null, null, null, null)

        val mealId = intakeRepository.createMeal("Meal", 1.0, listOf(food1 to 100.0, food2 to 100.0))

        // Verify meal exists before deletion
        assertNotNull(repository.getMealById(mealId))

        repository.deleteMeal(mealId)

        // Meal should no longer exist
        assertNull(repository.getMealById(mealId))

        // Items should be cascaded (empty list returned for non-existent meal)
        val items = repository.getMealItemsWithFood(mealId)
        assertEquals(0, items.size)
    }

    @Test
    fun testMealWithMultipleItems() = runTest {
        val food1 = foodRepository.insertManual("Food1", 100.0, null, null, null, null, null)
        val food2 = foodRepository.insertManual("Food2", 200.0, null, null, null, null, null)
        val food3 = foodRepository.insertManual("Food3", 150.0, null, null, null, null, null)

        val mealId = intakeRepository.createMeal(
            "Complex Meal",
            4.0,
            listOf(food1 to 100.0, food2 to 200.0, food3 to 50.0)
        )

        val items = repository.getMealItemsWithFood(mealId)
        assertEquals(3, items.size)

        val (totalG, totalKcal) = intakeRepository.computeMealTotals(mealId)
        assertEquals(350.0, totalG)
        assertEquals(575.0, totalKcal, 0.01) // (100*1) + (200*2) + (150*0.5)
    }
}
