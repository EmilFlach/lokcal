package com.emilflach.lokcal.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.MainViewModel.DayDelta
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.*

@Composable
fun WeeklyKcalGraph(last7: List<DayDelta>, maxWidth: Dp) {
    val colors = LocalRecipesColors.current
    val graphData = remember(last7) {
        last7.map { d ->
            val label = d.date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.titlecase() }
            val gradient = if(d.deltaKcal < 0)
                verticalGradient(
                    0f to Color.Transparent,
                    1f to colors.foregroundBrand.copy(alpha = 0.9f),
                ) else
                verticalGradient(
                    0f to colors.foregroundBrand.copy(alpha = 0.9f),
                    1f to Color.Transparent,
                )
            val value = when (d.deltaKcal) {
                in 400.0..Double.POSITIVE_INFINITY -> 400.0
                in 0.0..40.0 -> 40.0
                in -40.0..0.0 -> -40.0
                in Double.NEGATIVE_INFINITY..-400.0 -> -400.0
                else -> d.deltaKcal
            }
            Bars.Data(label = label, value = value, color = gradient)
        }
    }
    ColumnChart(
        modifier = Modifier
            .fillMaxWidth()
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
            thickness = ((maxWidth - (6 * 3).dp) / 7),
        ),
        minValue = (graphData.minOfOrNull { it.value } ?: 0.0).coerceIn(-400.0, 0.0),
        maxValue = graphData.maxOfOrNull { it.value } ?: 0.0.coerceIn(0.0, 400.0),
        animationDelay = 0,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        animationMode = AnimationMode.Together { it * 20L },
        dividerProperties = DividerProperties(false),
        gridProperties = GridProperties(false),
        labelProperties = LabelProperties(false),
        labelHelperProperties = LabelHelperProperties(false),
        indicatorProperties = HorizontalIndicatorProperties(false),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        graphData.forEach { data ->
            Text(
                text = data.label!!,
                style = MaterialTheme.typography.bodySmall.copy(color = colors.foregroundSupport),
                textAlign = TextAlign.Center,
                modifier = Modifier.width(maxWidth / 7),
            )
        }

    }
}