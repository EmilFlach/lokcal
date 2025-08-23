package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val eaten by viewModel.eatenKcal.collectAsState()
    val left by viewModel.leftKcal.collectAsState()
    val progress by viewModel.progress.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        // Massive number up top showing kcal left for the day
        Text("${left.toInt()} kcal left", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(4.dp))
        // Smaller number showing eaten so far
        Text("Eaten: ${eaten.toInt()} kcal", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        // Visualization of progress towards using up kcal
        run {
            val c = com.emilflach.lokcal.theme.LocalRecipesColors.current
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = c.foregroundBrand,
                trackColor = c.backgroundSurface2,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        }

        // Push sections to the bottom
        Spacer(Modifier.weight(1f))

        // Meal sections displayed at the bottom
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
