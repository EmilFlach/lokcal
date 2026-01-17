package com.emilflach.lokcal.ui.components.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.DayDelta
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.*

@Composable
fun MainSummaryGraph(last7: List<DayDelta>, maxWidth: Dp) {
    val colors = LocalRecipesColors.current
    val isDarkTheme = isSystemInDarkTheme()

    val hasData = remember(last7) {
        if (last7.isEmpty()) return@remember false
        val firstDelta = last7.first().deltaKcal
        // If there's any variation in deltas, or if any delta is non-negative (meaning they ate something), consider it having data.
        // If they haven't logged anything, delta = -budget for all days.
        last7.any { it.deltaKcal != firstDelta } || last7.any { it.deltaKcal >= 0 }
    }

    val graphData = remember(last7, hasData) {
        if (hasData) {
            last7.map { d ->
                val label = d.date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() }

                val colorMain = colors.foregroundBrand
                val colorSubtle = colors.foregroundBrand.copy(alpha = if (isDarkTheme) 0.0f else 0.4f)

                val gradient = if (d.deltaKcal < 0) {
                    verticalGradient(0f to colorSubtle, 1f to colorMain)
                } else {
                    verticalGradient(0f to colorMain, 1f to colorSubtle)
                }

                val value = when (d.deltaKcal) {
                    in 400.0..Double.POSITIVE_INFINITY -> 400.0
                    in 0.0..40.0 -> 40.0
                    in -40.0..0.0 -> -40.0
                    in Double.NEGATIVE_INFINITY..-400.0 -> -400.0
                    else -> d.deltaKcal
                }
                Bars.Data(label = label, value = value, color = gradient)
            }
        } else {
            // Fake data for background when no data is available
            val fakeValues = listOf(-120.0, 50.0, -80.0, 150.0, -40.0, -200.0, 100.0)
            val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            labels.zip(fakeValues).map { (label, value) ->
                val colorMain = colors.foregroundBrand.copy(alpha = 0.2f)
                val colorSubtle = colors.foregroundBrand.copy(alpha = 0.05f)
                val gradient = if (value < 0) {
                    verticalGradient(0f to colorSubtle, 1f to colorMain)
                } else {
                    verticalGradient(0f to colorMain, 1f to colorSubtle)
                }
                Bars.Data(label = label, value = value, color = gradient)
            }
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            ColumnChart(
                modifier = Modifier
                    .offset(x = (-1).dp) // The chart has a 1.dp offset for some reason
                    .height(80.dp),
                data = listOf(
                    Bars(
                        label = "Last 7",
                        values = graphData
                    )
                ),
                barProperties = BarProperties(
                    cornerRadius = Bars.Data.Radius.Rectangle(
                        topRight = 6.dp,
                        topLeft = 6.dp,
                        bottomLeft = 3.dp,
                        bottomRight = 3.dp
                    ),
                    spacing = 3.dp,
                    thickness = ((maxWidth - (6 * 3).dp) / 7), // Divide thickness for the max width, minus the spacing
                ),
                minValue = (graphData.minOfOrNull { it.value } ?: 0.0).coerceIn(-400.0, 0.0),
                maxValue = graphData.maxOfOrNull { it.value } ?: 0.0.coerceIn(0.0, 400.0),
                animationDelay = 0,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                animationMode = AnimationMode.Together { it * 25L },
                dividerProperties = DividerProperties(false),
                gridProperties = GridProperties(false),
                labelProperties = LabelProperties(false),
                labelHelperProperties = LabelHelperProperties(false),
                indicatorProperties = HorizontalIndicatorProperties(false),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().offset(x = 0.5.dp), // Magic number to fix label alignment
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                graphData.forEach { data ->
                    Text(
                        text = data.label!!,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (hasData) colors.foregroundSupport else colors.foregroundSupport.copy(alpha = 0.2f)
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width((maxWidth / 7) - 2.dp), // Magic number to fix label alignment
                    )
                }
            }
        }

        if (!hasData) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = 16.dp), // Align with graph height
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No data for the last 7 days",
                    style = MaterialTheme.typography.titleSmall.copy(color = colors.foregroundSupport),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Start logging to see your progress",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.foregroundSupport.copy(alpha = 0.6f)),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}