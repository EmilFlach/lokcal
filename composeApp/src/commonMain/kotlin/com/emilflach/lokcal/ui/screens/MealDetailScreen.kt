package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.viewmodel.MealDetailViewModel

@Composable
fun MealDetailScreen(
    viewModel: MealDetailViewModel,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
) {
    val items by viewModel.items.collectAsState()

    Scaffold(
        floatingActionButtonPosition = FabPosition.Start,
        floatingActionButton = {
            LargeFloatingActionButton(onClick = { onAdd(viewModel.mealType) }) {
                Column (horizontalAlignment = Alignment.CenterHorizontally){
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add portion")
                    Text("Add")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(viewModel.mealType.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack) { Text("Done") }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                Text("No items yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { entry ->
                        var gramsText by remember(entry.id, entry.quantity_g) { mutableStateOf(entry.quantity_g.toString()) }
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(entry.item_name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = gramsText,
                                    onValueChange = { gramsText = it },
                                    label = { Text("Grams") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = {
                                    gramsText.toDoubleOrNull()?.let { g -> viewModel.updateQuantity(entry.id, g) }
                                }) { Text("Save") }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { viewModel.deleteItem(entry.id) }) { Text("Delete") }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${entry.energy_kcal_total.toInt()} kcal", style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}