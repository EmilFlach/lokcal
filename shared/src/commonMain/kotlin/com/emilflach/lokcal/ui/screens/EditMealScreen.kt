package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GramQuantityControls
import com.emilflach.lokcal.ui.components.MealTimeItem
import com.emilflach.lokcal.ui.dialogs.StealImageDialog
import com.emilflach.lokcal.viewmodel.EditMealViewModel
import io.ktor.http.encodeURLParameter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EditMealScreen(
    viewModel: EditMealViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalRecipesColors.current
    val uriHandler = LocalUriHandler.current

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit ${state.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteMeal(onDeleted)
                    }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete meal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundPage,
                    titleContentColor = colors.foregroundDefault,
                    navigationIconContentColor = colors.foregroundDefault,
                    actionIconContentColor = colors.foregroundDefault,
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundPage)
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.imageUrl,
                onValueChange = viewModel::setImageUrl,
                singleLine = true,
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val url = "https://www.google.com/search?q=${state.name.encodeURLParameter()}&udm=2&tbs=isz:i"
                            uriHandler.openUri(url)
                        }) {
                            Icon(Icons.Default.ImageSearch, contentDescription = "Google Image Search")
                        }
                        IconButton(onClick = { viewModel.openStealDialog() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Steal image URL")
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.totalPortions,
                onValueChange = viewModel::setTotalPortionsText,
                singleLine = true,
                label = { Text("Total portions") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Items", style = MaterialTheme.typography.titleMedium, color = colors.foregroundDefault)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(state.items, key = { it.mealItemId }) { item ->
                    val subtitle = viewModel.subtitleForFood(item.food, item.quantityG)

                    MealTimeItem(
                        title = item.food.name,
                        subtitle = subtitle,
                        index = state.items.indexOf(item),
                        size = state.items.size,
                        imageUrl = item.food.image_url,
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
            }
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

