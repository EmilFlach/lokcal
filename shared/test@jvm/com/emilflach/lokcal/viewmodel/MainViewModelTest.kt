package com.emilflach.lokcal.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private var today = "2026-09-08"

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
    }

    @AfterTest
    fun teardown() = driver.close()

    private fun TestScope.viewModel() = MainViewModel(
        IntakeRepository(database), ExerciseRepository(database),
        WeightRepository(database), SettingsRepository(database), today,
        dateProvider = { today }, viewModelScope = backgroundScope
    )

    @Test
    fun resumeAfterMidnightFollowsTodayAndReloadsSummary() = runTest {
        database.exerciseQueries.logExercise("2026-09-08T12:00:00", "Walking", 30.0, 100.0, null)
        val vm = viewModel()
        runCurrent()
        assertEquals(100.0, vm.uiState.value.dayState.burnedKcal)

        today = "2026-09-09"
        vm.refresh()
        runCurrent()

        assertEquals(today, vm.getSelectedDateIso())
        assertEquals("Today, 9 Sep", vm.formattedDate())
        assertEquals(0.0, vm.uiState.value.dayState.burnedKcal)
        assertEquals(LocalDate.parse("2026-09-08"), vm.uiState.value.last7Deltas.last().date)
        assertEquals(LocalDate.parse(today), vm.getDateForPage(vm.getPageForDate(LocalDate.parse(today))))
    }

    @Test
    fun midnightKeepsHistoricalSelectionAndPageDatesStable() = runTest {
        val vm = viewModel()
        val yesterday = LocalDate.parse("2026-09-07")
        vm.loadFor(yesterday)
        runCurrent()
        val page = vm.getPageForDate(yesterday)

        today = "2026-09-09"
        vm.refreshCurrentDate()
        runCurrent()
        vm.onPageSelected(page)
        runCurrent()

        assertEquals(yesterday, vm.uiState.value.selectedDate)
        assertEquals(yesterday, vm.getDateForPage(page))
        assertEquals(page, vm.getPageForDate(yesterday))
        assertEquals("Mon, 7 Sep", vm.formattedDate())
    }

    @Test
    fun visibleScreenFollowsMultipleMissedDaysAndClockMovingBack() = runTest {
        val vm = viewModel()
        runCurrent()
        for (date in listOf("2026-09-12", "2026-09-11")) {
            today = date
            vm.refreshCurrentDate()
            runCurrent()
            assertEquals(today, vm.getSelectedDateIso())
        }
    }

    @Test
    fun repeatedRefreshBeforeLoadCompletesKeepsNewToday() = runTest {
        val vm = viewModel()
        runCurrent()
        today = "2026-09-09"
        vm.refreshCurrentDate()
        vm.refresh()
        runCurrent()
        assertEquals(today, vm.getSelectedDateIso())
    }

    @Test
    fun unchangedDatePollingDoesNotReloadButExplicitRefreshDoes() = runTest {
        val vm = viewModel()
        runCurrent()
        val originalBudget = vm.uiState.value.dayState.startingKcal
        SettingsRepository(database).setStartingKcal(originalBudget + 100.0)

        vm.refreshCurrentDate()
        runCurrent()
        assertEquals(originalBudget, vm.uiState.value.dayState.startingKcal)

        vm.refresh()
        runCurrent()
        assertEquals(originalBudget + 100.0, vm.uiState.value.dayState.startingKcal)
    }

}
