package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.SourcePreferenceViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SourcePreferenceScreen(
    onBack: () -> Unit,
    viewModel: SourcePreferenceViewModel
) {
    val colors = LocalRecipesColors.current
    val state by viewModel.state.collectAsState()

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundPage,
                    titleContentColor = colors.foregroundDefault,
                    navigationIconContentColor = colors.foregroundDefault,
                )
            )
        },
        containerColor = colors.backgroundPage
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Info text
            Text(
                text = "Select up to 2 sources for online food search. Use the arrows to change priority order in your search results.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.foregroundSupport,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Selected sources section
            val selectedSources = state.sources.filter { it.isSelected }.sortedBy { it.priority }
            if (selectedSources.isNotEmpty()) {
                Text(
                    text = "Selected Sources",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foregroundDefault,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.backgroundSurface1
                    )
                ) {
                    selectedSources.forEach { item ->
                        ListItem(
                            headlineContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.source.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.foregroundDefault
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (item.priority) {
                                            1 -> "PRIMARY"
                                            2 -> "SECONDARY"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (item.priority) {
                                            1 -> colors.foregroundDefault
                                            2 -> colors.foregroundSupport
                                            else -> colors.foregroundDisabled
                                        },
                                        modifier = Modifier
                                            .clip(MaterialTheme.shapes.small)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            },
                            supportingContent = {
                                Text(
                                    text = item.source.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport
                                )
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val canMoveUp = item.priority != null && item.priority > 1
                                    val canMoveDown = item.priority != null && item.priority < selectedSources.size

                                    IconButton(
                                        onClick = {
                                            if (canMoveUp) {
                                                val otherSource = selectedSources.find { it.priority == item.priority - 1 }
                                                if (otherSource != null) {
                                                    viewModel.swapPriority(item.source.id, otherSource.source.id)
                                                }
                                            }
                                        },
                                        enabled = canMoveUp,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move up",
                                            tint = if (canMoveUp) colors.foregroundDefault else colors.foregroundDisabled
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (canMoveDown) {
                                                val otherSource = selectedSources.find { it.priority == item.priority + 1 }
                                                if (otherSource != null) {
                                                    viewModel.swapPriority(item.source.id, otherSource.source.id)
                                                }
                                            }
                                        },
                                        enabled = canMoveDown,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move down",
                                            tint = if (canMoveDown) colors.foregroundDefault else colors.foregroundDisabled
                                        )
                                    }

                                    Checkbox(
                                        checked = true,
                                        onCheckedChange = {
                                            viewModel.toggleSource(item.source.id)
                                        }
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = colors.backgroundSurface1
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Available sources section
            val availableSources = state.sources.filter { !it.isSelected }
            if (availableSources.isNotEmpty()) {
                Text(
                    text = "Available Sources",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foregroundDefault,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colors.backgroundSurface1
                    )
                ) {
                    availableSources.forEach { item ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = item.source.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.foregroundDefault
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = item.source.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport
                                )
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = false,
                                    onCheckedChange = {
                                        viewModel.toggleSource(item.source.id)
                                    },
                                    enabled = state.sources.count { it.isSelected } < 2
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = colors.backgroundSurface1
                            )
                        )
                    }
                }
            }
        }
    }
}
