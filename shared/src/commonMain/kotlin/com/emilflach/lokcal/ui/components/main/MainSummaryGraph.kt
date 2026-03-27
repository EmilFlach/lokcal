package com.emilflach.lokcal.ui.components.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
    val isDarkTheme = colors.isDark

    val hasData = remember(last7) {
        last7.any { it.deltaKcal != 0.0 }
    }

    val graphData = remember(last7) {
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
                0.0 -> 0.0
                in 400.0..Double.POSITIVE_INFINITY -> 400.0
                in 0.0..40.0 -> 40.0
                in -40.0..0.0 -> -40.0
                in Double.NEGATIVE_INFINITY..-400.0 -> -400.0
                else -> d.deltaKcal
            }
            Bars.Data(label = label, value = value, color = gradient)
        }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
                    thickness = if (graphData.isEmpty()) 0.dp else ((maxWidth - ((graphData.size - 1) * 3).dp) / graphData.size), // Divide thickness for the max width, minus the spacing
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
                            color = colors.foregroundSupport
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width((maxWidth / graphData.size.coerceAtLeast(1)) - 2.dp), // Magic number to fix label alignment
                    )
                }
            }
        }

        if (!hasData) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .background(colors.backgroundPage.copy(alpha = 0.8f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nothing logged so far",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Past days will show up once you log",
                    style = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundSupport),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}