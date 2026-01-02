package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
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
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.theme.LocalRecipesColors
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

@Composable
fun MealTimeTopBarTrailingActions(
    viewModel: MealTimeViewModel,
    state: MealTimeViewModel.UiState,
) {
    val haptic = LocalHapticFeedback.current
    val isLeftover = state.isMarkedLeftover

    IconToggleButton(checked = isLeftover, onCheckedChange = {
        viewModel.toggleLeftovers()
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
    }) {
        Icon(
            imageVector = if (isLeftover) Icons.Filled.BookmarkRemove else Icons.Outlined.BookmarkAdd,
            contentDescription = "Toggle leftovers"
        )
    }
    SaveMealAction(viewModel)
}

@Composable
fun MealTimeFab(onAdd: () -> Unit) {
    val color = LocalRecipesColors.current
    FloatingActionButton(
        onClick = onAdd,
        containerColor = color.backgroundBrand,
        contentColor = color.onBackgroundBrand
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add portion")
        }
    }
}

@Composable
fun MealTimeTotalKcal(value: Int) {
    val color = LocalRecipesColors.current
    Column {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$value kcal",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 60.sp,
            textAlign = TextAlign.Center,
            color = color.foregroundDefault,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))
    }
}

fun LazyListScope.mealTimeItemsList(
    items: List<Intake>,
    viewModel: MealTimeViewModel
) {
    items(items, key = { it.id }) { entry ->
        val subtitle = viewModel.subtitleForIntake(entry)
        val isMeal = entry.source_meal_id != null
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val state by viewModel.state.collectAsState()

        MealTimeItem(
            title = entry.item_name,
            subtitle = subtitle,
            index = items.indexOf(entry),
            size = items.size,
            isMeal = isMeal,
            imageUrl = when {
                entry.source_food_id != null -> viewModel.imageUrlForFoodId(entry.source_food_id)
                entry.source_meal_id != null -> viewModel.imageUrlForMealId(entry.source_meal_id)
                else -> null
            },
            highlight = state.highlightedIntakeId == entry.id,
            onHighlighted = { viewModel.clearHighlight() },
            onLongPress = {
                if (isMeal) {
                    entry.source_meal_id.let { viewModel.copyMealItemsIntoMealTime(it) }
                } else {
                    entry.source_food_id?.let {
                        val productUrl = viewModel.productUrlForFoodId(it)
                        productUrl?.let { url ->
                            uriHandler.openUri(uri = url)
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
}

fun LazyListScope.mealTimeSuggestionsSection(
    title: String,
    items: List<Intake>,
    viewModel: MealTimeViewModel,
    requesters: FocusRequesters,
    isLeftoverSection: Boolean = false,
    keyPrefix: Long = 0,
) {
    if (items.isEmpty()) return

    item {
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LocalRecipesColors.current.foregroundDefault
        )
        Spacer(Modifier.height(8.dp))
    }

    itemsIndexed(items = items) { index, intake ->
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val state by viewModel.state.collectAsState()

        if (intake.source_meal_id != null) {
            val mealId = intake.source_meal_id
            val keyId = -mealId - keyPrefix
            val portionG = viewModel.portionForMeal(mealId)
            val portions = if (portionG > 0.0) intake.quantity_g / portionG else 0.0
            
            val initialPortions = state.suggestionInputs[keyId] ?: NumberUtils.formatPortions(portions)
            val portionsText = state.suggestionInputs[keyId] ?: initialPortions
            val portionsVal = NumberUtils.parseDecimal(portionsText)
            val liveGrams = (portionsVal * portionG).coerceAtLeast(0.0)
            val subtitle = viewModel.subtitleForMealSuggestion(mealId, liveGrams)

            IntakeListItem(
                name = intake.item_name,
                subtitle = subtitle,
                keyId = keyId,
                imageUrl = viewModel.imageUrlForMealId(mealId),
                initialValue = initialPortions,
                isMeal = true,
                addButtonDescription = "Add",
                index = index,
                size = items.size,
                requesters = requesters,
                onValueChange = { viewModel.updateSuggestionInput(keyId, it) },
                onAddClick = {
                    val text = state.suggestionInputs[keyId] ?: initialPortions
                    viewModel.addSuggestion(intake, text, isLeftoverSection)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onAddByKeyboard = {
                    val text = state.suggestionInputs[keyId] ?: initialPortions
                    viewModel.addSuggestion(intake, text, isLeftoverSection)
                },
                inputField = { tf, requester, onValueChange, onDone ->
                    PortionsTextField(tf, requester, onValueChange, onDone)
                }
            )
        } else if (intake.source_food_id != null) {
            val foodId = intake.source_food_id
            val keyId = foodId + keyPrefix
            val initialGrams = state.suggestionInputs[keyId] ?: intake.quantity_g.toInt().toString()
            val gramsText = state.suggestionInputs[keyId] ?: initialGrams
            val gramsVal = NumberUtils.parseDecimal(gramsText)
            val subtitle = viewModel.subtitleForFoodSuggestion(foodId, gramsVal)

            IntakeListItem(
                name = intake.item_name,
                subtitle = subtitle,
                keyId = keyId,
                imageUrl = viewModel.imageUrlForFoodId(foodId),
                initialValue = initialGrams,
                addButtonDescription = "Add",
                index = index,
                size = items.size,
                requesters = requesters,
                onValueChange = { viewModel.updateSuggestionInput(keyId, it) },
                onAddClick = {
                    val text = state.suggestionInputs[keyId] ?: initialGrams
                    viewModel.addSuggestion(intake, text, isLeftoverSection)
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onAddByKeyboard = {
                    val text = state.suggestionInputs[keyId] ?: initialGrams
                    viewModel.addSuggestion(intake, text, isLeftoverSection)
                },
                onLongPress = {
                    viewModel.productUrlForFoodId(foodId)?.let { url ->
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
