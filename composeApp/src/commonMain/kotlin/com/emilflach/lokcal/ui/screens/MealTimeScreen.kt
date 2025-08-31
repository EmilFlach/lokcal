package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GramQuantityControls
import com.emilflach.lokcal.ui.components.GramTextField
import com.emilflach.lokcal.ui.components.IntakeListItem
import com.emilflach.lokcal.ui.components.MealTimeItem
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.ui.components.PortionQuantityControls
import com.emilflach.lokcal.ui.components.PortionsTextField
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.util.NumberUtils.sanitizeDecimalInput
import com.emilflach.lokcal.viewmodel.MealTimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveMealAction(viewModel: MealTimeViewModel) {
    var show by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var portions by remember { mutableStateOf("1") }

    androidx.compose.material3.IconButton(onClick = { show = true }) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Save as meal")
    }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text("Save as meal") },
            text = {
                Column {
                    Text(text = "Name")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(text = "Total portions")
                    OutlinedTextField(
                        value = portions,
                        onValueChange = { portions = sanitizeDecimalInput(it) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveAsMealFromInputs(name, portions)
                    show = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { show = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTimeScreen(
    viewModel: MealTimeViewModel,
    onBack: () -> Unit,
    onAdd: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val c = LocalRecipesColors.current
    val gramsById = remember { mutableStateMapOf<Long, String>() }
    val requesters = remember { FocusRequesters() }

    Scaffold(
        topBar = {
            MealTopBar(
                title = viewModel.mealType,
                onBack = onBack,
                showSearch = false,
                trailingActions = {
                    SaveMealAction(viewModel)
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAdd(viewModel.mealType) },
                containerColor = c.backgroundBrand,
                contentColor = c.onBackgroundBrand
            ) {
                Column (horizontalAlignment = Alignment.CenterHorizontally){
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add portion")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalRecipesColors.current.backgroundPage)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.totalKcalLabel,
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 60.sp,
                            textAlign = TextAlign.Center,
                            color = c.foregroundDefault,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                    items(state.items, key = { it.id }) { entry ->
                        val subtitle = viewModel.subtitleForIntake(entry)
                        val isMeal = entry.source_meal_id != null
                        MealTimeItem(
                            title = entry.item_name,
                            subtitle = subtitle,
                            imageUrl = when {
                                entry.source_food_id != null -> viewModel.imageUrlForFoodId(entry.source_food_id)
                                entry.source_meal_id != null -> viewModel.imageUrlForMealId(entry.source_meal_id)
                                else -> null
                            },
                            isMeal = isMeal,
                            onLongPress = {
                                entry.source_meal_id?.let { viewModel.copyMealItemsIntoMealTime(it) }
                            },
                            quantityControls = { requester ->
                                if (isMeal) {
                                    PortionQuantityControls(
                                        requester = requester,
                                        stateKey = Pair(entry.id, entry.quantity_g),
                                        initialGrams = entry.quantity_g,
                                        portionGrams = viewModel.portionForEntry(entry),
                                        onCommitPortions = { portions ->
                                            viewModel.updateQuantityByPortions(entry.id, portions)
                                        },
                                        onDelete = { viewModel.deleteItem(entry.id) }
                                    )
                                } else {
                                    GramQuantityControls(
                                        requester = requester,
                                        stateKey = Pair(entry.id, entry.quantity_g),
                                        initialGrams = entry.quantity_g,
                                        portionGrams = viewModel.portionForEntry(entry),
                                        onCommitGrams = { g -> viewModel.updateQuantity(entry.id, g) },
                                        onDelete = { viewModel.deleteItem(entry.id) }
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    val items = state.yesterdayItems
                    val totalSize = items.size
                    item {
                        if(!items.isEmpty()) {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                text = "Same as yesterday",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.foregroundDefault
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                    }
                    itemsIndexed(items = items) { index, intake ->
                        if (intake.source_meal_id != null) {
                            val mealId = intake.source_meal_id
                            val keyId = -mealId
                            val portionG = viewModel.portionForMeal(mealId)
                            val portions = if (portionG > 0.0) intake.quantity_g / portionG else 0.0
                            val initialPortions = gramsById.getOrPut(keyId) { NumberUtils.formatPortions(portions) }
                            // Live subtitle based on current portions text
                            val portionsText = gramsById[keyId] ?: NumberUtils.formatPortions(portions)
                            val portionsVal = NumberUtils.parseDecimal(portionsText)
                            val liveGrams = (portionsVal * portionG).coerceAtLeast(0.0)
                            val subtitle = viewModel.subtitleForMealSuggestion(mealId, liveGrams)
                            IntakeListItem(
                                name = intake.item_name,
                                subtitle = subtitle,
                                keyId = keyId,
                                initialValue = initialPortions,
                                showBorder = true,
                                addButtonDescription = "Add",
                                index = index,
                                size = totalSize,
                                requesters = requesters,
                                gramsById = gramsById,
                                onAddClick = {
                                    val portionsText = gramsById[keyId] ?: initialPortions
                                    viewModel.addMealSuggestion(mealId, portionsText)
                                },
                                inputField = { tf, requester, onValueChange, onDone ->
                                    PortionsTextField(tf, requester, onValueChange, onDone)
                                }
                            )
                        } else if (intake.source_food_id != null) {
                            val foodId = intake.source_food_id
                            val keyId = foodId
                            val initialGrams = gramsById.getOrPut(keyId) { intake.quantity_g.toInt().toString() }
                            val gramsText = gramsById[keyId] ?: intake.quantity_g.toInt().toString()
                            val gramsVal = NumberUtils.parseDecimal(gramsText)
                            val subtitle = viewModel.subtitleForFoodSuggestion(foodId, gramsVal)
                            IntakeListItem(
                                name = intake.item_name,
                                subtitle = subtitle,
                                keyId = keyId,
                                initialValue = initialGrams,
                                addButtonDescription = "Add",
                                index = index,
                                size = totalSize,
                                requesters = requesters,
                                gramsById = gramsById,
                                onAddClick = {
                                    val gramsText = gramsById[keyId] ?: initialGrams
                                    viewModel.addFoodSuggestion(foodId, gramsText)
                                },
                                inputField = { tf, requester, onValueChange, onDone ->
                                    GramTextField(tf, requester, onValueChange, onDone)
                                }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
