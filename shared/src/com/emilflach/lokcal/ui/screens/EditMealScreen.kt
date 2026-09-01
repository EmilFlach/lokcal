package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.AppBackHandler
import com.emilflach.lokcal.ui.components.GramQuantityControls
import com.emilflach.lokcal.ui.components.MealTimeItem
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.dialogs.AddFoodDialog
import com.emilflach.lokcal.ui.dialogs.StealImageDialog
import com.emilflach.lokcal.ui.util.EntityImageData
import com.emilflach.lokcal.viewmodel.EditMealViewModel
import io.ktor.http.*
import lokcal.shared.generated.resources.Res
import lokcal.shared.generated.resources.common_add_food
import lokcal.shared.generated.resources.common_back
import lokcal.shared.generated.resources.common_google_image_search_desc
import lokcal.shared.generated.resources.common_image_url_label
import lokcal.shared.generated.resources.common_name
import lokcal.shared.generated.resources.common_steal_image_desc
import lokcal.shared.generated.resources.common_total_portions_label
import lokcal.shared.generated.resources.edit_meal_delete_desc
import lokcal.shared.generated.resources.edit_meal_items_header
import lokcal.shared.generated.resources.edit_meal_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealScreen(
    viewModel: EditMealViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalRecipesColors.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    AppBackHandler(onBackCompleted = {
        onBack()
    })

    PlatformScaffold(
        topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.edit_meal_title, state.name)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.deleteMeal(onDeleted)
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(Res.string.edit_meal_delete_desc))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundPage,
                        titleContentColor = colors.foregroundDefault,
                        navigationIconContentColor = colors.foregroundDefault,
                        actionIconContentColor = colors.foregroundDefault,
                    )
                )
            },
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(colors.backgroundPage),
            state = listState,
            contentPadding = inner.listContentPadding(),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    singleLine = true,
                    label = { Text(stringResource(Res.string.common_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.imageUrl,
                    onValueChange = viewModel::setImageUrl,
                    singleLine = true,
                    label = { Text(stringResource(Res.string.common_image_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val url = "https://www.google.com/search?q=${state.name.encodeURLParameter()}&udm=2&tbs=isz:i"
                                uriHandler.openUri(url)
                            }) {
                                Icon(Icons.Default.ImageSearch, contentDescription = stringResource(Res.string.common_google_image_search_desc))
                            }
                            IconButton(onClick = { viewModel.openStealDialog() }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(Res.string.common_steal_image_desc))
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.totalPortions,
                    onValueChange = viewModel::setTotalPortionsText,
                    singleLine = true,
                    label = { Text(stringResource(Res.string.common_total_portions_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(Res.string.edit_meal_items_header), style = MaterialTheme.typography.titleMedium, color = colors.foregroundDefault)
                Spacer(Modifier.height(8.dp))
            }
            items(state.items, key = { it.mealItemId }) { item ->
                val subtitle = viewModel.subtitleForFood(item.food, item.quantityG)

                MealTimeItem(
                    title = item.food.name,
                    subtitle = subtitle,
                    index = state.items.indexOf(item),
                    size = state.items.size,
                    imageUrl = item.food.image_url,
                    imageEntity = EntityImageData(EntityImageData.FOOD, item.food.id, item.food.image_url ?: ""),
                    quantityControls = { requester ->
                        GramQuantityControls(
                            requester = requester,
                            stateKey = item.mealItemId,
                            initialGrams = item.quantityG,
                            portionGrams = viewModel.defaultPortionGrams(item.food),
                            onCommitGrams = { g ->
                                viewModel.updateItemQuantity(item.mealItemId, g)
                            },
                            onDelete = {
                                viewModel.deleteItem(item.mealItemId)
                            }
                        )
                    }
                )
                Spacer(Modifier.height(2.dp))
            }
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.openAddFoodDialog() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.common_add_food))
                }
            }
        }

        if (state.showAddFoodDialog) {
            AddFoodDialog(
                onDismissRequest = { viewModel.closeAddFoodDialog() },
                searchQuery = state.addFoodQuery,
                onSearchQueryChange = { viewModel.setAddFoodQuery(it) },
                results = state.addFoodResults,
                subtitleForFood = { food -> viewModel.subtitleForFood(food, viewModel.defaultPortionGrams(food)) },
                onFoodSelected = { viewModel.addFood(it) }
            )
        }

        if (state.showStealDialog) {
            StealImageDialog(
                onDismissRequest = { viewModel.closeStealDialog() },
                searchQuery = state.stealSearchQuery,
                onSearchQueryChange = { viewModel.setStealSearchQuery(it) },
                results = state.stealResults,
                onItemSelected = { viewModel.stealImage(it) }
            )
        }
    }
}
