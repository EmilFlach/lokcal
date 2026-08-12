@file:OptIn(ExperimentalTime::class)

package com.emilflach.lokcal.health

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.data.AutomaticStepsDayState
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.SettingsRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class AutomaticStepsSyncManagerTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var settingsRepository: SettingsRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = Database(driver)
        exerciseRepository = ExerciseRepository(database)
        settingsRepository = SettingsRepository(database)
    }

    @AfterTest
    fun teardown() = driver.close()

    @Test
    fun amsterdamToSanFranciscoKeepsOneExtendedDay() = runTest {
        var now = instant("2025-07-12T08:00:00", "Europe/Amsterdam")
        var zone = TimeZone.of("Europe/Amsterdam")
        val windows = mutableListOf<Pair<Long, Long>>()
        val readSteps: suspend (Long, Long) -> Int? = { start, end ->
            windows += start to end
            if (windows.size == 1) 1_000 else 3_000
        }
        val manager = manager()

        assertTrue(manager.sync(now, zone, readSteps))
        val originalStart = windows.single().first

        zone = TimeZone.of("America/Los_Angeles")
        now = instant("2025-07-12T18:00:00", "America/Los_Angeles")
        assertTrue(manager.sync(now, zone, readSteps))
        assertEquals(originalStart, windows.last().first)

        now = instant("2025-07-13T00:05:00", "America/Los_Angeles")
        assertTrue(manager.sync(now, zone, readSteps))
        val rolloverWindow = windows[2]
        assertEquals(originalStart, rolloverWindow.first)
        assertEquals(33, (rolloverWindow.second - rolloverWindow.first) / 3_600_000)

        val july12 = exerciseRepository.getByDateRange("2025-07-12T00:00:00", "2025-07-12T23:59:59")
        assertEquals(1, july12.size)
        assertEquals(30.0, july12.single().duration_min)
        assertEquals("2025-07-13", settingsRepository.getAutomaticStepsDayState()?.dateIso)
    }

    @Test
    fun forwardTimezoneChangeStartsDestinationDate() = runTest {
        var now = instant("2025-07-12T08:00:00", "Europe/Amsterdam")
        var zone = TimeZone.of("Europe/Amsterdam")
        val windows = mutableListOf<Pair<Long, Long>>()
        val readSteps: suspend (Long, Long) -> Int? = { start, end ->
            windows += start to end
            2_000
        }
        val manager = manager()

        assertTrue(manager.sync(now, zone, readSteps))
        zone = TimeZone.of("Asia/Tokyo")
        now = instant("2025-07-13T12:00:00", "Asia/Tokyo")
        assertTrue(manager.sync(now, zone, readSteps))

        assertEquals(3, windows.size)
        val tokyoMidnight = instant("2025-07-13T00:00:00", "Asia/Tokyo").toEpochMilliseconds()
        assertEquals(tokyoMidnight, windows[1].second)
        assertEquals(tokyoMidnight, windows[2].first)
        assertEquals("2025-07-13", settingsRepository.getAutomaticStepsDayState()?.dateIso)
    }

    @Test
    fun failedReadDoesNotOverwriteExistingValue() = runTest {
        exerciseRepository.logAutomaticSteps("2025-07-12", 4_000)
        val manager = manager()

        assertEquals(
            false,
            manager.sync(
                now = instant("2025-07-12T12:00:00", "Europe/Amsterdam"),
                timeZone = TimeZone.of("Europe/Amsterdam"),
                readSteps = { _, _ -> null },
            )
        )
        val exercise = exerciseRepository
            .getByDateRange("2025-07-12T00:00:00", "2025-07-12T23:59:59")
            .single()
        assertEquals(40.0, exercise.duration_min)
        assertNotNull(settingsRepository.getAutomaticStepsDayState())
    }

    @Test
    fun rolloverUsesDstAwareLocalMidnights() = runTest {
        val amsterdam = TimeZone.of("Europe/Amsterdam")
        suspend fun assertDayLength(dateIso: String, nextDateIso: String, expectedHours: Long) {
            val start = kotlinx.datetime.LocalDate.parse(dateIso).atStartOfDayIn(amsterdam).toEpochMilliseconds()
            settingsRepository.setAutomaticStepsDayState(
                AutomaticStepsDayState(dateIso, start)
            )
            val windows = mutableListOf<Pair<Long, Long>>()
            val manager = manager()

            assertTrue(
                manager.sync(
                    now = instant("${nextDateIso}T00:05:00", amsterdam.id),
                    timeZone = amsterdam,
                    readSteps = { windowStart, windowEnd ->
                        windows += windowStart to windowEnd
                        0
                    },
                )
            )
            assertEquals(expectedHours, (windows.first().second - windows.first().first) / 3_600_000)
        }

        assertDayLength("2025-03-30", "2025-03-31", 23)
        assertDayLength("2025-10-26", "2025-10-27", 25)
    }

    @Test
    fun multiDayGapResetsWithoutChangingHistoricalRow() = runTest {
        val amsterdam = TimeZone.of("Europe/Amsterdam")
        val oldStart = instant("2025-07-12T00:00:00", amsterdam.id).toEpochMilliseconds()
        settingsRepository.setAutomaticStepsDayState(AutomaticStepsDayState("2025-07-12", oldStart))
        exerciseRepository.logAutomaticSteps("2025-07-12", 4_000)
        val windows = mutableListOf<Pair<Long, Long>>()

        assertTrue(
            manager().sync(
                now = instant("2025-07-15T12:00:00", amsterdam.id),
                timeZone = amsterdam,
                readSteps = { start, end ->
                    windows += start to end
                    1_000
                },
            )
        )

        assertEquals(1, windows.size)
        assertEquals(
            instant("2025-07-15T00:00:00", amsterdam.id).toEpochMilliseconds(),
            windows.single().first,
        )
        val oldExercise = exerciseRepository
            .getByDateRange("2025-07-12T00:00:00", "2025-07-12T23:59:59")
            .single()
        assertEquals(40.0, oldExercise.duration_min)
        assertEquals("2025-07-15", settingsRepository.getAutomaticStepsDayState()?.dateIso)
    }

    @Test
    fun failedRolloverDoesNotAdvanceDay() = runTest {
        val amsterdam = TimeZone.of("Europe/Amsterdam")
        val oldStart = instant("2025-07-12T00:00:00", amsterdam.id).toEpochMilliseconds()
        settingsRepository.setAutomaticStepsDayState(AutomaticStepsDayState("2025-07-12", oldStart))

        assertEquals(
            false,
            manager().sync(
                now = instant("2025-07-13T00:05:00", amsterdam.id),
                timeZone = amsterdam,
                readSteps = { _, _ -> null },
            ),
        )
        assertEquals("2025-07-12", settingsRepository.getAutomaticStepsDayState()?.dateIso)
    }

    @Test
    fun persistedStateRemainsBackwardCompatible() {
        val state = AutomaticStepsDayState.decode("1|2025-07-12|1752271200000|Europe/Amsterdam")
        assertEquals(state, AutomaticStepsDayState("2025-07-12", 1752271200000))
    }

    private fun manager() = AutomaticStepsSyncManager(
        exerciseRepository = exerciseRepository,
        settingsRepository = settingsRepository,
    )

    private fun instant(localDateTime: String, timeZone: String): Instant =
        LocalDateTime.parse(localDateTime).toInstant(TimeZone.of(timeZone))
}
