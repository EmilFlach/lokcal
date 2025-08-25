package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.emilflach.lokcal.viewmodel.ExerciseListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val c = LocalRecipesColors.current

    Scaffold(
        topBar = {
            MealTopBar(
                title = "Exercise",
                onBack = onBack,
                showSearch = false,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = c.backgroundBrand,
                contentColor = c.onBackgroundBrand
            ) { Icon(imageVector = Icons.Filled.Add, contentDescription = "Add exercise") }
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
                Text("No exercise yet.", style = MaterialTheme.typography.bodyMedium)
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
                        if (state.summaryText.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(state.summaryText, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        Spacer(Modifier.height(32.dp))
                    }
                    items(state.items, key = { it.id }) { e ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onEdit(e.id) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                val label = when (e.exercise_type) {
                                    "WALKING" -> "Walking"
                                    "RUNNING" -> "Running"
                                    else -> e.exercise_type
                                }
                                Text(label, style = MaterialTheme.typography.titleMedium)
                                Text("${e.duration_min.toInt()} min • ${e.energy_kcal_total.toInt()} kcal", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.delete(e.id) }) {
                                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
