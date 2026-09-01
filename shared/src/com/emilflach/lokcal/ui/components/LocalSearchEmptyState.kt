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
import lokcal.shared.generated.resources.Res
import lokcal.shared.generated.resources.search_configure_sources_button
import lokcal.shared.generated.resources.search_no_local_results
import lokcal.shared.generated.resources.search_online_instead
import lokcal.shared.generated.resources.search_search_online_button
import lokcal.shared.generated.resources.search_setup_sources_subtitle
import org.jetbrains.compose.resources.stringResource

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
            text = stringResource(Res.string.search_no_local_results),
            style = MaterialTheme.typography.titleMedium,
            color = color.foregroundDefault
        )
        Text(
            text = if (sourcesConfigured) stringResource(Res.string.search_online_instead) else stringResource(Res.string.search_setup_sources_subtitle),
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
            Text(if (sourcesConfigured) stringResource(Res.string.search_search_online_button) else stringResource(Res.string.search_configure_sources_button))
        }
    }
}
