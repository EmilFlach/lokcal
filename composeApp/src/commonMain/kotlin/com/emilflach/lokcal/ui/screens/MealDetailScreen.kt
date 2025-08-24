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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.viewmodel.MealDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    viewModel: MealDetailViewModel,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val c = LocalRecipesColors.current

    Scaffold(
        topBar = {
            MealTopBar(
                title = viewModel.mealType,
                onBack = onBack,
                showSearch = false,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAdd(viewModel.mealType) },
                containerColor = c.backgroundBrand,
                contentColor = c.onBackgroundBrand
            ) {
                Column (horizontalAlignment = Alignment.CenterHorizontally){
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add portion")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalRecipesColors.current.backgroundPage)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (state.items.isEmpty()) {
                Text("No items yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "${state.totalKcal.toInt()} kcal",
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 60.sp,
                            textAlign = TextAlign.Center,
                            color = c.foregroundDefault,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(32.dp))
                    }
                    items(state.items, key = { it.id }) { entry ->
                        com.emilflach.lokcal.ui.components.MealDetailItem(
                            entry = entry,
                            viewModel = viewModel,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}