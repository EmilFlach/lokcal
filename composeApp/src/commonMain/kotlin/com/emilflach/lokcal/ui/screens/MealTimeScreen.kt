package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.util.NumberUtils.sanitizeDecimalInput
import com.emilflach.lokcal.viewmodel.MealTimeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveMealAction(viewModel: MealTimeViewModel) {
    val color = LocalRecipesColors.current
    val haptic = LocalHapticFeedback.current
    var showAlert by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var portions by remember { mutableStateOf("1") }

    IconButton(onClick = { showAlert = true }) {
        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save as meal")
    }

    if (showAlert) {
        AlertDialog(
            containerColor = color.backgroundSurface1,
            onDismissRequest = { showAlert = false },
            title = { Text("Save as meal") },
            text = {
                Column {
                    Text(text = "Name")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                    )
                    Spacer(Modifier.height(32.dp))
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
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    showAlert = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAlert = false
                    haptic.performHapticFeedback(HapticFeedbackType.Reject)
                }) { Text("Cancel") }
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
    val color = LocalRecipesColors.current
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    val gramsById = remember { mutableStateMapOf<Long, String>() }
    val requesters = remember { FocusRequesters() }

    Scaffold(
        topBar = {
            MealTopBar(
                title = viewModel.mealType,
                onBack = onBack,
                showSearch = false,
                trailingActions = {
                    // Toggle leftovers marker for this meal/date
                    val isLeftover = state.isMarkedLeftover
                    IconToggleButton(checked = isLeftover, onCheckedChange = {
                        viewModel.toggleLeftovers()
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    }) {
                        Icon(
                            imageVector = if(isLeftover) Icons.Filled.BookmarkRemove else Icons.Outlined.BookmarkAdd,
                            contentDescription = "Toggle leftovers")
                    }
                    SaveMealAction(viewModel)
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAdd(viewModel.mealType) },
                containerColor = color.backgroundBrand,
                contentColor = color.onBackgroundBrand
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
                            color = color.foregroundDefault,
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
                            index = state.items.indexOf(entry),
                            size = state.items.size,
                            imageUrl = when {
                                entry.source_food_id != null -> viewModel.imageUrlForFoodId(entry.source_food_id)
                                entry.source_meal_id != null -> viewModel.imageUrlForMealId(entry.source_meal_id)
                                else -> null
                            },
                            onLongPress = {
                                if (isMeal) {
                                    entry.source_meal_id.let { viewModel.copyMealItemsIntoMealTime(it) }
                                } else {
                                    entry.source_food_id?.let {
                                        val productUrl = viewModel.productUrlForFoodId(it)
                                        productUrl?.let { url ->
                                            uriHandler.openUri(
                                                uri = url,
                                            )
                                        }
                                    }
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            quantityControls = { requester ->
                                if (isMeal) {
                                    PortionQuantityControls(
                                        requester = requester,
                                        stateKey = entry.id,
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
                                        stateKey = entry.id,
                                        initialGrams = entry.quantity_g,
                                        portionGrams = viewModel.portionForEntry(entry),
                                        onCommitGrams = { g ->
                                            viewModel.updateQuantity(entry.id, g)
                                        },
                                        onDelete = { viewModel.deleteItem(entry.id) }
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(2.dp))
                    }

                    val items = state.yesterdayItems
                    val totalSize = items.size
                    item {
                        if(!items.isEmpty()) {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                text = "Same as yesterday",
                                style = MaterialTheme.typography.titleMedium,
                                color = color.foregroundDefault
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
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onAddByKeyboard = {
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
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onAddByKeyboard = {
                                    val gramsText = gramsById[keyId] ?: initialGrams
                                    viewModel.addFoodSuggestion(foodId, gramsText)
                                },
                                onLongPress = {
                                    val food = viewModel.productUrlForFoodId(foodId)
                                    food?.let { url ->
                                        uriHandler.openUri(url)
                                    }
                                },
                                inputField = { tf, requester, onValueChange, onDone ->
                                    GramTextField(tf, requester, onValueChange, onDone)
                                }
                            )
                        }
                    }

                    val leftovers = state.leftoversItems
                    val leftoversSize = leftovers.size
                    item {
                        if (!leftovers.isEmpty()) {
                            Spacer(Modifier.height(32.dp))
                            Text(
                                text = "Leftovers",
                                style = MaterialTheme.typography.titleMedium,
                                color = color.foregroundDefault
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    itemsIndexed(items = leftovers) { index, intake ->
                        if (intake.source_meal_id != null) {
                            val mealId = intake.source_meal_id
                            val keyId = -mealId - 10_000 // ensure unique from yesterday section
                            val portionG = viewModel.portionForMeal(mealId)
                            val portions = if (portionG > 0.0) intake.quantity_g / portionG else 0.0
                            val initialPortions = gramsById.getOrPut(keyId) { NumberUtils.formatPortions(portions) }
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
                                size = leftoversSize,
                                requesters = requesters,
                                gramsById = gramsById,
                                onAddClick = {
                                    val portionsText2 = gramsById[keyId] ?: initialPortions
                                    viewModel.addMealSuggestionFromLeftover(mealId, portionsText2, intake)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onAddByKeyboard = {
                                    val portionsText2 = gramsById[keyId] ?: initialPortions
                                    viewModel.addMealSuggestionFromLeftover(mealId, portionsText2, intake)
                                },
                                inputField = { tf, requester, onValueChange, onDone ->
                                    PortionsTextField(tf, requester, onValueChange, onDone)
                                }
                            )
                        } else if (intake.source_food_id != null) {
                            val foodId = intake.source_food_id
                            val keyId = foodId + 10_000 // ensure unique
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
                                size = leftoversSize,
                                requesters = requesters,
                                gramsById = gramsById,
                                onAddClick = {
                                    val gramsText2 = gramsById[keyId] ?: initialGrams
                                    viewModel.addFoodSuggestionFromLeftover(foodId, gramsText2, intake)
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                },
                                onAddByKeyboard = {
                                    val gramsText2 = gramsById[keyId] ?: initialGrams
                                    viewModel.addFoodSuggestionFromLeftover(foodId, gramsText2, intake)
                                },
                                onLongPress = {
                                    val food = viewModel.productUrlForFoodId(foodId)
                                    food?.let { url ->
                                        uriHandler.openUri(url)
                                    }
                                },
                                inputField = { tf, requester, onValueChange, onDone ->
                                    GramTextField(tf, requester, onValueChange, onDone)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
