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
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.util.EntityImageData
import com.emilflach.lokcal.util.NumberUtils
import com.emilflach.lokcal.viewmodel.MealTimeViewModel

@Composable
fun SaveMealAction(viewModel: MealTimeViewModel) {
    IconButton(onClick = { viewModel.showSaveMealDialog() }) {
        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save as meal")
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
    items: List<MealTimeViewModel.IntakeUiState>,
    viewModel: MealTimeViewModel
) {
    items(items, key = { it.intake.id }) { ui ->
        val entry = ui.intake
        val isMeal = entry.source_meal_id != null
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val state by viewModel.state.collectAsState()

        MealTimeItem(
            title = entry.item_name,
            subtitle = ui.subtitle,
            index = items.indexOf(ui),
            size = items.size,
            isMeal = isMeal,
            imageUrl = ui.imageUrl,
            imageEntity = if (!isMeal && entry.source_food_id != null)
                EntityImageData(EntityImageData.FOOD, entry.source_food_id, ui.imageUrl ?: "")
            else if (isMeal)
                EntityImageData(EntityImageData.MEAL, entry.source_meal_id, ui.imageUrl ?: "")
            else null,
            highlight = state.highlightedIntakeId == entry.id,
            onHighlighted = { viewModel.clearHighlight() },
            onLongPress = {
                if (isMeal) {
                    entry.source_meal_id.let { viewModel.copyMealItemsIntoMealTime(it) }
                } else {
                    ui.productUrl?.let { url ->
                        uriHandler.openUri(uri = url)
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
                        portionGrams = ui.portionGrams,
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
                        portionGrams = ui.portionGrams,
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
    items: List<MealTimeViewModel.SuggestionUiState>,
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

    itemsIndexed(items = items) { index, ui ->
        val intake = ui.intake
        val haptic = LocalHapticFeedback.current
        val uriHandler = LocalUriHandler.current
        val state by viewModel.state.collectAsState()

        if (intake.source_meal_id != null) {
            val mealId = intake.source_meal_id
            val keyId = -mealId - keyPrefix
            val portionG = ui.portionGrams
            val portions = if (portionG > 0.0) intake.quantity_g / portionG else 0.0
            
            val initialPortions = state.suggestionInputs[keyId] ?: NumberUtils.formatPortions(portions)
            val portionsText = state.suggestionInputs[keyId] ?: initialPortions
            val portionsVal = NumberUtils.parseDecimal(portionsText)
            val liveGrams = (portionsVal * portionG).coerceAtLeast(0.0)
            
            var subtitle by remember(mealId, liveGrams) { mutableStateOf("") }
            LaunchedEffect(mealId, liveGrams) {
                subtitle = viewModel.subtitleForMealSuggestion(mealId, liveGrams)
            }

            IntakeListItem(
                name = intake.item_name,
                subtitle = subtitle,
                keyId = keyId,
                imageUrl = ui.imageUrl,
                imageEntity = EntityImageData(EntityImageData.MEAL, mealId, ui.imageUrl ?: ""),
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
            
            var subtitle by remember(foodId, gramsVal) { mutableStateOf("") }
            LaunchedEffect(foodId, gramsVal) {
                subtitle = viewModel.subtitleForFoodSuggestion(foodId, gramsVal)
            }

            IntakeListItem(
                name = intake.item_name,
                subtitle = subtitle,
                keyId = keyId,
                imageUrl = ui.imageUrl,
                imageEntity = EntityImageData(EntityImageData.FOOD, foodId, ui.imageUrl ?: ""),
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
                    ui.productUrl?.let { url ->
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
