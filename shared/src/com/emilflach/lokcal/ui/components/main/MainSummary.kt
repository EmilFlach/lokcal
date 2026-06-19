package com.emilflach.lokcal.ui.components.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.DayDelta
import com.emilflach.lokcal.viewmodel.DayState
import kotlinx.datetime.LocalDate

@Composable
fun MainSummary(
    state: DayState,
    formattedDate: String,
    selectedDate: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    last7: List<DayDelta>,
    animationTrigger: Int,
    onOpenExercise: (String) -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    hideGraphs: Boolean = isCompact,
) {
    val colors = LocalRecipesColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.backgroundPage, MaterialTheme.shapes.medium)
            .padding(if (isCompact) 12.dp else 16.dp)
    ) {
        Column {
            MainSummaryHeader(
                formattedDate = formattedDate,
                selectedDate = selectedDate,
                onDateSelect = onDateSelect,
                onOpenWeightList = onOpenWeightList,
                onOpenWeightToday = onOpenWeightToday,
                onOpenStatistics = onOpenStatistics,
                onOpenSettings = onOpenSettings,
                colors = colors,
                showWeightBadge = state.showWeightPrompt
            )
            Spacer(Modifier.height(if (isCompact) 8.dp else 12.dp))

            val fadeAlpha = remember { Animatable(1f) }
            LaunchedEffect(animationTrigger) {
                if (animationTrigger > 0) {
                    fadeAlpha.animateTo(
                        targetValue = 0.5f,
                        animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                    )
                    fadeAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                    )
                }
            }

            MainSummaryKcal(
                state = state,
                colors = colors,
                fadeAlpha = fadeAlpha.value,
                onOpenExercise = { onOpenExercise(selectedDate.toString()) },
                isCompact = isCompact
            )

            if (!hideGraphs) {
                Spacer(Modifier.height(16.dp))
                MainSummaryGraph(last7, this@BoxWithConstraints.maxWidth, onOpenStatistics)
            }
        }
    }
}
