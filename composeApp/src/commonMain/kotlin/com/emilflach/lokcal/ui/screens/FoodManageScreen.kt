package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.FoodEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodManageScreen(
    viewModel: FoodEditViewModel,
    onBack: () -> Unit,
    onOpenEdit: (Long?) -> Unit,
) {
    val color = LocalRecipesColors.current
    val search by viewModel.search.collectAsState()
    val foods by viewModel.foods.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foods") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenEdit(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add food")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = color.backgroundPage,
                    titleContentColor = color.foregroundDefault,
                    navigationIconContentColor = color.foregroundDefault,
                    actionIconContentColor = color.foregroundDefault,
                )
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            TextField(
                value = search,
                onValueChange = { viewModel.setSearch(it) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = color.backgroundSurface1,
                    unfocusedContainerColor = color.backgroundSurface1,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = {
                        viewModel.setSearch("")
                    }, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",

                            )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(foods, key = { it.id }) { food ->
                    ListItem(
                        headlineContent = { Text(food.name) },
                        supportingContent = {
                            val kcal = food.energy_kcal_per_100g
                            val servingSize = food.serving_size
                            Text("${kcal.toInt()} kcal • ${servingSize}g")
                        },
                        modifier = Modifier.clip(getRoundedCornerShape(
                            index = foods.indexOf(food),
                            size = foods.size
                        )).clickable { onOpenEdit(food.id) }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
