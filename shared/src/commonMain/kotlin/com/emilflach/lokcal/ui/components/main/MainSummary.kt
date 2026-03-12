package com.emilflach.lokcal.ui.components.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.theme.RecipesColors
import com.emilflach.lokcal.viewmodel.DayDelta
import com.emilflach.lokcal.viewmodel.DayState
import kotlinx.datetime.LocalDate

@Composable
fun MainSummary(
    state: DayState,
    formattedDate: String,
    onDateClick: () -> Unit,
    selectedDate: LocalDate,
    last7: List<DayDelta>,
    animationTrigger: Int,
    onOpenExercise: (String) -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalRecipesColors.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.backgroundPage, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column {
            MainSummaryHeader(
                formattedDate = formattedDate,
                onDateClick = onDateClick,
                onOpenWeightList = onOpenWeightList,
                onOpenStatistics = onOpenStatistics,
                onOpenSettings = onOpenSettings,
                colors = colors
            )
            Spacer(Modifier.height(12.dp))

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
                onOpenExercise = { onOpenExercise(selectedDate.toString()) }
            )

            Spacer(Modifier.height(16.dp))
            MainSummaryGraph(last7, this@BoxWithConstraints.maxWidth)
            if (state.showWeightPrompt) {
                Spacer(Modifier.height(16.dp))
                WeightPrompt(
                    colors = colors,
                    onOpenWeightToday = onOpenWeightToday
                )
            }
        }
    }
}

@Composable
private fun WeightPrompt(
    colors: RecipesColors,
    onOpenWeightToday: () -> Unit
) {
    Surface(
        color = colors.backgroundBrand,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundPage)
            .clip(MaterialTheme.shapes.large)
            .clickable { onOpenWeightToday() }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("It's Thursday, log your weight!", style = MaterialTheme.typography.titleMedium)
        }
    }
}
