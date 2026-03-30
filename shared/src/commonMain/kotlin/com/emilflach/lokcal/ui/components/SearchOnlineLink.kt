package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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

@Composable
fun SearchOnlineLink(
    query: String,
    onSearchOnline: () -> Unit,
    modifier: Modifier = Modifier
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Search for \"$query\" online",
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
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search online",
                    tint = color.foregroundDefault
                )
            }
        }
    }
}
