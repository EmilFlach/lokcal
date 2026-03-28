package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.StatisticsViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import ir.ehsannarmani.compose_charts.models.DrawStyle.Stroke

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }
    val mostEaten by viewModel.mostEaten.collectAsState()
    val dailyKcal by viewModel.dailyKcal.collectAsState()
    val chart by viewModel.chart.collectAsState()
    val daysFilled by viewModel.daysFilled.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val graphMode by viewModel.graphMode.collectAsState()
    val colors = LocalRecipesColors.current

    PlatformScaffold(
        topBar = {
                TopAppBar(
                    title = { Text("Statistics") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
        containerColor = colors.backgroundPage
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.backgroundSurface1
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Days filled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport
                                )
                                Text(
                                    "$daysFilled days",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foregroundDefault
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (dailyKcal.isNotEmpty()) {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when (graphMode) {
                                        StatisticsViewModel.GraphMode.BURNED -> "Burned kcal"
                                        StatisticsViewModel.GraphMode.EATEN -> "Eaten kcal"
                                        StatisticsViewModel.GraphMode.BALANCE -> "Remaining kcal"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    StatisticsViewModel.GraphMode.entries.forEach { mode ->
                                        TextButton(
                                            onClick = { viewModel.setGraphMode(mode) },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = if (graphMode == mode) colors.foregroundBrand else colors.foregroundSupport
                                            )
                                        ) {
                                            Text(
                                                when (mode) {
                                                    StatisticsViewModel.GraphMode.BALANCE -> "Remaining"
                                                    StatisticsViewModel.GraphMode.BURNED -> "Burned"
                                                    StatisticsViewModel.GraphMode.EATEN -> "Eaten"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))

                            val values = when (graphMode) {
                                StatisticsViewModel.GraphMode.BURNED -> dailyKcal.map { it.burned }
                                StatisticsViewModel.GraphMode.EATEN -> dailyKcal.map { it.eaten }
                                StatisticsViewModel.GraphMode.BALANCE -> dailyKcal.map { it.delta }
                            }
                            val minVal = values.minOrNull() ?: 0.0
                            val maxVal = values.maxOrNull() ?: 0.0
                            val graphTextStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundSupport)

                            Box(
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth()
                            ) {
                                LineChart(
                                    modifier = Modifier.fillMaxSize(),
                                    minValue = minVal.coerceIn(Double.NEGATIVE_INFINITY, 0.0),
                                    maxValue = maxVal.coerceAtLeast(1.0),
                                    data = remember(values, graphMode) {
                                        listOf(
                                            Line(
                                                label = when (graphMode) {
                                                    StatisticsViewModel.GraphMode.BURNED -> "Burned"
                                                    StatisticsViewModel.GraphMode.EATEN -> "Eaten"
                                                    StatisticsViewModel.GraphMode.BALANCE -> "Balance"
                                                },
                                                values = values,
                                                color = SolidColor(
                                                    when (graphMode) {
                                                        StatisticsViewModel.GraphMode.BURNED -> colors.foregroundSuccess
                                                        StatisticsViewModel.GraphMode.EATEN -> colors.foregroundWarning
                                                        StatisticsViewModel.GraphMode.BALANCE -> colors.foregroundBrand
                                                    }
                                                ),
                                                firstGradientFillColor = (when (graphMode) {
                                                    StatisticsViewModel.GraphMode.BURNED -> colors.foregroundSuccess
                                                    StatisticsViewModel.GraphMode.EATEN -> colors.foregroundWarning
                                                    StatisticsViewModel.GraphMode.BALANCE -> colors.foregroundBrand
                                                }).copy(alpha = .5f),
                                                secondGradientFillColor = Color.Transparent,
                                                strokeAnimationSpec = tween(200, easing = EaseInOutCubic),
                                                gradientAnimationSpec = tween(200, easing = EaseInOutCubic),
                                                gradientAnimationDelay = 100,
                                                drawStyle = Stroke(2.dp),
                                            )
                                        )
                                    },
                                    dividerProperties = DividerProperties(enabled = false),
                                    gridProperties = GridProperties(enabled = false),
                                    labelProperties = LabelProperties(enabled = false),
                                    labelHelperProperties = LabelHelperProperties(enabled = false),
                                    indicatorProperties = HorizontalIndicatorProperties(
                                        enabled = false,
                                        textStyle = graphTextStyle
                                    ),
                                    zeroLineProperties = ZeroLineProperties(
                                        enabled = true,
                                        color = SolidColor(colors.foregroundSupport.copy(alpha = 0.5f)),
                                        thickness = 2.dp
                                    ),
                                    curvedEdges = true,
                                    animationMode = AnimationMode.Together { it * 2L },
                                )

                                Text(
                                    text = if (minVal < 0) "${minVal.toInt()} kcal" else "0 kcal",
                                    style = graphTextStyle,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 4.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface1)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                                Text(
                                    text = if (maxVal > 0) "${maxVal.toInt()} kcal" else "0 kcal",
                                    style = graphTextStyle,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(horizontal = 4.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface1)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dailyKcal.first().day,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface1)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Text(
                                    text = dailyKcal.last().day,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport,
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface1)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            RangeSlider(
                                value = chart.startIndex.toFloat()..chart.endIndex.toFloat(),
                                onValueChange = { range ->
                                    viewModel.onChartRangeChanged(range.start.toInt(), range.endInclusive.toInt())
                                },
                                valueRange = 0f..(chart.allDays.size - 1).toFloat(),
                                steps = if (chart.allDays.size > 2) chart.allDays.size - 2 else 0,
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.foregroundBrand,
                                    activeTrackColor = colors.foregroundBrand,
                                    inactiveTrackColor = colors.foregroundSupport.copy(alpha = 0.2f)
                                )
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }

                insights?.let { ins ->
                    item {
                        Text(
                            "Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.backgroundSurface1)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InsightRow("Total weight eaten", "${ins.totalKgEaten.roundToOneDecimal()} kg")
                            InsightRow("Tracked intakes", "${ins.intakeCount}")
                            InsightRow("Total kcal eaten", "${ins.totalKcalEaten.toInt()} kcal")
                            InsightRow("Total kcal burned", "${ins.totalKcalBurned.toInt()} kcal")
                            InsightRow(
                                "Budget balance",
                                "${if (ins.totalDelta > 0) "+" else ""}${ins.totalDelta.toInt()} kcal",
                                color = if (ins.totalDelta > 0) colors.foregroundDanger else colors.foregroundSuccess
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                }

                item {
                    Text(
                        "Most eaten foods",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                }

                itemsIndexed(mostEaten) { index, item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                item.item_name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                "Tracked ${item.intake_count} times",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foregroundSupport
                            )
                        },
                        trailingContent = {
                            val weightG = item.total_quantity_g ?: 0.0
                            Text(
                                text = if (weightG >= 1000) "${(weightG / 1000).roundToOneDecimal()} kg" else "${weightG.toInt()} g",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.clip(
                            getRoundedCornerShape(
                                index = index,
                                size = mostEaten.size
                            )
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String, color: Color = LocalRecipesColors.current.foregroundDefault) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalRecipesColors.current.foregroundSupport)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun Double.roundToOneDecimal(): Double = (this * 10).toInt() / 10.0
