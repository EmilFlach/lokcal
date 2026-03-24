package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FoodRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var repository: FoodRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        repository = FoodRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testInsertAndGetById() = runTest {
        val foodId = repository.insertManual(
            name = "Apple",
            energyKcalPer100g = 52.0,
            servingSize = "100g",
            gtin13 = "1234567890123",
            imageUrl = "https://example.com/apple.jpg",
            productUrl = "https://example.com/apple",
            source = "manual"
        )

        // Add aliases
        repository.addAlias(foodId, "Fresh Farms", "brand")
        repository.addAlias(foodId, "Apple", "locale:en")
        repository.addAlias(foodId, "Appel", "locale:nl")

        val food = repository.getById(foodId)
        assertNotNull(food)
        assertEquals("Apple", food.name)
        assertEquals(52.0, food.energy_kcal_per_100g)
        assertEquals("https://example.com/apple", food.product_url)
        assertEquals("https://example.com/apple.jpg", food.image_url)
        assertEquals("1234567890123", food.gtin13)
        assertEquals("100g", food.serving_size)
        assertEquals("manual", food.source)

        // Check aliases
        val aliases = repository.getAliases(foodId)
        assertEquals(3, aliases.size)
    }

    @Test
    fun testInsertMinimalFood() = runTest {
        val foodId = repository.insertManual(
            name = "Banana",
            energyKcalPer100g = 89.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = null
        )

        val food = repository.getById(foodId)
        assertNotNull(food)
        assertEquals("Banana", food.name)
        assertEquals(89.0, food.energy_kcal_per_100g)
    }

    @Test
    fun testUpdateDetails() = runTest {
        val foodId = repository.insertManual(
            name = "Orange",
            energyKcalPer100g = 47.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = "manual"
        )

        repository.updateDetails(
            id = foodId,
            name = "Blood Orange",
            energyKcalPer100g = 50.0,
            productUrl = "https://example.com/orange",
            imageUrl = "https://example.com/orange.jpg",
            gtin13 = "9876543210987",
            servingSize = "150g",
            source = "ah"
        )

        // Add aliases after update
        repository.addAlias(foodId, "Premium", "brand")
        repository.addAlias(foodId, "Blood Orange", "locale:en")
        repository.addAlias(foodId, "Bloedsinaasappel", "locale:nl")

        val updated = repository.getById(foodId)
        assertNotNull(updated)
        assertEquals("Blood Orange", updated.name)
        assertEquals(50.0, updated.energy_kcal_per_100g)
        assertEquals("ah", updated.source)

        val aliases = repository.getAliases(foodId)
        assertEquals(3, aliases.size)
    }

    @Test
    fun testDelete() = runTest {
        val foodId = repository.insertManual(
            name = "Temporary",
            energyKcalPer100g = 100.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = null
        )

        assertNotNull(repository.getById(foodId))

        repository.delete(foodId)

        assertNull(repository.getById(foodId))
    }

    @Test
    fun testGetAll() = runTest {
        repository.insertManual("Food1", 100.0, null, null, null, null, null)
        repository.insertManual("Food2", 200.0, null, null, null, null, null)
        repository.insertManual("Food3", 300.0, null, null, null, null, null)

        val all = repository.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun testSearchByName() = runTest {
        repository.insertManual("Apple Pie", 237.0, null, null, null, null, null)
        repository.insertManual("Banana Split", 150.0, null, null, null, null, null)
        repository.insertManual("Cherry Tart", 280.0, null, null, null, null, null)

        val results = repository.search("apple")
        assertEquals(1, results.size)
        assertEquals("Apple Pie", results[0].name)
    }


    @Test
    fun testSearchByEnglishName() = runTest {
        val foodId = repository.insertManual(
            name = "Tomaat",
            energyKcalPer100g = 18.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = null
        )
        repository.addAlias(foodId, "Tomato", "locale:en")

        val results = repository.search("tomato")
        assertEquals(1, results.size)
        assertEquals("Tomaat", results[0].name)
    }

    @Test
    fun testSearchByDutchName() = runTest {
        val foodId = repository.insertManual(
            name = "Tomato",
            energyKcalPer100g = 18.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = null
        )
        repository.addAlias(foodId, "Tomaat", "locale:nl")

        val results = repository.search("tomaat")
        assertEquals(1, results.size)
        assertEquals("Tomato", results[0].name)
    }

    @Test
    fun testSearchByBrand() = runTest {
        val foodId = repository.insertManual(
            name = "Cola",
            energyKcalPer100g = 42.0,
            servingSize = null,
            gtin13 = null,
            imageUrl = null,
            productUrl = null,
            source = null
        )
        repository.addAlias(foodId, "Coca-Cola", "brand")

        val results = repository.search("coca-cola")
        assertEquals(1, results.size)
        assertEquals("Cola", results[0].name)
    }

    @Test
    fun testSearchByGtin13Barcode() = runTest {
        repository.insertManual(
            name = "Product",
            energyKcalPer100g = 100.0,
            servingSize = null,
            gtin13 = "5449000000996",
            imageUrl = null,
            productUrl = null,
            source = null
        )

        val results = repository.search("5449000000996")
        assertEquals(1, results.size)
        assertEquals("Product", results[0].name)
    }

    @Test
    fun testSearchMultiWord() = runTest {
        repository.insertManual("Apple Juice Organic", 46.0, null, null, null, null, null)
        repository.insertManual("Organic Apple Sauce", 68.0, null, null, null, null, null)
        repository.insertManual("Apple Pie", 237.0, null, null, null, null, null)

        val results = repository.search("organic apple")
        assertTrue(results.size >= 2)
        assertTrue(results.any { it.name == "Apple Juice Organic" })
        assertTrue(results.any { it.name == "Organic Apple Sauce" })
    }

    @Test
    fun testSearchCaseInsensitive() = runTest {
        repository.insertManual("UPPERCASE", 100.0, null, null, null, null, null)
        repository.insertManual("lowercase", 100.0, null, null, null, null, null)
        repository.insertManual("MixedCase", 100.0, null, null, null, null, null)

        val uppercase = repository.search("UPPERCASE")
        val lowercase = repository.search("lowercase")
        val mixedcase = repository.search("mixedCase")

        assertEquals(1, uppercase.size)
        assertEquals(1, lowercase.size)
        assertEquals(1, mixedcase.size)
    }

    @Test
    fun testSearchEmpty() = runTest {
        val results = repository.search("")
        assertEquals(0, results.size)
    }

    @Test
    fun testSearchNoMatch() = runTest {
        repository.insertManual("Food", 100.0, null, null, null, null, null)
        val results = repository.search("xyz123notfound")
        assertTrue(results.isEmpty() || results.size == 1)
    }

    @Test
    fun testSearchFuzzyMatch() = runTest {
        repository.insertManual("Apple", 52.0, null, null, null, null, null)
        val results = repository.search("aple")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name == "Apple" })
    }

    @Test
    fun testSourcePriority() = runTest {
        val id1 = repository.insertManual("Test Food", 100.0, null, null, null, null, "nevo")
        val id2 = repository.insertManual("Test Food", 100.0, null, null, null, null, "manual")

        val results = repository.search("Test Food")
        assertEquals(2, results.size)
        assertEquals("manual", results[0].source)
        assertEquals("nevo", results[1].source)
    }

    @Test
    fun testTrackingCountPriority() = runTest {
        val id1 = repository.insertManual("Rarely Eaten", 100.0, null, null, null, null, null)
        val id2 = repository.insertManual("Often Eaten", 100.0, null, null, null, null, null)

        val trackingCounts = mapOf(
            id2 to 10L,
            id1 to 1L
        )

        val results = repository.search("eaten", trackingCounts)
        assertEquals(2, results.size)
        assertEquals("Often Eaten", results[0].name)
        assertEquals("Rarely Eaten", results[1].name)
    }

    @Test
    fun testTrackingCountOverPrefix() = runTest {
        val id1 = repository.insertManual("Apple Fresh", 52.0, null, null, null, null, null)
        val id2 = repository.insertManual("Pineapple", 50.0, null, null, null, null, null)

        val trackingCounts = mapOf(
            id2 to 100L, // Highly tracked, but only "contains" match for "apple"
            id1 to 1L    // Rarely tracked, but "prefix" match for "apple"
        )

        val results = repository.search("apple", trackingCounts)
        
        // After change: Highly tracked "Pineapple" should win over "Apple Fresh"
        assertEquals("Pineapple", results[0].name)
        assertEquals("Apple Fresh", results[1].name)
    }
}
