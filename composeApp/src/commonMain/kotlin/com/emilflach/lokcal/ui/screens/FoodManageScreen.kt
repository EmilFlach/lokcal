package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodManageScreen(
    viewModel: com.emilflach.lokcal.viewmodel.FoodEditViewModel,
    onBack: () -> Unit,
    onOpenEdit: (Long?) -> Unit,
) {
    val colors = LocalRecipesColors.current

    val search by viewModel.search.collectAsState()
    val foods by viewModel.foods.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage foods") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenEdit(null) },
                containerColor = colors.backgroundBrand,
                contentColor = colors.onBackgroundBrand
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add food")
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { viewModel.setSearch(it) },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(foods, key = { it.id }) { food ->
                    ListItem(
                        headlineContent = { Text(food.name) },
                        supportingContent = {
                            val kcal = food.energy_kcal_per_100g
                            val servingSize = food.serving_size
                            Text("${kcal.toInt()} kcal • ${servingSize}g")
                        },
                        modifier = Modifier.clickable { onOpenEdit(food.id) }
                    )
                }
            }
        }
    }
}
