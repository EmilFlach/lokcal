package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.ui.util.EntityImageData
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ImageCacheRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var foodRepo: FoodRepository
    private lateinit var repo: ImageCacheRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        // Create ImageCache table (V7 migration — not in SQLDelight schema for existing DBs)
        driver.execute(
            null,
            """
            CREATE TABLE IF NOT EXISTS ImageCache (
                entity_type TEXT NOT NULL,
                entity_id   INTEGER NOT NULL,
                image_data  BLOB NOT NULL,
                mime_type   TEXT NOT NULL DEFAULT 'image/jpeg',
                cached_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                byte_size   INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (entity_type, entity_id)
            )
            """.trimIndent(),
            0
        )
        // Create triggers for cascade delete
        driver.execute(
            null,
            """
            CREATE TRIGGER IF NOT EXISTS delete_food_image_on_food_delete
            AFTER DELETE ON Food BEGIN
                DELETE FROM ImageCache WHERE entity_type = 'FOOD' AND entity_id = OLD.id;
            END
            """.trimIndent(),
            0
        )
        driver.execute(
            null,
            """
            CREATE TRIGGER IF NOT EXISTS delete_meal_image_on_meal_delete
            AFTER DELETE ON Meal BEGIN
                DELETE FROM ImageCache WHERE entity_type = 'MEAL' AND entity_id = OLD.id;
            END
            """.trimIndent(),
            0
        )
        database = Database(driver)
        foodRepo = FoodRepository(database)
        repo = ImageCacheRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    private suspend fun insertFood(name: String = "Test Food"): Long =
        foodRepo.insertManual(
            name = name,
            energyKcalPer100g = 100.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = "manual"
        )

    private suspend fun insertMeal(name: String = "Test Meal"): Long {
        database.mealsQueries.mealInsertWithPortions(name, null, 1.0)
        return database.mealsQueries.lastInsertId().awaitAsOneOrNull() ?: error("Meal insert failed")
    }

    @Test
    fun `getImage returns null when no entry exists`() = runTest {
        assertNull(repo.getImage(EntityImageData.FOOD, 999L))
        assertNull(repo.getImage(EntityImageData.MEAL, 999L))
    }

    @Test
    fun `saveImage and getImage round-trip for FOOD`() = runTest {
        val foodId = insertFood()
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        repo.saveImage(EntityImageData.FOOD, foodId, bytes, "image/jpeg")
        val result = repo.getImage(EntityImageData.FOOD, foodId)
        assertNotNull(result)
        assertEquals(bytes.toList(), result.first.toList())
        assertEquals("image/jpeg", result.second)
    }

    @Test
    fun `saveImage and getImage round-trip for MEAL`() = runTest {
        val mealId = insertMeal()
        val bytes = byteArrayOf(10, 20, 30)
        repo.saveImage(EntityImageData.MEAL, mealId, bytes, "image/png")
        val result = repo.getImage(EntityImageData.MEAL, mealId)
        assertNotNull(result)
        assertEquals(bytes.toList(), result.first.toList())
        assertEquals("image/png", result.second)
    }

    @Test
    fun `FOOD and MEAL entities with same id are independent`() = runTest {
        val foodId = insertFood()
        val mealId = insertMeal()
        // Use the same numeric id if possible, otherwise just test independence
        val foodBytes = byteArrayOf(1, 2)
        val mealBytes = byteArrayOf(3, 4)
        repo.saveImage(EntityImageData.FOOD, foodId, foodBytes, "image/jpeg")
        repo.saveImage(EntityImageData.MEAL, mealId, mealBytes, "image/jpeg")

        val foodResult = repo.getImage(EntityImageData.FOOD, foodId)
        val mealResult = repo.getImage(EntityImageData.MEAL, mealId)
        assertNotNull(foodResult)
        assertNotNull(mealResult)
        assertEquals(foodBytes.toList(), foodResult.first.toList())
        assertEquals(mealBytes.toList(), mealResult.first.toList())
    }

    @Test
    fun `saveImage overwrites existing entry`() = runTest {
        val foodId = insertFood()
        repo.saveImage(EntityImageData.FOOD, foodId, byteArrayOf(1, 2), "image/jpeg")
        repo.saveImage(EntityImageData.FOOD, foodId, byteArrayOf(9, 8, 7), "image/png")
        val result = repo.getImage(EntityImageData.FOOD, foodId)
        assertNotNull(result)
        assertEquals(3, result.first.size)
        assertEquals("image/png", result.second)
    }

    @Test
    fun `deleteImage removes entry`() = runTest {
        val foodId = insertFood()
        repo.saveImage(EntityImageData.FOOD, foodId, byteArrayOf(1, 2, 3), "image/jpeg")
        repo.deleteImage(EntityImageData.FOOD, foodId)
        assertNull(repo.getImage(EntityImageData.FOOD, foodId))
    }

    @Test
    fun `getTotalCacheSizeBytes sums byte_size correctly`() = runTest {
        val id1 = insertFood("Food 1")
        val id2 = insertMeal("Meal 1")
        repo.saveImage(EntityImageData.FOOD, id1, ByteArray(100), "image/jpeg")
        repo.saveImage(EntityImageData.MEAL, id2, ByteArray(200), "image/jpeg")
        assertEquals(300L, repo.getTotalCacheSizeBytes())
    }

    @Test
    fun `getTotalCacheSizeBytes returns 0 when empty`() = runTest {
        assertEquals(0L, repo.getTotalCacheSizeBytes())
    }

    @Test
    fun `evictIfNeeded does nothing when under limit`() = runTest {
        val id1 = insertFood("Food 1")
        repo.saveImage(EntityImageData.FOOD, id1, ByteArray(50), "image/jpeg")
        repo.evictIfNeeded(maxBytes = 100L)
        assertNotNull(repo.getImage(EntityImageData.FOOD, id1))
        assertEquals(50L, repo.getTotalCacheSizeBytes())
    }

    @Test
    fun `evictIfNeeded removes oldest entries when over limit`() = runTest {
        val id1 = insertFood("Food 1")
        val id2 = insertFood("Food 2")
        repo.saveImage(EntityImageData.FOOD, id1, ByteArray(60), "image/jpeg")
        repo.saveImage(EntityImageData.FOOD, id2, ByteArray(60), "image/jpeg")
        repo.evictIfNeeded(maxBytes = 80L)
        val remaining = listOf(
            repo.getImage(EntityImageData.FOOD, id1),
            repo.getImage(EntityImageData.FOOD, id2)
        ).count { it != null }
        assertEquals(1, remaining)
    }

    @Test
    fun `image is deleted when Food row is deleted (trigger cascade)`() = runTest {
        val foodId = insertFood()
        repo.saveImage(EntityImageData.FOOD, foodId, byteArrayOf(1, 2, 3), "image/jpeg")
        assertNotNull(repo.getImage(EntityImageData.FOOD, foodId))
        foodRepo.delete(foodId)
        assertNull(repo.getImage(EntityImageData.FOOD, foodId))
    }

    @Test
    fun `image is deleted when Meal row is deleted (trigger cascade)`() = runTest {
        val mealId = insertMeal()
        repo.saveImage(EntityImageData.MEAL, mealId, byteArrayOf(1, 2, 3), "image/jpeg")
        assertNotNull(repo.getImage(EntityImageData.MEAL, mealId))
        database.mealsQueries.deleteMealById(mealId)
        assertNull(repo.getImage(EntityImageData.MEAL, mealId))
    }

    @Test
    fun `getCachedIdsByType returns only matching type`() = runTest {
        val foodId1 = insertFood("Food 1")
        val foodId2 = insertFood("Food 2")
        val mealId1 = insertMeal("Meal 1")
        insertFood("Food 3") // no image cached
        repo.saveImage(EntityImageData.FOOD, foodId1, byteArrayOf(1), "image/jpeg")
        repo.saveImage(EntityImageData.FOOD, foodId2, byteArrayOf(2), "image/jpeg")
        repo.saveImage(EntityImageData.MEAL, mealId1, byteArrayOf(3), "image/jpeg")

        val foodIds = repo.getCachedIdsByType(EntityImageData.FOOD)
        assertEquals(setOf(foodId1, foodId2), foodIds)

        val mealIds = repo.getCachedIdsByType(EntityImageData.MEAL)
        assertEquals(setOf(mealId1), mealIds)
    }

    @Test
    fun `getCachedIdsByType returns empty set when nothing cached`() = runTest {
        insertFood("Food 1")
        insertMeal("Meal 1")
        assertTrue(repo.getCachedIdsByType(EntityImageData.FOOD).isEmpty())
        assertTrue(repo.getCachedIdsByType(EntityImageData.MEAL).isEmpty())
    }
}
