package com.emilflach.lokcal.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.util.BurnBuckets
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.*
import kotlin.math.roundToInt

/** Below this a bar is difficult to read or tap, so slots get grouped into wider ones instead. */
private val MIN_BAR_WIDTH = 18.dp

@Composable
fun DailyBurnChart(
    bucketKcal: List<Double>,
    totalKcal: Double,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRecipesColors.current
    val values = remember(bucketKcal) {
        List(BurnBuckets.BUCKETS_PER_DAY) { bucketKcal.getOrElse(it) { 0.0 } }
    }
    val hasData = totalKcal > 0.0 || values.any { it > 0.0 }
    val visibleBuckets = remember(values) { BurnBuckets.focusedRange(values) }
    val timeTicks = remember(visibleBuckets) { BurnBuckets.ticks(visibleBuckets) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.backgroundSurface1)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Calories burned",
                style = MaterialTheme.typography.titleSmall,
                color = colors.foregroundDefault,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalKcal.roundToInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.foregroundDefault,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "kcal",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.foregroundSupport,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(116.dp)) {
            val spacing = 2.dp
            // 30-minute bars where they fit; wider slices rather than unreadable slivers.
            val slotsPerBar = remember(visibleBuckets, maxWidth) {
                BurnBuckets.slotsPerBar(
                    bucketCount = visibleBuckets.size,
                    availableWidth = maxWidth.value,
                    spacing = spacing.value,
                    minBarWidth = MIN_BAR_WIDTH.value,
                )
            }
            val barGroups = remember(visibleBuckets, slotsPerBar) {
                BurnBuckets.barGroups(visibleBuckets, slotsPerBar)
            }
            val barValues = remember(values, barGroups) {
                barGroups.map { group -> group.sumOf { values[it] } }
            }
            val maxValue = barValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val chartSeries = remember(barGroups, barValues, colors.foregroundBrand) {
                listOf(
                    Bars(
                        label = "Calories burned",
                        values = barGroups.mapIndexed { index, group ->
                            Bars.Data(
                                id = group.first(),
                                label = BurnBuckets.label(group.first()),
                                value = barValues[index],
                                color = SolidColor(colors.foregroundBrand),
                            )
                        },
                    )
                )
            }
            ColumnChart(
                modifier = Modifier.fillMaxSize(),
                data = chartSeries,
                barProperties = BarProperties(
                    thickness = ((maxWidth - spacing * (barGroups.size - 1)) / barGroups.size)
                        .coerceAtLeast(1.dp),
                    spacing = spacing,
                    cornerRadius = Bars.Data.Radius.Rectangle(
                        topLeft = 5.dp,
                        topRight = 5.dp,
                        bottomLeft = 1.dp,
                        bottomRight = 1.dp,
                    ),
                ),
                popupProperties = PopupProperties(
                    textStyle = MaterialTheme.typography.labelMedium.copy(color = colors.backgroundPage),
                    containerColor = colors.foregroundDefault,
                    contentBuilder = { popup ->
                        val group = barGroups[popup.valueIndex]
                        "${BurnBuckets.label(group.first())}–${BurnBuckets.label(group.last() + 1)}  " +
                            "${popup.value.roundToInt()} kcal"
                    },
                ),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                animationMode = AnimationMode.Together { it * 25L },
                animationDelay = 0,
                dividerProperties = DividerProperties(false),
                gridProperties = GridProperties(false),
                labelProperties = LabelProperties(false),
                labelHelperProperties = LabelHelperProperties(false),
                indicatorProperties = HorizontalIndicatorProperties(false),
                minValue = 0.0,
                maxValue = maxValue,
            )
            if (!hasData) {
                Text(
                    text = "No activity logged yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foregroundSupport,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            timeTicks.forEach { bucket ->
                Text(
                    text = BurnBuckets.label(bucket),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.foregroundSupport,
                )
            }
        }
    }
}
