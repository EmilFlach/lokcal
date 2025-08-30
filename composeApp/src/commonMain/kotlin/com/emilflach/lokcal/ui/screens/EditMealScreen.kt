package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GramQuantityControls
import com.emilflach.lokcal.ui.components.MealTimeItem
import com.emilflach.lokcal.viewmodel.EditMealViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealScreen(
    viewModel: EditMealViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalRecipesColors.current

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
                        viewModel.deleteMeal()
                        onDeleted()
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
                modifier = Modifier.fillMaxWidth()
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
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

