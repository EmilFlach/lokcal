package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.camera.CameraManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.util.getTopSafeAreaInset
import com.emilflach.lokcal.util.showBarcodeScanner
import com.emilflach.lokcal.util.usesNativeNavigation
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: (itemAdded: Boolean) -> Unit,
    autoFocusSearch: Boolean = false,
) {
    val color = LocalRecipesColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val state by viewModel.state.collectAsState()

    androidx.compose.ui.backhandler.BackHandler {
        onDone(false)
    }

    var waitingForCameraPermission by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    LaunchedEffect(state.meals, state.foods) {
        if ((state.meals.isNotEmpty() || state.foods.isNotEmpty()) && !showItems) {
            delay(10)
            showItems = true
        }
    }

    Scaffold(
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
                onSearchOnline = viewModel::searchOnline,
                isSearchingOnline = state.isSearchingOnline,
            )
        }
    ) { innerPadding ->
        val topPadding = if (usesNativeNavigation) getTopSafeAreaInset() + 128.dp else 0.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color.backgroundPage)
                .then(if (usesNativeNavigation) Modifier.padding(top = topPadding) else Modifier.padding(innerPadding))
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            val requesters = remember { FocusRequesters() }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
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
                            onSearchOnline = { viewModel.searchOnline() }
                        )
                    }
                } else {
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
