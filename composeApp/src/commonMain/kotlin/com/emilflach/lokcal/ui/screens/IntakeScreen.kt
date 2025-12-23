package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.camera.CameraManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GramTextField
import com.emilflach.lokcal.ui.components.IntakeListItem
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.ui.components.PortionsTextField
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import org.ncgroup.kscan.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: () -> Unit,
    autoFocusSearch: Boolean = false,
) {
    val color = LocalRecipesColors.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val keyboard = LocalSoftwareKeyboardController.current
    val state by viewModel.state.collectAsState()
    val showScanner = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MealTopBar(
                title = state.selectedMealType,
                onBack = onDone,
                showSearch = true,
                query = state.query,
                onQueryChange = viewModel::setQuery,
                autoFocusSearch = autoFocusSearch,
                onScanBarcode = {
                    keyboard?.hide()
                    showScanner.value = true
                },
                onSearchOnline = viewModel::searchOnline,
                isSearchingOnline = state.isSearchingOnline,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color.backgroundPage)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            // Cache grams per transient item id; reset when the query changes to avoid stale values
            val gramsById = remember(state.query) { mutableStateMapOf<Long, String>() }
            val requesters = remember { FocusRequesters() }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val showingOnline = state.isSearchingAh || state.isSearchingOff || state.ahFoods.isNotEmpty() || state.offFoods.isNotEmpty()
                if (showingOnline) {
                    // Show AH section first
                    item("ah_header") {
                        Text(
                            text = "Albert Heijn",
                            style = MaterialTheme.typography.titleMedium,
                            color = color.foregroundSupport,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (state.ahFoods.isEmpty()) {
                        if (state.isSearchingAh) {
                            item("ah_loading") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = color.foregroundDefault,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else if (state.ahError != null) {
                            item("ah_error") {
                                val err = state.ahError
                                Text(
                                    text = err ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color.foregroundSupport,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        } else if (state.ahNoResults) {
                            item("ah_empty") {
                                Text(
                                    text = "No matching results found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color.foregroundSupport,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                    itemsIndexed(items = state.ahFoods) { index, item ->
                        val keyId = item.id
                        val initialGrams = gramsById.getOrPut(keyId) {
                            viewModel.defaultPortionGrams(item).toInt().toString()
                        }
                        IntakeListItem(
                            name = item.name,
                            subtitle = viewModel.subtitleForFood(item, initialGrams),
                            keyId = keyId,
                            imageUrl = item.image_url,
                            initialValue = initialGrams,
                            index = index,
                            size = state.ahFoods.size,
                            requesters = requesters,
                            gramsById = gramsById,
                            onAddClick = {
                                viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            onAddByKeyboard = {
                                viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                            },
                            onLongPress = {
                                item.product_url?.let { uriHandler.openUri(it) }
                            },
                            inputField = { tf, requester, onValueChange, onDone ->
                                GramTextField(tf, requester, onValueChange, onDone)
                            }
                        )
                    }

                    // OFF section
                    item("off_header") {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "OpenFoodFacts",
                            style = MaterialTheme.typography.titleMedium,
                            color = color.foregroundSupport,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                    if (state.offFoods.isEmpty()) {
                        if (state.isSearchingOff) {
                            item("off_loading") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = color.foregroundDefault,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else if (state.offError != null) {
                            item("off_error") {
                                val err = state.offError
                                Text(
                                    text = err ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color.foregroundSupport,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        } else if (state.offNoResults) {
                            item("off_empty") {
                                Text(
                                    text = "No matching results found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = color.foregroundSupport,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                    itemsIndexed(items = state.offFoods) { index, item ->
                        val keyId = item.id
                        val initialGrams = gramsById.getOrPut(keyId) {
                            viewModel.defaultPortionGrams(item).toInt().toString()
                        }
                        IntakeListItem(
                            name = item.name,
                            subtitle = viewModel.subtitleForFood(item, initialGrams),
                            keyId = keyId,
                            imageUrl = item.image_url,
                            initialValue = initialGrams,
                            index = index,
                            size = state.offFoods.size,
                            requesters = requesters,
                            gramsById = gramsById,
                            onAddClick = {
                                viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            onAddByKeyboard = {
                                viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                            },
                            onLongPress = {
                                item.product_url?.let { uriHandler.openUri(it) }
                            },
                            inputField = { tf, requester, onValueChange, onDone ->
                                GramTextField(tf, requester, onValueChange, onDone)
                            }
                        )
                    }
                } else {
                    val totalSize = state.meals.size + state.foods.size

                    itemsIndexed(items = state.meals) { index, item ->
                        val keyId = -item.id // Negative to avoid collision with food IDs
                        val initialPortions = gramsById.getOrPut(keyId) { "1" }

                        IntakeListItem(
                            name = item.name,
                            subtitle = viewModel.subtitleForMeal(item, initialPortions),
                            keyId = keyId,
                            initialValue = initialPortions,
                            showBorder = true,
                            index = index,
                            size = totalSize,
                            requesters = requesters,
                            gramsById = gramsById,
                            onAddClick = {
                                viewModel.addMealByPortions(item.id, initialPortions) { onDone() }
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            onAddByKeyboard = {
                                viewModel.addMealByPortions(item.id, initialPortions) { onDone() }
                            },
                            inputField = { tf, requester, onValueChange, onDone ->
                                PortionsTextField(tf, requester, onValueChange, onDone)
                            }
                        )
                    }

                    itemsIndexed(items = state.foods) { index, item ->
                        val keyId = item.id
                        val initialGrams = gramsById.getOrPut(keyId) {
                            viewModel.defaultPortionGrams(item).toInt().toString()
                        }

                        IntakeListItem(
                            name = item.name,
                            subtitle = viewModel.subtitleForFood(item, initialGrams),
                            keyId = keyId,
                            imageUrl = item.image_url,
                            initialValue = initialGrams,
                            index = state.meals.size + index,
                            size = totalSize,
                            requesters = requesters,
                            gramsById = gramsById,
                            onAddClick = {
                                viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            },
                            onAddByKeyboard = {
                                viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                            },
                            onLongPress = {
                                item.product_url?.let {
                                    uriHandler.openUri(item.product_url)
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

    if (showScanner.value) {
        if (CameraManager.arePermissionsGranted()) {
            BackHandler { showScanner.value = false }
            ScannerView(
                codeTypes = listOf(
                    BarcodeFormats.FORMAT_EAN_13,
                ),
                scannerUiOptions = ScannerUiOptions(
                    headerTitle = "Scan barcode",
                    showZoom = false,
                ),
                colors = scannerColors(
                    headerContainerColor = color.backgroundPage,
                    barcodeFrameColor = color.foregroundBrand,
                ),
            ) { result ->
                when (result) {
                    is BarcodeResult.OnSuccess -> {
                        val raw = result.barcode.data
                        val digits = raw.filter { it.isDigit() }
                        if (digits.length == 13) {
                            viewModel.setQuery(digits)
                        } else {
                            viewModel.setQuery(raw)
                        }
                        showScanner.value = false
                    }
                    else -> {
                        showScanner.value = false
                    }
                }
            }
        } else {
            showScanner.value = false
        }
    }
}



@Stable
class FocusRequesters {
    private val map = mutableStateMapOf<Any, FocusRequester>()
    operator fun get(key: Any): FocusRequester = map.getOrPut(key) { FocusRequester() }
    fun request(key: Any) { map[key]?.requestFocus() }
}
