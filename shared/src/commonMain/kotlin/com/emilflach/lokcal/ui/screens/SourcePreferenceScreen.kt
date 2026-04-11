package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
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

    PlatformScaffold(
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
            Text(
                text = "Open Food Facts is always included. You can optionally add one regional source — its results will appear first.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.foregroundSupport,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Optional regional sources
            Text(
                text = "Regional source",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.foregroundDefault,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.backgroundSurface1)
            ) {
                val noneSelected = state.selectedSourceId == "none"
                ListItem(
                    modifier = Modifier.clickable { viewModel.selectNone() },
                    headlineContent = {
                        Text(
                            text = "None",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.foregroundDefault
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Use Open Food Facts only",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foregroundSupport
                        )
                    },
                    trailingContent = {
                        RadioButton(selected = noneSelected, onClick = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = colors.backgroundSurface1)
                )
                HorizontalDivider(color = colors.backgroundPage, thickness = 1.dp)
                state.optionalSources.forEach { source ->
                    val isSelected = state.selectedSourceId == source.id
                    ListItem(
                        modifier = Modifier.clickable {
                            viewModel.selectSource(source.id)
                        },
                        headlineContent = {
                            Text(
                                text = source.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.foregroundDefault
                            )
                        },
                        supportingContent = {
                            Text(
                                text = source.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foregroundSupport
                            )
                        },
                        trailingContent = { RadioButton(selected = isSelected, onClick = null) },
                        colors = ListItemDefaults.colors(containerColor = colors.backgroundSurface1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Always-included section
            Text(
                text = "Always included",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.foregroundDefault,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.backgroundSurface1)
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = "Open Food Facts",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.foregroundDefault
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "Global open food database",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foregroundSupport
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = true,
                            onClick = null,
                            enabled = false
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = colors.backgroundSurface1)
                )
            }
        }
    }
}
