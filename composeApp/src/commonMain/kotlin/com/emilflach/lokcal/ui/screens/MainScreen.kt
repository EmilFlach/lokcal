package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenMeal: (String) -> Unit,
) {
    val summaries by viewModel.summaries.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.loadToday()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Text("Today", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        summaries.forEach { s ->
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onOpenMeal(s.mealType) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.mealType.lowercase().replaceFirstChar { it.titlecase() }, style = MaterialTheme.typography.titleMedium)
                        Text("${s.totalKcal.toInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (s.summaryText.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(s.summaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
