package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.WeightListViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle.Stroke
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightListScreen(
    viewModel: WeightListViewModel,
    onBack: () -> Unit,
    openAdd: Boolean = false,
) {
    val colors = LocalRecipesColors.current
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
                TextButton(onClick = { viewModel.saveToday() }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.openAddDialog(false) }) { Text("Cancel") }
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
            focusRequester.requestFocus()
        }
    }

    Scaffold(
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
            ) {
                items(items) { item: WeightLog ->
                    ListItem(
                        headlineContent = { Text("${item.weight_kg} kg") },
                        supportingContent = { Text(item.date) },
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
                        }
                    )
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
            Spacer(Modifier.height(16.dp))
        }
    }

}