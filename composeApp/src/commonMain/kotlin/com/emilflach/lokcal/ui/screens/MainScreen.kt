package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.ui.components.GradientBackground
import com.emilflach.lokcal.ui.components.main.MainMealList
import com.emilflach.lokcal.ui.components.main.MainSummary
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
    val pagerState = rememberPagerState(initialPage = viewModel.initialPage) { viewModel.pageCount }

    LaunchedEffect(uiState.selectedDate) {
        val targetPage = viewModel.getPageForDate(uiState.selectedDate)
        if (pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageSelected(pagerState.currentPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackground(uiState.dayState.percentageLeft.toFloat())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            MainSummary(
                state = uiState.dayState,
                formattedDate = viewModel.formattedDate(),
                onDateClick = { viewModel.setToCurrentDate() },
                selectedDate = uiState.selectedDate,
                last7 = uiState.last7Deltas,
                animationTrigger = animationTrigger,
                onOpenExercise = onOpenExercise,
                onOpenWeightToday = onOpenWeightToday,
                onOpenWeightList = onOpenWeightList,
                onOpenStatistics = onOpenStatistics,
                onOpenSettings = onOpenSettings
            )

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 16.dp,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    pagerSnapDistance = PagerSnapDistance.atMost(1)
                ),
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f)
            ) { page ->
                val date = remember(page) { viewModel.getDateForPage(page) }
                val dayState = if (date == uiState.selectedDate) {
                    uiState.dayState
                } else {
                    remember(date) { viewModel.getDayStateFor(date) }
                }

                MainMealList(
                    state = dayState,
                    selectedDate = date,
                    onOpenMeal = onOpenMeal
                )
            }
        }
    }
}

