package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.WeightListViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import ir.ehsannarmani.compose_charts.models.DrawStyle.Stroke
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun WeightListScreen(
    viewModel: WeightListViewModel,
    onBack: () -> Unit,
    openAdd: Boolean = false,
) {
    val colors = LocalRecipesColors.current

    BackHandler {
        onBack()
    }
    val haptic = LocalHapticFeedback.current
    val items by viewModel.items.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val input by viewModel.input.collectAsState()
    val error by viewModel.error.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(openAdd) {
        if (openAdd) viewModel.openAddDialog(true)
    }

    if (showAddDialog) {
        AlertDialog(
            containerColor = colors.backgroundSurface1,
            onDismissRequest = { viewModel.openAddDialog(false) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveToday()
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.openAddDialog(false)
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                }) { Text("Cancel") }
            },
            title = { Text("Add today's weight") },
            text = {
                Column {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { new -> viewModel.onInputChanged(new) },
                        label = { Text("kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.focusRequester(focusRequester)

                    )
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    PlatformScaffold(
        topBar = {
                TopAppBar(
                    title = { Text("Weight") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.openAddDialog(true) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add weight")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundPage,
                        titleContentColor = colors.foregroundDefault,
                        navigationIconContentColor = colors.foregroundDefault,
                        actionIconContentColor = colors.foregroundDefault,
                    )
                )
            }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            WeightChart(viewModel)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                items(items) { item: WeightLog ->
                    ListItem(
                        headlineContent = { Text("${item.weight_kg} kg") },
                        supportingContent = { Text(item.date, color = colors.foregroundSupport) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        viewModel.deleteById(item.id)
                                    }
                            )
                        },
                        modifier = Modifier.clip(getRoundedCornerShape(
                            index = items.indexOf(item),
                            size = items.size
                        ))
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun WeightChart(viewModel: WeightListViewModel) {
    val colors = LocalRecipesColors.current
    val chart by viewModel.chart.collectAsState()
    val sorted = chart.sorted
    val displayedItems = chart.displayedItems
    val weights = chart.weights

    val graphTextStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundSupport)
    Box(
        modifier = Modifier.height(200.dp).fillMaxWidth().padding(16.dp)) {
        LineChart(
            minValue = weights.minOrNull() ?: 0.0,
            maxValue = weights.maxOrNull() ?: 0.0,
            dividerProperties = DividerProperties(false),
            gridProperties = GridProperties(false),
            labelProperties = LabelProperties(false),
            labelHelperProperties = LabelHelperProperties(false),
            indicatorProperties = HorizontalIndicatorProperties(
                false,
                textStyle = graphTextStyle,
            ),

            modifier = Modifier.fillMaxSize(),
            data = remember (weights) {
                listOf(
                    Line(
                        label = "Weights",
                        values = weights,
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
            animationMode = AnimationMode.Together(),
        )
        Text(
            text = "${chart.min} kg",
            style = graphTextStyle,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 4.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colors.backgroundSurface1)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Text(
            text = "${chart.max} kg",
            style = graphTextStyle,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 4.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colors.backgroundSurface1)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }


    if (sorted.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (displayedItems.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = displayedItems.first().date,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.foregroundSupport,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(colors.backgroundSurface1)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        text = displayedItems.last().date,
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
            }
            RangeSlider(
                value = chart.startIndex.toFloat()..chart.endIndex.toFloat(),
                onValueChange = { range ->
                    viewModel.onChartRangeChanged(range.start.toInt(), range.endInclusive.toInt())
                },
                valueRange = 0f..(sorted.size - 1).toFloat(),
                steps = if (sorted.size > 2) sorted.size - 2 else 0,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.foregroundBrand,
                    activeTrackColor = colors.foregroundBrand,
                    inactiveTrackColor = colors.foregroundSupport.copy(alpha = 0.2f)
                )
            )
        }
    }

}