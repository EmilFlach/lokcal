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
    onChanged: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        // Top actions kept minimal to maximize space
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Add intake", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onDone) { Text("Done") }
        }
        // Short summary of items already added today for this section — always shown even if zero
        Spacer(Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(8.dp))

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

        // Remove extra spacing and titles to bring results higher
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.foods) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                    // Description intentionally hidden per requirement

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val portionG: Double = item.serving_size?.toDoubleOrNull()?.takeIf { it > 0 } ?: 100.0

                        Button(onClick = { viewModel.logPortion(item.id, portionG); onChanged() }) { Text("1 portion (${portionG.toInt()} g)") }
                        OutlinedButton(onClick = { viewModel.logPortion(item.id, 20.0); onChanged() }) { Text("20 g") }
                        OutlinedButton(onClick = { viewModel.logPortion(item.id, 100.0); onChanged() }) { Text("100 g") }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
