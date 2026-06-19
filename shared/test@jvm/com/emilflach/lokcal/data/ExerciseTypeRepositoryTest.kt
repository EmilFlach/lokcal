package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ExerciseTypeRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var repository: ExerciseTypeRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        repository = ExerciseTypeRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testInsertAndGetAll() = runTest {
        repository.insert("Cycling", 450.0, sortOrder = 0)
        repository.insert("Swimming", 600.0, sortOrder = 1)

        val types = repository.getAll()
        assertEquals(2, types.size)
        assertEquals("Cycling", types[0].name)
        assertEquals(450.0, types[0].kcal_per_hour)
        assertEquals("Swimming", types[1].name)
        assertEquals(600.0, types[1].kcal_per_hour)
    }

    @Test
    fun testUpdate() = runTest {
        repository.insert("Cycling", 450.0)

        val inserted = repository.getAll().first()
        repository.update(inserted.id, "Road Cycling", 500.0)

        val updated = repository.getById(inserted.id)
        assertNotNull(updated)
        assertEquals("Road Cycling", updated.name)
        assertEquals(500.0, updated.kcal_per_hour)
    }

    @Test
    fun testDelete() = runTest {
        repository.insert("Cycling", 450.0)

        val inserted = repository.getAll().first()
        repository.delete(inserted.id)

        val types = repository.getAll()
        assertEquals(0, types.size)
    }

    @Test
    fun testGetById() = runTest {
        repository.insert("Yoga", 250.0, sortOrder = 2)

        val all = repository.getAll()
        val id = all.first().id

        val found = repository.getById(id)
        assertNotNull(found)
        assertEquals("Yoga", found.name)
        assertEquals(250.0, found.kcal_per_hour)
        assertEquals(2L, found.sort_order)
    }

    @Test
    fun testSortOrder() = runTest {
        repository.insert("Last", 100.0, sortOrder = 2)
        repository.insert("First", 200.0, sortOrder = 0)
        repository.insert("Middle", 150.0, sortOrder = 1)

        val types = repository.getAll()
        assertEquals(3, types.size)
        assertEquals("First", types[0].name)
        assertEquals("Middle", types[1].name)
        assertEquals("Last", types[2].name)
    }

    @Test
    fun testImageUrl() = runTest {
        repository.insert("Cycling", 450.0, imageUrl = "https://example.com/cycling.jpg")

        val types = repository.getAll()
        assertEquals(1, types.size)
        assertEquals("https://example.com/cycling.jpg", types[0].image_url)
    }

    @Test
    fun testImageUrlNullByDefault() = runTest {
        repository.insert("Walking", 200.0)

        val types = repository.getAll()
        assertEquals(1, types.size)
        assertNull(types[0].image_url)
    }

    @Test
    fun testUpdateImageUrl() = runTest {
        repository.insert("Cycling", 450.0, imageUrl = "https://example.com/old.jpg")

        val inserted = repository.getAll().first()
        repository.update(inserted.id, "Cycling", 450.0, imageUrl = "https://example.com/new.jpg")

        val updated = repository.getById(inserted.id)
        assertNotNull(updated)
        assertEquals("https://example.com/new.jpg", updated.image_url)
    }

    @Test
    fun testGetByIdReturnsNullForMissing() = runTest {
        val result = repository.getById(9999L)
        assertNull(result)
    }
}
