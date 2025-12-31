package com.emilflach.lokcal.ui.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.RecipesColors
import com.emilflach.lokcal.viewmodel.DayState
import kotlin.math.roundToInt

@Composable
fun MainSummaryKcal(
    state: DayState,
    colors: RecipesColors,
    fadeAlpha: Float,
    onOpenExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .weight(4f)
                .fillMaxHeight()
                .background(colors.backgroundSurface2, MaterialTheme.shapes.medium)
                .padding(16.dp)
                .alpha(fadeAlpha)
        ) {
            Column {
                Text(
                    text = if (state.leftKcal > 0) "Remaining" else "Above goal",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (state.leftKcal > 0) "${state.leftKcal.roundToInt()}" else "${state.leftKcal.roundToInt() * -1}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Left,
                    color = colors.foregroundDefault,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.foregroundBrand.copy(alpha = 0.1f))
                ) {
                    val total = (state.startingKcal + state.burnedKcal).coerceAtLeast(state.eatenKcal).coerceAtLeast(1.0)
                    val eatenRatio = (state.eatenKcal / total).toFloat()
                    val remainingGoal = (state.startingKcal - state.eatenKcal).coerceAtLeast(0.0)
                    val remainingGoalRatio = (remainingGoal / total).toFloat()
                    val remainingBurned = (total - state.eatenKcal - remainingGoal).coerceAtLeast(0.0)
                    val remainingBurnedRatio = (remainingBurned / total).toFloat()

                    if (eatenRatio > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(eatenRatio)
                                .background(colors.foregroundBrand)
                        )
                    }
                    if (remainingBurnedRatio > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(remainingBurnedRatio)
                                .background(colors.foregroundBrand.copy(alpha = 0.6f))
                        )
                    }
                    if (remainingGoalRatio > 0) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(remainingGoalRatio)
                                .background(colors.foregroundBrand.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(
            Modifier
                .weight(3f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(
                        MaterialTheme.shapes.medium.copy(
                            bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                        )
                    )
                    .background(colors.backgroundSurface1)
                    .clickable { onOpenExercise() }
                    .padding(horizontal = 16.dp)
                    .alpha(fadeAlpha)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Burned",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.foregroundSupport,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = state.burnedKcal.roundToInt().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.foregroundSupport
                    )
                }
            }
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(
                        MaterialTheme.shapes.medium.copy(
                            topStart = MaterialTheme.shapes.extraSmall.topStart,
                            topEnd = MaterialTheme.shapes.extraSmall.topEnd
                        )
                    )
                    .background(colors.backgroundSurface1)
                    .padding(horizontal = 16.dp)
                    .alpha(fadeAlpha)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Eaten",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.foregroundSupport
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = state.eatenKcal.roundToInt().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.foregroundSupport
                    )
                }
            }
        }
    }
}
