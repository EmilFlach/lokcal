package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import lokcal.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val colors = LocalRecipesColors.current
    val listState = rememberLazyListState()

    BackHandler { onBack() }

    val libraries by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }

    val sortedLibraries = remember(libraries) {
        libraries?.libraries?.sortedBy { it.name } ?: emptyList()
    }

    var selectedLibrary by remember { mutableStateOf<Library?>(null) }

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open source licenses") },
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
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { padding ->
        val itemColors = ListItemDefaults.colors(containerColor = colors.backgroundSurface1)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding.listContentPadding(),
        ) {
            item {
                Text(
                    text = "Lokcal is built using these open-source projects. Thank you to all the contributors who made their work freely available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.foregroundSupport,
                )
                Spacer(Modifier.height(16.dp))
            }
            itemsIndexed(sortedLibraries, key = { _, lib -> lib.uniqueId }) { index, library ->
                val licenseNames = library.licenses.joinToString(", ") { it.name }
                ListItem(
                    headlineContent = { Text(library.name) },
                    supportingContent = if (licenseNames.isNotEmpty()) {
                        { Text(licenseNames, color = colors.foregroundSupport) }
                    } else null,
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(index, sortedLibraries.size))
                        .clickable { selectedLibrary = library }
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }

    selectedLibrary?.let { lib ->
        val licenseText = lib.licenses.joinToString("\n\n---\n\n") { lic ->
            buildString {
                append(lic.name)
                lic.licenseContent?.let { append("\n\n$it") }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedLibrary = null },
            title = { Text(lib.name) },
            text = {
                Text(
                    text = licenseText.ifEmpty { "No license text available." },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { selectedLibrary = null }) {
                    Text("Close")
                }
            }
        )
    }
}
