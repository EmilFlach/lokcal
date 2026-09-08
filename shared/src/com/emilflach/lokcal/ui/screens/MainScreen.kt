package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.drop
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GradientBackground
import com.emilflach.lokcal.ui.components.isNativeNavigation
import com.emilflach.lokcal.ui.components.main.MainMealList
import com.emilflach.lokcal.ui.components.main.MainSummary
import com.emilflach.lokcal.ui.components.main.MainSummaryGraph
import com.emilflach.lokcal.ui.components.main.MainSummaryKcal
import com.emilflach.lokcal.ui.components.platformBottomInset
import com.emilflach.lokcal.ui.components.platformTopInset
import com.emilflach.lokcal.viewmodel.DayState
import com.emilflach.lokcal.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenMeal: (String, String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenStatistics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val animationTrigger by viewModel.animationTrigger.collectAsState()
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(viewModel, isWindowFocused) {
        if (isWindowFocused) {
            viewModel.refresh()
            while (isActive) {
                viewModel.refreshCurrentDate()
                delay(10_000)
            }
        }
    }
    val pagerState = rememberPagerState(
        initialPage = viewModel.getPageForDate(uiState.selectedDate)
    ) { viewModel.pageCount }
    val isProgrammaticScroll = remember { arrayOf(false) }

    LaunchedEffect(uiState.selectedDate) {
        val targetPage = viewModel.getPageForDate(uiState.selectedDate)
        if (pagerState.currentPage != targetPage) {
            isProgrammaticScroll[0] = true
            try {
                pagerState.animateScrollToPage(targetPage)
            } finally {
                isProgrammaticScroll[0] = false
            }
        }
    }

    LaunchedEffect(pagerState, viewModel) {
        // The first emission describes initialization, not user navigation. In
        // particular, it may still show yesterday while the focus refresh loads today.
        snapshotFlow { pagerState.currentPage }.drop(1).collect { page ->
            if (!isProgrammaticScroll[0]) {
                viewModel.onPageSelected(page)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compactMealDescriptions = maxHeight < 900.dp
            val compactHeader = maxHeight < 800.dp
            val hideGraphs = maxHeight < 700.dp
            
            GradientBackground(uiState.dayState.percentageLeft.toFloat())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = platformTopInset, bottom = platformBottomInset)
            ) {

                if (!isNativeNavigation) {
                    MainSummary(
                        state = uiState.dayState,
                        formattedDate = viewModel.formattedDate(),
                        selectedDate = uiState.selectedDate,
                        onDateSelect = { viewModel.loadFor(it) },
                        last7 = uiState.last7Deltas,
                        animationTrigger = animationTrigger,
                        onOpenExercise = onOpenExercise,
                        onOpenWeightToday = onOpenWeightToday,
                        onOpenWeightList = onOpenWeightList,
                        onOpenStatistics = onOpenStatistics,
                        onOpenSettings = onOpenSettings,
                        isCompact = compactHeader,
                        hideGraphs = hideGraphs
                    )
                } else {
                    // Show only the kcal/graph section when using native navigation (header is in nav bar)
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(LocalRecipesColors.current.backgroundPage, MaterialTheme.shapes.medium)
                            .padding(if (compactHeader) 12.dp else 16.dp)
                    ) {
                        val boxWidth = maxWidth
                        Column {
                            MainSummaryKcal(
                                state = uiState.dayState,
                                colors = LocalRecipesColors.current,
                                fadeAlpha = 1f,
                                onOpenExercise = { onOpenExercise(uiState.selectedDate.toString()) },
                                isCompact = compactHeader
                            )

                            if (!hideGraphs) {
                                Spacer(Modifier.height(16.dp))
                                MainSummaryGraph(uiState.last7Deltas, boxWidth, onOpenStatistics)
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 16.dp,
                    flingBehavior = PagerDefaults.flingBehavior(
                        state = pagerState,
                        pagerSnapDistance = PagerSnapDistance.atMost(1)
                    ),
                    beyondViewportPageCount = 1,
                    modifier = Modifier.weight(1f).testTag("main_day_pager")
                ) { page ->
                val date = remember(page) { viewModel.getDateForPage(page) }
                var dayState by remember(date) { mutableStateOf(DayState()) }
                
                LaunchedEffect(date, uiState.selectedDate, uiState.dayState) {
                    dayState = if (date == uiState.selectedDate) {
                        uiState.dayState
                    } else {
                        viewModel.getDayStateFor(date)
                    }
                }

                MainMealList(
                    state = dayState,
                    selectedDate = date,
                    onOpenMeal = onOpenMeal,
                    isCompact = compactMealDescriptions
                )
            }
        }
    }
}
}

