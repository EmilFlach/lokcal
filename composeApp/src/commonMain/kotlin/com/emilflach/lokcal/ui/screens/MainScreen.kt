package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.ParticleBackground
import com.emilflach.lokcal.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenMeal: (String) -> Unit,
    onOpenExercise: () -> Unit,
) {
    val summaries by viewModel.summaries.collectAsState()
    val eaten by viewModel.eatenKcal.collectAsState()
    val left by viewModel.leftKcal.collectAsState()
    val burned by viewModel.burnedKcal.collectAsState()
    val exerciseTotal by viewModel.exerciseTotalKcal.collectAsState()
    val exerciseSummary by viewModel.exerciseSummaryText.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val density = LocalDensity.current
    val colors = LocalRecipesColors.current
    val thresholdPx = remember(density) { with(density) { 64.dp.toPx() } }

    fun calculateParticleCount(eatenCalories: Double, maxCalories: Double = 1690.0): Int {
        val ratio = (eatenCalories / maxCalories).coerceIn(0.0, 1.0)
        return (ratio * 3000).toInt()
    }

    val particleCount = remember(eaten, burned) {
        calculateParticleCount(eaten, 1690 + burned)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Particle background
        ParticleBackground(
            particleCount = particleCount,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .pointerInput(Unit) {
                    var accumX = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                accumX > thresholdPx -> viewModel.previousDay()
                                accumX < -thresholdPx -> viewModel.nextDay()
                            }
                            accumX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            accumX += dragAmount
                        }
                    )
                }
        ) {
            // Selected date label
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundPage, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedDate.toString(),
                    color = colors.foregroundSupport,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (left > 0) "${left.toInt()}" else "${left.toInt() * -1}",
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (left > 0) "kcal left" else "kcal over",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Eaten: ${eaten.toInt()} kcal, Burned: ${burned.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.foregroundSupport,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

            }

            // Push sections to the bottom
            Spacer(Modifier.weight(1f))



            // Meal sections displayed at the bottom
            summaries.forEach { s ->
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(colors.backgroundPage)
                        .clip(MaterialTheme.shapes.large)
                        .let { modifier ->
                            if (s.totalKcal.toInt() > 0) {
                                modifier.drawBehind {
                                    val borderWidth = 2.dp.toPx()
                                    drawRect(
                                        color = colors.backgroundBrand,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(borderWidth, this.size.height)
                                    )
                                }
                            } else {
                                modifier
                            }
                        }
                        .clickable { onOpenMeal(s.mealType) }

                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                s.mealType.lowercase().replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${s.totalKcal.toInt()} kcal",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (s.summaryText.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = s.summaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foregroundSupport,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Exercise summary card
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .background(colors.backgroundPage)
                    .clip(MaterialTheme.shapes.large)
                    .let { modifier ->
                        if (exerciseTotal.toInt() > 0) {
                            modifier.drawBehind {
                                val borderWidth = 2.dp.toPx()
                                drawRect(
                                    color = colors.backgroundBrand,
                                    topLeft = Offset(0f, 0f),
                                    size = Size(borderWidth, this.size.height)
                                )
                            }
                        } else {
                            modifier
                        }
                    }
                    .clickable { onOpenExercise() }
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Exercise", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${exerciseTotal.toInt()} kcal",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (exerciseSummary.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = exerciseSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foregroundSupport
                        )
                    }
                }
            }
        }
    }
}
