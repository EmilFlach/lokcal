package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import lokcal.shared.generated.resources.Res
import lokcal.shared.generated.resources.search_configure_sources_desc
import lokcal.shared.generated.resources.search_online_query
import lokcal.shared.generated.resources.search_search_online_button
import lokcal.shared.generated.resources.search_setup_sources_subtitle
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchOnlineLink(
    query: String,
    onSearchOnline: () -> Unit,
    modifier: Modifier = Modifier,
    sourcesConfigured: Boolean = true,
) {
    val color = LocalRecipesColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(color.backgroundSurface1)
            .clickable { onSearchOnline() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (sourcesConfigured) stringResource(Res.string.search_online_query, query) else stringResource(Res.string.search_setup_sources_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = color.foregroundDefault,
            modifier = Modifier.weight(1f)
        )

        Surface(
            color = color.backgroundSurface2,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .size(40.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sourcesConfigured) Icons.Default.Search else Icons.Default.Settings,
                    contentDescription = if (sourcesConfigured) stringResource(Res.string.search_search_online_button) else stringResource(Res.string.search_configure_sources_desc),
                    tint = color.foregroundDefault
                )
            }
        }
    }
}
