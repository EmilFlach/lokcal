package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.StatisticsViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import ir.ehsannarmani.compose_charts.models.DrawStyle.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit
) {
    val mostEaten by viewModel.mostEaten.collectAsState()
    val dailyKcal by viewModel.dailyKcal.collectAsState()
    val selectedMonths by viewModel.selectedMonths.collectAsState()
    val colors = LocalRecipesColors.current

    Scaffold(
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            PeriodSelector(selectedMonths) { viewModel.setPeriod(it) }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (dailyKcal.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                "Kcal intake over time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(24.dp))

                            val values = dailyKcal.map { it.delta }
                            val minKcal = values.minOrNull() ?: 0.0
                            val maxKcal = values.maxOrNull() ?: 0.0
                            val graphTextStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundSupport)

                            Box(
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth()
                            ) {
                                LineChart(
                                    modifier = Modifier.fillMaxSize(),
                                    minValue = minKcal.coerceIn(Double.NEGATIVE_INFINITY, 0.0),
                                    maxValue = maxKcal.coerceAtLeast(0.0),
                                    data = remember(values) {
                                        listOf(
                                            Line(
                                                label = "Kcal",
                                                values = values,
                                                color = SolidColor(colors.foregroundBrand),
                                                firstGradientFillColor = colors.foregroundBrand.copy(alpha = .5f),
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
                                    animationMode = AnimationMode.Together(),
                                )

                                Text(
                                    text = if (minKcal < 0) "${minKcal.toInt()} kcal" else "0 kcal",
                                    style = graphTextStyle,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 4.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface1)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                                Text(
                                    text = if (maxKcal > 0) "+${maxKcal.toInt()} kcal" else "0 kcal",
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
                        headlineContent = { Text(
                            item.item_name,
                            style = MaterialTheme.typography.bodyMedium) },
                        trailingContent = {
                            val weightG = item.total_quantity_g ?: 0.0
                            Text(
                                text = if (weightG >= 1000) "${(weightG / 1000).roundToOneDecimal()} kg" else "${weightG.toInt()} g",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        modifier = Modifier.clip(getRoundedCornerShape(
                            index = index,
                            size = mostEaten.size
                        ))
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun PeriodSelector(selectedMonths: Int, onSelect: (Int) -> Unit) {
    val colors = LocalRecipesColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.backgroundSurface2)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(1, 3, 6, 12).forEach { months ->
            val isSelected = selectedMonths == months
            val label = when (months) {
                1 -> "1m"
                3 -> "3m"
                6 -> "6m"
                12 -> "1y"
                else -> "${months}m"
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.foregroundBrand else Color.Transparent)
                    .clickable { onSelect(months) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else colors.foregroundDefault,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}


private fun Double.roundToOneDecimal(): Double = (this * 10).toInt() / 10.0
