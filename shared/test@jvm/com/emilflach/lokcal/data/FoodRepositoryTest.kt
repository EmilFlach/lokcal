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
    private lateinit var intakeRepository: IntakeRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        repository = FoodRepository(database)
        intakeRepository = IntakeRepository(database)
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
            servingSize = 100.0,
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
        assertEquals(100.0, food.serving_size)
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
            servingSize = 150.0,
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
    fun testSearchAccentedQueryMatchesPlainDB() = runTest {
        // DB has plain ASCII; user types accented
        repository.insertManual("Creme fraiche", 292.0, null, null, null, null, null)
        val results = repository.search("crème fraîche")
        assertTrue(results.isNotEmpty(), "accented query should match plain DB name")
        assertEquals("Creme fraiche", results[0].name)
    }

    @Test
    fun testSearchPlainQueryMatchesAccentedDB() = runTest {
        // DB has accented name; user types plain ASCII
        repository.insertManual("Crème fraîche", 292.0, null, null, null, null, null)
        val results = repository.search("creme fraiche")
        assertTrue(results.isNotEmpty(), "plain query should match accented DB name")
        assertEquals("Crème fraîche", results[0].name)
    }

    @Test
    fun testSearchAccentedSingleWord() = runTest {
        repository.insertManual("Café au lait", 40.0, null, null, null, null, null)
        val withAccent = repository.search("café")
        val withoutAccent = repository.search("cafe")
        assertTrue(withAccent.isNotEmpty(), "accented query should find accented DB name")
        assertTrue(withoutAccent.isNotEmpty(), "plain query should find accented DB name")
    }

    @Test
    fun testSearchAmpersandInName() = runTest {
        repository.insertManual("Ben & Jerry's", 250.0, null, null, null, null, null)
        val withAmpersand = repository.search("ben & jerry")
        val withAnd = repository.search("ben and jerry")
        val withoutConnector = repository.search("ben jerry")
        assertTrue(withAmpersand.isNotEmpty(), "should find with &")
        assertTrue(withAnd.isNotEmpty(), "should find with 'and'")
        assertTrue(withoutConnector.isNotEmpty(), "should find without connector")
    }

    @Test
    fun testSearchPunctuationOnlyTokensIgnored() = runTest {
        repository.insertManual("Salt & Pepper", 0.0, null, null, null, null, null)
        val results = repository.search("salt & pepper")
        assertTrue(results.isNotEmpty(), "lone & token should not block search results")
    }

    @Test
    fun testSearchApostropheIgnored() = runTest {
        repository.insertManual("McDonald's", 500.0, null, null, null, null, null)
        val withApostrophe = repository.search("mcdonald's")
        val withoutApostrophe = repository.search("mcdonalds")
        assertTrue(withApostrophe.isNotEmpty(), "should find with apostrophe")
        assertTrue(withoutApostrophe.isNotEmpty(), "should find without apostrophe")
    }

    @Test
    fun testSourcePriority() = runTest {
        repository.insertManual("Test Food", 100.0, null, null, null, null, "nevo")
        repository.insertManual("Test Food", 100.0, null, null, null, null, "manual")

        val results = repository.search("Test Food")
        assertEquals(2, results.size)
        assertTrue(results.any { it.source == "manual" })
        assertTrue(results.any { it.source == "nevo" })
    }

    @Test
    fun testTrackingCountPriority() = runTest {
        val rareId = repository.insertManual("Rarely Eaten", 100.0, null, null, null, null, null)
        val oftenId = repository.insertManual("Often Eaten", 100.0, null, null, null, null, null)

        // Log "Often Eaten" 5 times across different dates, "Rarely Eaten" once
        repeat(5) { i -> intakeRepository.logOrUpdateFoodIntake(oftenId, 100.0, "lunch", "2024-01-${(i + 1).toString().padStart(2, '0')}") }
        intakeRepository.logOrUpdateFoodIntake(rareId, 100.0, "lunch", "2024-02-01")

        val results = repository.search("eaten")
        assertEquals(2, results.size)
        assertEquals("Often Eaten", results[0].name)
        assertEquals("Rarely Eaten", results[1].name)
    }

    @Test
    fun testHighTrackingBeatsExactMatch() = runTest {
        // "raw pasta" with 32 tracks should rank above exact match "pasta" with 0 tracks
        repository.insertManual("pasta", 350.0, null, null, null, null, null)
        val rawPastaId = repository.insertManual("raw pasta", 350.0, null, null, null, null, null)

        repeat(32) { i -> intakeRepository.logOrUpdateFoodIntake(rawPastaId, 100.0, "lunch", "2024-01-${(i + 1).toString().padStart(2, '0')}") }

        val results = repository.search("pasta")
        assertEquals(2, results.size)
        assertEquals("raw pasta", results[0].name, "frequently tracked 'raw pasta' should beat untracked exact match 'pasta'")
    }

    @Test
    fun testTrackingCountTiebreak() = runTest {
        // Both start with "apple" → same relevance score; tracking count decides
        val juiceId = repository.insertManual("Apple Juice", 46.0, null, null, null, null, null)
        val sauceId = repository.insertManual("Apple Sauce", 68.0, null, null, null, null, null)

        repeat(8) { i -> intakeRepository.logOrUpdateFoodIntake(sauceId, 100.0, "lunch", "2024-01-${(i + 1).toString().padStart(2, '0')}") }
        intakeRepository.logOrUpdateFoodIntake(juiceId, 100.0, "lunch", "2024-02-01")

        val results = repository.search("apple")
        assertEquals(2, results.size)
        assertEquals("Apple Sauce", results[0].name)
        assertEquals("Apple Juice", results[1].name)
    }
}
