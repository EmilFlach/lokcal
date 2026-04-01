package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.util.NumberUtils.sanitizeDecimalInput
import com.emilflach.lokcal.viewmodel.MealTimeViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MealTimeScreen(
    viewModel: MealTimeViewModel,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    shouldHighlightLatest: Boolean = false
) {
    val color = LocalRecipesColors.current
    val haptic = LocalHapticFeedback.current

    BackHandler {
        onBack()
    }
    val state by viewModel.state.collectAsState()
    val showSaveMealDialog by viewModel.showSaveMealDialog.collectAsState()

    if (showSaveMealDialog) {
        DualInputAlertDialog(
            title = "Save as meal",
            field1Label = "Name",
            field1Initial = "",
            field1KeyboardType = KeyboardType.Text,
            field2Label = "Total portions",
            field2Initial = "1",
            field2KeyboardType = KeyboardType.Decimal,
            confirmText = "Save",
            dismissText = "Cancel",
            onConfirm = { name, portions ->
                viewModel.saveAsMealFromInputs(name, sanitizeDecimalInput(portions))
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.hideSaveMealDialog()
            },
            onDismiss = {
                viewModel.hideSaveMealDialog()
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
            }
        )
    }
    val requesters = remember { FocusRequesters() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (shouldHighlightLatest) {
            viewModel.loadForSelectedDate(shouldHighlightLatest = true)
        }
    }

    PlatformScaffold(
        topBar = {
            MealTopBar(
                title = viewModel.mealType,
                onBack = onBack,
                showSearch = false,
                trailingActions = {
                    MealTimeTopBarTrailingActions(viewModel, state)
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            MealTimeFab(onAdd = { onAdd(viewModel.mealType) })
        },
        hasFab = true,
        scrollState = listState,
        navBarBackgroundColor = color.backgroundPage
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color.backgroundPage)
                .padding(horizontal = 16.dp)
        ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = paddingValues,
                    state = listState
                ) {
                    item {
                        MealTimeTotalKcal(state.totalKcal.roundToInt())
                    }

                    mealTimeItemsList(
                        items = state.items,
                        viewModel = viewModel
                    )

                    mealTimeSuggestionsSection(
                        title = "Same as yesterday",
                        items = state.yesterdayItems,
                        viewModel = viewModel,
                        requesters = requesters,
                    )

                    mealTimeSuggestionsSection(
                        title = "Leftovers",
                        items = state.leftoversItems,
                        viewModel = viewModel,
                        requesters = requesters,
                        isLeftoverSection = true,
                        keyPrefix = 10_000,
                    )
                }
            }
        }
    }
