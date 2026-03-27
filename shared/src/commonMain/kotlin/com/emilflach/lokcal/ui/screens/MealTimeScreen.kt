package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.util.getTopPaddingForNativeNavigation
import com.emilflach.lokcal.util.usesNativeNavigation
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

    BackHandler {
        onBack()
    }
    val state by viewModel.state.collectAsState()
    val requesters = remember { FocusRequesters() }

    LaunchedEffect(Unit) {
        if (shouldHighlightLatest) {
            viewModel.loadForSelectedDate(shouldHighlightLatest = true)
        }
    }

    Scaffold(
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
        }
    ) { paddingValues ->
        val topPadding = getTopPaddingForNativeNavigation()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color.backgroundPage)
                .then(if (!usesNativeNavigation) Modifier.padding(paddingValues) else Modifier)
                .padding(horizontal = 16.dp)
        ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = topPadding, bottom = 80.dp)
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
