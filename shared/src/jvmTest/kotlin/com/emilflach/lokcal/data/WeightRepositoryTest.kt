package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class WeightRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var repository: WeightRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        repository = WeightRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testSetForToday() = runTest {
        repository.setForToday(75.5)

        val all = repository.getAll()
        assertEquals(1, all.size)
        assertEquals(75.5, all[0].weight_kg)
    }

    @Test
    fun testSetForTodayUpdatesExisting() = runTest {
        repository.setForToday(75.0)
        repository.setForToday(75.5)

        val all = repository.getAll()
        assertEquals(1, all.size) // Should update, not create duplicate
        assertEquals(75.5, all[0].weight_kg)
    }

    @Test
    fun testGetAll() = runTest {
        // Manually insert different dates by directly using database
        database.weightQueries.insertOrReplace("2024-01-15", 75.0)
        database.weightQueries.insertOrReplace("2024-01-16", 75.5)
        database.weightQueries.insertOrReplace("2024-01-17", 76.0)

        val all = repository.getAll()
        assertEquals(3, all.size)
        // Should be descending order by date
        assertEquals("2024-01-17", all[0].date)
        assertEquals("2024-01-16", all[1].date)
        assertEquals("2024-01-15", all[2].date)
    }

    @Test
    fun testGetForDate() = runTest {
        database.weightQueries.insertOrReplace("2024-01-15", 75.0)

        val weight = repository.getForDate("2024-01-15")
        assertNotNull(weight)
        assertEquals(75.0, weight.weight_kg)
    }

    @Test
    fun testGetForDateNotFound() = runTest {
        val weight = repository.getForDate("2024-01-15")
        assertNull(weight)
    }

    @Test
    fun testDeleteById() = runTest {
        database.weightQueries.insertOrReplace("2024-01-15", 75.0)
        val all = repository.getAll()
        val id = all[0].id

        repository.deleteById(id)

        val afterDelete = repository.getAll()
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testMultipleWeightEntries() = runTest {
        database.weightQueries.insertOrReplace("2024-01-15", 75.0)
        database.weightQueries.insertOrReplace("2024-01-16", 75.2)
        database.weightQueries.insertOrReplace("2024-01-17", 74.8)
        database.weightQueries.insertOrReplace("2024-01-18", 75.5)

        val all = repository.getAll()
        assertEquals(4, all.size)
    }

    @Test
    fun testUniqueConstraintOnDate() = runTest {
        database.weightQueries.insertOrReplace("2024-01-15", 75.0)
        database.weightQueries.insertOrReplace("2024-01-15", 76.0) // Should replace

        val all = repository.getAll()
        assertEquals(1, all.size)
        assertEquals(76.0, all[0].weight_kg)
    }

    @Test
    fun testWeightValidation() = runTest {
        // Weight must be positive (constraint in schema)
        assertFails {
            database.weightQueries.insertOrReplace("2024-01-15", 0.0)
        }

        assertFails {
            database.weightQueries.insertOrReplace("2024-01-15", -5.0)
        }
    }
}
