package com.emilflach.lokcal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.FoodRepository
import com.emilflach.lokcal.data.IntakeRepository
import com.emilflach.lokcal.data.SqlDriverFactory
import com.emilflach.lokcal.data.createDatabase
import com.emilflach.lokcal.theme.AppTheme
import com.emilflach.lokcal.util.currentDateIso

private sealed class Screen {
    data object Main : Screen()
    data class Intake(val mealType: String) : Screen()
}

@Composable
internal fun App(sqlDriverFactory: SqlDriverFactory) = AppTheme {
    // Database and repositories
    val database = remember(sqlDriverFactory) { createDatabase(sqlDriverFactory) }
    val foodRepo = remember(database) { FoodRepository(database) }
    val intakeRepo = remember(database) { IntakeRepository(database) }

    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var refreshToggle by remember { mutableStateOf(false) }

    when (val s = screen) {
        Screen.Main -> MainScreen(
            intakeRepo = intakeRepo,
            onOpenMeal = { meal -> screen = Screen.Intake(meal) },
            refreshKey = refreshToggle
        )
        is Screen.Intake -> IntakeScreen(
            initialMealType = s.mealType,
            foodRepo = foodRepo,
            intakeRepo = intakeRepo,
            onDone = { screen = Screen.Main; refreshToggle = !refreshToggle }
        )
    }
}

@Composable
private fun MainScreen(
    intakeRepo: IntakeRepository,
    onOpenMeal: (String) -> Unit,
    refreshKey: Boolean
) {
    val date = currentDateIso()
    val startIso = "${date}T00:00:00"
    val endIso = "${date}T23:59:59"

    val entries = remember(refreshKey) { intakeRepo.getIntakeByDateRange(startIso, endIso) }
    val groups = remember(entries) {
        entries.groupBy { it.meal_type }
    }

    val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Text("Today", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        mealTypes.forEach { type ->
            val list = groups[type].orEmpty()
            val totalKcal = list.sumOf { it.energy_kcal_total }
            val summary = buildSummary(list)
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onOpenMeal(type) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(type.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.titleMedium)
                        Text("${totalKcal.toInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (summary.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(summary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun buildSummary(list: List<com.emilflach.lokcal.Intake>): String {
    if (list.isEmpty()) return ""
    val counts = list.groupingBy { it.item_name }.eachCount().entries
        .sortedByDescending { it.value }
        .take(3)
    return counts.joinToString(", ") { (name, count) -> if (count > 1) "$name x$count" else name }
}

@Composable
private fun IntakeScreen(
    initialMealType: String,
    foodRepo: FoodRepository,
    intakeRepo: IntakeRepository,
    onDone: () -> Unit
) {
    // UI state
    var query by remember { mutableStateOf("") }
    var foods by remember { mutableStateOf(foodRepo.getAll()) }
    var selectedMealType by remember { mutableStateOf(initialMealType) }

    fun refreshSearch() {
        foods = if (query.isBlank()) foodRepo.getAll() else foodRepo.search(query)
    }

    LaunchedEffect(query) { refreshSearch() }

    fun nowIso(): String {
        // Basic ISO timestamp using current local date; time set to noon for simplicity
        return currentDateIso() + "T12:00:00"
    }

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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val types = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
            types.forEach { type ->
                val isSelected = selectedMealType == type
                if (isSelected) {
                    Button(onClick = { selectedMealType = type }) { Text(type.lowercase().replaceFirstChar { it.titlecase() }) }
                } else {
                    OutlinedButton(onClick = { selectedMealType = type }) { Text(type.lowercase().replaceFirstChar { it.titlecase() }) }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search food") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        Text("Results", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(foods) { item ->
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
                        // Parse serving size as grams; fallback to 100g when missing or invalid
                        val portionG: Double = item.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0

                        Button(onClick = {
                            intakeRepo.logFoodIntake(
                                foodId = item.id,
                                quantityG = portionG,
                                timestamp = nowIso(),
                                mealType = selectedMealType,
                                notes = null
                            )
                        }) { Text("1 portion") }

                        OutlinedButton(onClick = {
                            intakeRepo.logFoodIntake(
                                foodId = item.id,
                                quantityG = 20.0,
                                timestamp = nowIso(),
                                mealType = selectedMealType,
                                notes = null
                            )
                        }) { Text("20 g") }

                        OutlinedButton(onClick = {
                            intakeRepo.logFoodIntake(
                                foodId = item.id,
                                quantityG = 100.0,
                                timestamp = nowIso(),
                                mealType = selectedMealType,
                                notes = null
                            )
                        }) { Text("100 g") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
