package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.IntakeViewModel

fun LazyListScope.onlineSearchSections(
    state: IntakeViewModel.UiState,
    viewModel: IntakeViewModel,
    requesters: FocusRequesters,
    onDone: (itemAdded: Boolean) -> Unit
) {
    state.sourceSections.forEachIndexed { index, section ->
        searchSection(
            title = section.sourceName,
            section = section,
            viewModel = viewModel,
            requesters = requesters,
            onDone = onDone
        )
        if (index < state.sourceSections.size - 1) {
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
    if (state.showGlobalNoResults) {
        item {
            GlobalNoResults()
        }
    }
}

@Composable
fun GlobalNoResults() {
    val color = LocalRecipesColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No results found online",
            style = MaterialTheme.typography.titleMedium,
            color = color.foregroundDefault
        )
        Text(
            text = "Try a different search term",
            style = MaterialTheme.typography.bodyMedium,
            color = color.foregroundSupport,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
