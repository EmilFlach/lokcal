package com.emilflach.lokcal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.SqlDriverFactory
import com.emilflach.lokcal.data.createDatabase
import com.emilflach.lokcal.theme.AppTheme

@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    // Database and repository
    val database = remember(sqlDriverFactory) { createDatabase(sqlDriverFactory) }
    val repository = remember(database) { FoodRepository(database) }

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var foods by remember { mutableStateOf(repository.getAll()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val trimmed = name.trim()
                if (trimmed.isNotEmpty()) {
                    repository.insert(trimmed, description.takeIf { it.isNotBlank() })
                    // Refresh list and clear inputs
                    foods = repository.getAll()
                    name = ""
                    description = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add Food")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Foods", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(foods) { item ->
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                    val desc = item.description
                    if (!desc.isNullOrBlank()) {
                        Text(text = desc, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            }
        }
    }
}
