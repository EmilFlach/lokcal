package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.util.NumberUtils.sanitizeDecimalInput
import com.emilflach.lokcal.viewmodel.MealTimeViewModel
import kotlin.math.roundToInt
import lokcal.shared.generated.resources.Res
import lokcal.shared.generated.resources.common_cancel
import lokcal.shared.generated.resources.common_name
import lokcal.shared.generated.resources.common_save
import lokcal.shared.generated.resources.common_total_portions_label
import lokcal.shared.generated.resources.meal_time_leftovers
import lokcal.shared.generated.resources.meal_time_same_as_yesterday
import lokcal.shared.generated.resources.meal_time_save_as_meal_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTimeScreen(
    viewModel: MealTimeViewModel,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    shouldHighlightLatest: Boolean = false
) {
    val color = LocalRecipesColors.current
    val haptic = LocalHapticFeedback.current

    AppBackHandler(onBackCompleted = {
        onBack()
    })
    val state by viewModel.state.collectAsState()
    val showSaveMealDialog by viewModel.showSaveMealDialog.collectAsState()

    if (showSaveMealDialog) {
        DualInputAlertDialog(
            title = stringResource(Res.string.meal_time_save_as_meal_title),
            field1Label = stringResource(Res.string.common_name),
            field1Initial = "",
            field1KeyboardType = KeyboardType.Text,
            field2Label = stringResource(Res.string.common_total_portions_label),
            field2Initial = "1",
            field2KeyboardType = KeyboardType.Decimal,
            confirmText = stringResource(Res.string.common_save),
            dismissText = stringResource(Res.string.common_cancel),
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
    val sameAsYesterdayTitle = stringResource(Res.string.meal_time_same_as_yesterday)
    val leftoversTitle = stringResource(Res.string.meal_time_leftovers)

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
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = paddingValues.listContentPadding(),
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
                    title = sameAsYesterdayTitle,
                    items = state.yesterdayItems,
                    viewModel = viewModel,
                    requesters = requesters,
                )

                mealTimeSuggestionsSection(
                    title = leftoversTitle,
                    items = state.leftoversItems,
                    viewModel = viewModel,
                    requesters = requesters,
                    isLeftoverSection = true,
                    keyPrefix = 10_000,
                )
            }
        }

    }
