package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.camera.CameraManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.util.showBarcodeScanner
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: (itemAdded: Boolean) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    autoFocusSearch: Boolean = false,
) {
    val color = LocalRecipesColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val state by viewModel.state.collectAsState()

    BackHandler {
        onDone(false)
    }

    var waitingForCameraPermission by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    LaunchedEffect(state.meals, state.foods) {
        if ((state.meals.isNotEmpty() || state.foods.isNotEmpty()) && !showItems) {
            delay(10.milliseconds)
            showItems = true
        }
    }

    PlatformScaffold(
        topBar = {
            MealTopBar(
                title = state.selectedMealType,
                onBack = { onDone(false) },
                showSearch = true,
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onClearQuery = {
                    coroutineScope.launch {
                        listState.scrollToItem(0)
                    }
                },
                autoFocusSearch = autoFocusSearch,
                onScanBarcode = if (showBarcodeScanner) {
                    {
                        keyboard?.hide()
                        if (CameraManager.arePermissionsGranted()) {
                            viewModel.setShowScanner(true)
                        } else {
                            waitingForCameraPermission = true
                        }
                    }
                } else null,
                onSearchOnline = if (state.sourcesConfigured) viewModel::searchOnline else onNavigateToSettings,
                isSearchingOnline = state.isSearchingOnline,
            )
        },
        scrollState = listState,
        navBarBackgroundColor = color.backgroundPage
    ) { paddingValues ->
        val requesters = remember { FocusRequesters() }
        LazyColumn(
            contentPadding = paddingValues.listContentPadding(),
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            val totalSize = state.meals.size + state.foods.size

            if (state.showOnlineSearchSections) {
                onlineSearchSections(
                    state = state,
                    viewModel = viewModel,
                    requesters = requesters,
                    onDone = onDone
                )
            } else if (state.query.isNotBlank() && state.meals.isEmpty() && state.foods.isEmpty()) {
                item {
                    LocalSearchEmptyState(
                        onSearchOnline = if (state.sourcesConfigured) viewModel::searchOnline else onNavigateToSettings,
                        sourcesConfigured = state.sourcesConfigured,
                    )
                }
            } else {
                item {
                    AnimatedVisibility(state.query.isNotBlank()) {
                        SearchOnlineLink(
                            query = state.query,
                            onSearchOnline = if (state.sourcesConfigured) viewModel::searchOnline else onNavigateToSettings,
                            modifier = Modifier.padding(bottom = 16.dp),
                            sourcesConfigured = state.sourcesConfigured,
                        )
                    }
                }
                itemsIndexed(items = state.meals) { index, item ->
                    AnimatedVisibility(
                        visible = showItems,
                        enter = intakeItemEnterTransition(index)
                    ) {
                        MealIntakeListItem(
                            meal = item,
                            viewModel = viewModel,
                            index = index,
                            size = totalSize,
                            requesters = requesters,
                            onDone = onDone
                        )
                    }
                }

                itemsIndexed(items = state.foods) { index, item ->
                    val globalIndex = state.meals.size + index
                    AnimatedVisibility(
                        visible = showItems,
                        enter = intakeItemEnterTransition(globalIndex)
                    ) {
                        FoodIntakeListItem(
                            food = item,
                            viewModel = viewModel,
                            index = globalIndex,
                            size = totalSize,
                            requesters = requesters,
                            onDone = onDone
                        )
                    }
                }
            }
        }
    }

    ScannerOverlay(
        viewModel = viewModel,
        waitingForCameraPermission = waitingForCameraPermission,
        onPermissionResult = { granted ->
            waitingForCameraPermission = false
            CameraManager.setPermissionsGranted(granted)
        }
    )

    if (state.showScanner) {
        BackHandler { viewModel.setShowScanner(false) }
    }
}

@Composable
private fun intakeItemEnterTransition(index: Int): EnterTransition =
    fadeIn(animationSpec = tween(150, delayMillis = index * 15)) +
        slideInVertically(animationSpec = tween(150, delayMillis = index * 15)) { it / 2 }
