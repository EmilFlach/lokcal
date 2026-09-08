package com.emilflach.lokcal.viewmodel

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.*
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.data.*
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.ui.screens.MainScreen
import kotlinx.coroutines.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class MainScreenRolloverTest {
    @Test
    fun enteringHomeAfterMidnightFollowsToday() = checkHomeRollover(initiallyVisible = false)

    @Test
    fun refocusingExistingHomeAfterMidnightFollowsToday() = checkHomeRollover(initiallyVisible = true)

    @Test
    fun historicalSelectionSurvivesReentryAfterMidnight() =
        checkHomeRollover(initiallyVisible = false, historicalDate = "2026-09-06")

    private fun checkHomeRollover(initiallyVisible: Boolean, historicalDate: String? = null) = runComposeUiTest {
        val home = HomeHarness(initiallyVisible)
        try {
            setContent { home.Content() }
            waitUntil(timeoutMillis = 5_000) { home.vm.uiState.value.last7Deltas.size == 7 }
            historicalDate?.let {
                runOnIdle { home.vm.navigateToDate(it) }
                waitUntil(timeoutMillis = 5_000) { home.vm.getSelectedDateIso() == it }
            }
            if (initiallyVisible) {
                runOnIdle { home.isFocused = false }
                waitForIdle()
            }
            runOnIdle {
                home.today = "2026-09-09"
                home.isFocused = true
                home.showHome = true
            }
            waitForIdle()
            runOnIdle { home.vm.refreshCurrentDate() }
            waitForIdle()
            runOnIdle { assertEquals(historicalDate ?: home.today, home.vm.getSelectedDateIso()) }
            // The visible pager must open the same date as the summary.
            onNodeWithTag("main_day_pager").performTouchInput { swipeLeft() }
            waitForIdle()
            val expected = if (historicalDate == null) "2026-09-10" else "2026-09-07"
            runOnIdle { assertEquals(expected, home.vm.getSelectedDateIso()) }
            onNode(hasText("Breakfast") and SemanticsMatcher("visible page") { it.boundsInRoot.width > 0 }).performClick()
            runOnIdle { assertEquals(expected, home.openedMealDate) }
            // Re-entering must preserve the selection after today's date is already observed.
            runOnIdle { home.showHome = false }
            waitForIdle()
            runOnIdle { home.showHome = true }
            waitForIdle()
            runOnIdle { assertEquals(expected, home.vm.getSelectedDateIso()) }
        } finally {
            home.close()
        }
    }

    @Test
    fun datePickerAndSwipesKeepPagerAndSummaryInSync() = runComposeUiTest {
        val home = HomeHarness(true)
        try {
            setContent { home.Content() }
            waitUntil(timeoutMillis = 5_000) { home.vm.uiState.value.last7Deltas.size == 7 }
            onNodeWithText("Today, 8 Sep").performClick()
            onNodeWithText("Monday, September 7, 2026").performClick()
            onNodeWithText("OK").performClick()
            waitForIdle()
            onNodeWithText("Mon, 7 Sep").assertExists()
            onNode(hasText("Breakfast") and SemanticsMatcher("visible page") { it.boundsInRoot.width > 0 }).performClick()
            runOnIdle { assertEquals("2026-09-07", home.openedMealDate) }

            onNodeWithTag("main_day_pager").performTouchInput { swipeRight() }
            waitForIdle()
            onNodeWithText("Sun, 6 Sep").assertExists()
            onNode(hasText("Breakfast") and SemanticsMatcher("visible page") { it.boundsInRoot.width > 0 }).performClick()
            runOnIdle {
                assertEquals("2026-09-06", home.vm.getSelectedDateIso())
                assertEquals("2026-09-06", home.openedMealDate)
            }
        } finally {
            home.close()
        }
    }

    @Test
    fun summaryAndGraphUpdateBeforeTheDragIsReleased() = runComposeUiTest {
        val home = HomeHarness(true)
        try {
            setContent { home.Content() }
            waitUntil(timeoutMillis = 5_000) { home.vm.uiState.value.last7Deltas.size == 7 }
            val pager = onNodeWithTag("main_day_pager")
            // Cross into the next page, but keep the finger down. Waiting for
            // settledPage would leave yesterday's summary visible at this point.
            pager.performTouchInput {
                down(Offset(width * 0.9f, centerY))
                moveTo(Offset(width * 0.6f, centerY), delayMillis = 150)
                moveTo(Offset(width * 0.1f, centerY), delayMillis = 150)
            }
            waitForIdle()
            runOnIdle {
                assertEquals("2026-09-09", home.vm.getSelectedDateIso())
                assertEquals(LocalDate.parse("2026-09-08"), home.vm.uiState.value.last7Deltas.last().date)
            }
            onNodeWithText("Wed, 9 Sep").assertExists()
            pager.performTouchInput { up() }
        } finally {
            home.close()
        }
    }

    private class HomeHarness(initiallyVisible: Boolean) {
        private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        private val database = Database(driver).also { Database.Schema.synchronous().create(driver) }
        private val dispatcher = object : CoroutineDispatcher() {
            override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
                javax.swing.SwingUtilities.invokeLater(block)
            }
        }
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        var today = "2026-09-08"
        var showHome by mutableStateOf(initiallyVisible)
        var isFocused by mutableStateOf(true)
        var openedMealDate: String? = null
        val vm = MainViewModel(
            IntakeRepository(database), ExerciseRepository(database),
            WeightRepository(database), SettingsRepository(database), today,
            dateProvider = { today }, viewModelScope = scope
        )

        @Composable
        fun Content() {
            if (showHome) {
                val windowInfo = object : WindowInfo by LocalWindowInfo.current {
                    override val isWindowFocused get() = isFocused
                }
                CompositionLocalProvider(LocalWindowInfo provides windowInfo) {
                    AppTheme {
                        // Match the extra refresh performed by the navigation entry.
                        LaunchedEffect(Unit) { vm.refresh() }
                        MainScreen(vm, { _, date -> openedMealDate = date }, {}, {}, {}, {}, {})
                    }
                }
            }
        }

        fun close() {
            scope.cancel()
            driver.close()
        }
    }
}
