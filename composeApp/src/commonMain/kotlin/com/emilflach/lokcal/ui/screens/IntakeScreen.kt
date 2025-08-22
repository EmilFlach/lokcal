package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: () -> Unit,
    autoFocusSearch: Boolean = false,
    onChanged: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Top actions kept minimal to maximize space
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add intake", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onDone) { Text("Done") }
        }
        // Summary of items added today for this meal
        Spacer(Modifier.height(8.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = "Added today: ${state.addedCount} • ${state.addedTotalKcal.toInt()} kcal",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.addedSummaryText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(state.addedSummaryText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(autoFocusSearch) {
            if (autoFocusSearch) focusRequester.requestFocus()
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.setQuery(it) },
            label = { Text("Search food") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Keep per-row state in remembered maps keyed by food id
        val gramsById = remember { mutableStateMapOf<Long, String>() }
        val portionsById = remember { mutableStateMapOf<Long, String>() }
        val expandedById = remember { mutableStateMapOf<Long, Boolean>() }
        val usePortionsById = remember { mutableStateMapOf<Long, Boolean>() }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.foods) { item ->
                val defaultPortionG: Double = item.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0
                val initialGrams = gramsById[item.id] ?: defaultPortionG.toInt().toString()
                if (!gramsById.containsKey(item.id)) gramsById[item.id] = initialGrams
                val initialPortions = portionsById[item.id] ?: "1"
                if (!portionsById.containsKey(item.id)) portionsById[item.id] = initialPortions

                // Helpers
                fun parseNonNegativeInt(text: String): Int {
                    val n = text.trim().filter { it.isDigit() }
                    return n.toIntOrNull()?.coerceAtLeast(0) ?: 0
                }
                fun parseGrams(text: String): Double {
                    return text.trim().replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                }

                val gramsValue = parseGrams(gramsById[item.id] ?: initialGrams)
                val portionsValue = parseNonNegativeInt(portionsById[item.id] ?: initialPortions)

                val kcalPer100 = item.energy_kcal_per_100g

                val expanded = expandedById[item.id] ?: false
                val usePortions = usePortionsById[item.id] ?: false

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { expandedById[item.id] = !expanded }
                        .padding(vertical = 8.dp),
                ) {
                    // Compact row: Add 1 portion + two-line info (clickable to expand)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(onClick = {
                            val gramsToAdd = if (usePortions) {
                                portionsValue * defaultPortionG
                            } else {
                                gramsValue
                            }
                            if (gramsToAdd > 0.0) {
                                viewModel.logPortion(item.id, gramsToAdd)
                                onChanged()
                            }
                        }) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add portion")
                        }
                        val tfValue = if (usePortions) (portionsById[item.id] ?: initialPortions) else (gramsById[item.id] ?: initialGrams)
                        BasicTextField(
                            value = tfValue,
                            onValueChange = { newVal ->
                                val cleaned = newVal.filter { it.isDigit() }.take(5)
                                if (usePortions) portionsById[item.id] = cleaned else gramsById[item.id] = cleaned
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    innerTextField()
                                }
                            }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = run {
                                    val totalGrams = if (usePortions) portionsValue * defaultPortionG else gramsValue
                                    val kcal = (kcalPer100 * totalGrams / 100.0)
                                    "${totalGrams.toInt()} g • ${kcal.toInt()} kcal"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (expanded) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column (modifier = Modifier.width(70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(if (usePortions) "Portions" else "Grams", style = MaterialTheme.typography.bodyMedium)
                                IconToggleButton(
                                    checked = usePortions,
                                    onCheckedChange = { usePortionsById[item.id] = it },
                                ) {
                                    Icon(imageVector = Icons.Filled.Scale, contentDescription = "Add portion")
                                }
                            }

                            if (usePortions) {
                                // Portions mode: decrement buttons
                                StepButton(label = "-5") {
                                    val newVal = (portionsValue - 5).coerceAtLeast(0)
                                    portionsById[item.id] = newVal.toString()
                                }
                                StepButton(label = "-2") {
                                    val newVal = (portionsValue - 2).coerceAtLeast(0)
                                    portionsById[item.id] = newVal.toString()
                                }
                                StepButton(label = "-1") {
                                    val newVal = (portionsValue - 1).coerceAtLeast(0)
                                    portionsById[item.id] = newVal.toString()
                                }
                                StepButton(label = "+1") {
                                    val newVal = portionsValue + 1
                                    portionsById[item.id] = newVal.toString()
                                }
                                StepButton(label = "+2") {
                                    val newVal = portionsValue + 2
                                    portionsById[item.id] = newVal.toString()
                                }
                                StepButton(label = "+5") {
                                    val newVal = portionsValue + 5
                                    portionsById[item.id] = newVal.toString()
                                }
                            } else {
                                // Grams mode: decrement/increment buttons
                                StepButton(label = "-100") {
                                    val newVal = (gramsValue - 100).coerceAtLeast(0.0)
                                    gramsById[item.id] = newVal.toInt().toString()
                                }
                                StepButton(label = "-10") {
                                    val newVal = (gramsValue - 10).coerceAtLeast(0.0)
                                    gramsById[item.id] = newVal.toInt().toString()
                                }
                                StepButton(label = "-5") {
                                    val newVal = (gramsValue - 5).coerceAtLeast(0.0)
                                    gramsById[item.id] = newVal.toInt().toString()
                                }
                                StepButton(label = "+5") {
                                    val newVal = (gramsValue + 5)
                                    gramsById[item.id] = newVal.toInt().toString()
                                }
                                StepButton(label = "+10") {
                                    val newVal = (gramsValue + 10)
                                    gramsById[item.id] = newVal.toInt().toString()
                                }
                                StepButton(label = "+100") {
                                    val newVal = (gramsValue + 100)
                                    gramsById[item.id] = newVal.toInt().toString()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
        modifier = Modifier.height(50.dp).width(50.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
