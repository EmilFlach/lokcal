package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@Composable
fun LocalSearchEmptyState(
    onSearchOnline: () -> Unit,
    sourcesConfigured: Boolean = true,
) {
    val color = LocalRecipesColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No local results found",
            style = MaterialTheme.typography.titleMedium,
            color = color.foregroundDefault
        )
        Text(
            text = if (sourcesConfigured) "Search online instead?" else "Set up your online search sources",
            style = MaterialTheme.typography.bodyMedium,
            color = color.foregroundSupport,
            modifier = Modifier.padding(top = 4.dp)
        )
        Button(
            onClick = onSearchOnline,
            modifier = Modifier.padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color.backgroundSurface2,
                contentColor = color.foregroundDefault
            )
        ) {
            Text(if (sourcesConfigured) "Search online" else "Configure sources")
        }
    }
}
