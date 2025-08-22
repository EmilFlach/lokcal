package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: () -> Unit,
    autoFocusSearch: Boolean = false,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Add intake", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onDone) { Text("Done") }
        }

        Text("Meal type", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        com.emilflach.lokcal.ui.components.MealTypeSelector(
            selected = state.selectedMealType,
            onSelect = { viewModel.selectMealType(it) }
        )

        // Short summary of items already added today for this section
        if (state.addedCount > 0) {
            Spacer(Modifier.height(12.dp))
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = "Added today: ${state.addedCount} • ${state.addedTotalKcal.toInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (state.addedSummaryText.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(state.addedSummaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(autoFocusSearch) {
            if (autoFocusSearch) focusRequester.requestFocus()
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.setQuery(it) },
            label = { Text("Search food") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Results", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.foods) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                    val desc = item.description
                    if (!desc.isNullOrBlank()) {
                        Text(text = desc, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val portionG: Double = item.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0

                        Button(onClick = { viewModel.logPortion(item.id, portionG) }) { Text("1 portion (${portionG.toInt()} g)") }
                        OutlinedButton(onClick = { viewModel.logPortion(item.id, 20.0) }) { Text("20 g") }
                        OutlinedButton(onClick = { viewModel.logPortion(item.id, 100.0) }) { Text("100 g") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
