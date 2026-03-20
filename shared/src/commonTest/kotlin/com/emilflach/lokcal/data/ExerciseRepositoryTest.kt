package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ExerciseRepositoryTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var repository: ExerciseRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        repository = ExerciseRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun testLogWalkingExercise() = runTest {
        repository.logExercise(
            type = ExerciseRepository.Type.WALKING,
            minutes = 30.0,
            timestamp = "2024-01-15T10:00:00",
            notes = "Morning walk"
        )

        val exercises = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, exercises.size)
        assertEquals("WALKING", exercises[0].exercise_type)
        assertEquals(30.0, exercises[0].duration_min)
        assertEquals(100.0, exercises[0].energy_kcal_total, 0.01) // 200 kcal/hr * 0.5 hr
        assertEquals("Morning walk", exercises[0].notes)
    }

    @Test
    fun testLogRunningExercise() = runTest {
        repository.logExercise(
            type = ExerciseRepository.Type.RUNNING,
            minutes = 20.0,
            timestamp = "2024-01-15T18:00:00",
            notes = null
        )

        val exercises = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, exercises.size)
        assertEquals("RUNNING", exercises[0].exercise_type)
        assertEquals(20.0, exercises[0].duration_min)
        assertEquals(246.67, exercises[0].energy_kcal_total, 0.1) // 740 kcal/hr * (20/60) hr
    }

    @Test
    fun testLogAutomaticSteps() = runTest {
        repository.logAutomaticSteps(10000)

        val exercises = repository.getByDateRange("2020-01-01T00:00:00", "2030-12-31T23:59:59")
        assertEquals(1, exercises.size)
        assertEquals("AUTOMATIC_STEPS", exercises[0].exercise_type)
        assertEquals(100.0, exercises[0].duration_min) // 10000 steps / 100
    }

    @Test
    fun testLogAutomaticStepsUpdatesExisting() = runTest {
        repository.logAutomaticSteps(5000)
        repository.logAutomaticSteps(8000)

        val exercises = repository.getByDateRange("2020-01-01T00:00:00", "2030-12-31T23:59:59")
        assertEquals(1, exercises.size) // Should update, not create new
        assertEquals(80.0, exercises[0].duration_min) // Latest: 8000 / 100
    }

    @Test
    fun testUpdateExercise() = runTest {
        repository.logExercise(
            type = ExerciseRepository.Type.WALKING,
            minutes = 30.0,
            timestamp = "2024-01-15T10:00:00",
            notes = "Original"
        )

        val exercises = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        val exerciseId = exercises[0].id

        repository.updateExercise(
            id = exerciseId,
            type = ExerciseRepository.Type.RUNNING,
            minutes = 45.0,
            notes = "Updated"
        )

        val updated = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, updated.size)
        assertEquals("RUNNING", updated[0].exercise_type)
        assertEquals(45.0, updated[0].duration_min)
        assertEquals("Updated", updated[0].notes)
    }

    @Test
    fun testDeleteExercise() = runTest {
        repository.logExercise(
            type = ExerciseRepository.Type.WALKING,
            minutes = 30.0,
            timestamp = "2024-01-15T10:00:00"
        )

        val exercises = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, exercises.size)

        repository.deleteById(exercises[0].id)

        val afterDelete = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(0, afterDelete.size)
    }

    @Test
    fun testGetByDateRange() = runTest {
        repository.logExercise(ExerciseRepository.Type.WALKING, 30.0, "2024-01-15T10:00:00")
        repository.logExercise(ExerciseRepository.Type.RUNNING, 20.0, "2024-01-16T10:00:00")
        repository.logExercise(ExerciseRepository.Type.WALKING, 25.0, "2024-01-17T10:00:00")

        val jan15 = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(1, jan15.size)

        val jan15to16 = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-16T23:59:59")
        assertEquals(2, jan15to16.size)

        val all = repository.getByDateRange("2024-01-01T00:00:00", "2024-01-31T23:59:59")
        assertEquals(3, all.size)
    }

    @Test
    fun testSumKcalByDate() = runTest {
        repository.logExercise(ExerciseRepository.Type.WALKING, 30.0, "2024-01-15T10:00:00") // 100 kcal
        repository.logExercise(ExerciseRepository.Type.WALKING, 30.0, "2024-01-15T14:00:00") // 100 kcal
        repository.logExercise(ExerciseRepository.Type.RUNNING, 20.0, "2024-01-15T18:00:00") // ~247 kcal

        val total = repository.sumKcalByDate("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(446.67, total, 1.0) // Sum of all exercises
    }

    @Test
    fun testGetDailyBurned() = runTest {
        repository.logExercise(ExerciseRepository.Type.WALKING, 30.0, "2024-01-15T10:00:00")
        repository.logExercise(ExerciseRepository.Type.RUNNING, 20.0, "2024-01-16T10:00:00")

        val daily = repository.getDailyBurned("2024-01-15T00:00:00", "2024-01-16T23:59:59")
        assertEquals(2, daily.size)
        assertEquals("2024-01-15", daily[0].day)
        assertEquals("2024-01-16", daily[1].day)
    }

    @Test
    fun testExerciseTypeEnum() {
        assertEquals("WALKING", ExerciseRepository.Type.WALKING.dbName)
        assertEquals("RUNNING", ExerciseRepository.Type.RUNNING.dbName)
        assertEquals("AUTOMATIC_STEPS", ExerciseRepository.Type.AUTOMATIC_STEPS.dbName)

        assertEquals(200.0, ExerciseRepository.Type.WALKING.kcalPerHour)
        assertEquals(740.0, ExerciseRepository.Type.RUNNING.kcalPerHour)
        assertEquals(220.0, ExerciseRepository.Type.AUTOMATIC_STEPS.kcalPerHour)
    }

    @Test
    fun testExerciseTypeFromDb() {
        assertEquals(ExerciseRepository.Type.WALKING, ExerciseRepository.Type.fromDb("WALKING"))
        assertEquals(ExerciseRepository.Type.RUNNING, ExerciseRepository.Type.fromDb("RUNNING"))
        assertEquals(ExerciseRepository.Type.AUTOMATIC_STEPS, ExerciseRepository.Type.fromDb("AUTOMATIC_STEPS"))
    }

    @Test
    fun testMultipleExercisesOnSameDay() = runTest {
        repository.logExercise(ExerciseRepository.Type.WALKING, 30.0, "2024-01-15T08:00:00")
        repository.logExercise(ExerciseRepository.Type.WALKING, 20.0, "2024-01-15T12:00:00")
        repository.logExercise(ExerciseRepository.Type.RUNNING, 15.0, "2024-01-15T18:00:00")

        val exercises = repository.getByDateRange("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertEquals(3, exercises.size)

        val totalKcal = repository.sumKcalByDate("2024-01-15T00:00:00", "2024-01-15T23:59:59")
        assertTrue(totalKcal > 0)
    }
}
