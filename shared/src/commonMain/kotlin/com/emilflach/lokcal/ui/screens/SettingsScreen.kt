package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.backup.BackupManager
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.SingleInputAlertDialog
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMealsList: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenFoodManage: () -> Unit,
    onOpenSourcePreferences: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    settingsRepo: SettingsRepository,
) {
    val colors = LocalRecipesColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    BackHandler {
        onBack()
    }

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.backgroundPage,
                    titleContentColor = colors.foregroundDefault,
                    navigationIconContentColor = colors.foregroundDefault,
                    actionIconContentColor = colors.foregroundDefault,
                )
            )
        },
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { padding ->
        val itemColors = ListItemDefaults.colors(containerColor = colors.backgroundSurface1)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding.listContentPadding(),
            state = listState
        ) {
            // Section: Manage
            item { SettingsSectionHeader("Manage") }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Manage meals") },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(0, 3))
                        .clickable { onOpenMealsList() }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Manage foods") },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(1, 3))
                        .clickable { onOpenFoodManage() }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Weight log") },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(2, 3))
                        .clickable { onOpenWeightList() }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Section: Preferences
            item { SettingsSectionHeader("Preferences") }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                var currentKcal by remember { mutableStateOf(0.0) }
                LaunchedEffect(Unit) {
                    currentKcal = settingsRepo.getStartingKcal()
                }
                var showKcalDialog by remember { mutableStateOf(false) }
                var kcalInput by remember(currentKcal) { mutableStateOf(currentKcal.toInt().toString()) }

                Column {
                    ListItem(
                        headlineContent = { Text("Starting kcal") },
                        supportingContent = {
                            Text("${currentKcal.toInt()} kcal", color = colors.foregroundSupport)
                        },
                        colors = itemColors,
                        modifier = Modifier
                            .clip(getRoundedCornerShape(0, 2))
                            .clickable {
                                kcalInput = currentKcal.toInt().toString()
                                showKcalDialog = true
                            }
                    )
                    if (showKcalDialog) {
                        SingleInputAlertDialog(
                            title = "Set starting kcal",
                            fieldLabel = "kcal",
                            initialValue = kcalInput,
                            confirmText = "Save",
                            dismissText = "Cancel",
                            keyboardType = KeyboardType.Number,
                            error = null,
                            onConfirm = { value ->
                                val v = value.trim().toDoubleOrNull()
                                if (v != null && v > 0) {
                                    scope.launch {
                                        settingsRepo.setStartingKcal(v)
                                        currentKcal = settingsRepo.getStartingKcal()
                                        showKcalDialog = false
                                    }
                                }
                            },
                            onDismiss = { showKcalDialog = false }
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Search sources") },
                    supportingContent = {
                        Text("Configure online food search sources", color = colors.foregroundSupport)
                    },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(1, 2))
                        .clickable { onOpenSourcePreferences() }
                )
            }

            // Section: Health (conditional)
            if (HealthManager.showAutomaticExerciseLogging()) {
                item { Spacer(Modifier.height(16.dp)) }
                item { SettingsSectionHeader("Health") }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    val healthGranted by HealthManager.permissionsGranted.collectAsState()
                    ListItem(
                        headlineContent = { Text("Step tracking") },
                        supportingContent = {
                            Text(
                                if (healthGranted) "Connected via Health Connect" else "Not connected",
                                color = colors.foregroundSupport
                            )
                        },
                        trailingContent = {
                            if (!healthGranted) {
                                Button(onClick = onRequestHealthPermissions) {
                                    Text("Enable")
                                }
                            }
                        },
                        colors = itemColors,
                        modifier = Modifier.clip(getRoundedCornerShape(0, 1))
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Section: Data
            item { SettingsSectionHeader("Data") }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                var exportResult by remember { mutableStateOf<Boolean?>(null) }
                var exportText by remember { mutableStateOf("Export database") }
                LaunchedEffect(exportResult) {
                    exportText = when (exportResult) {
                        null -> "Export database"
                        true -> "Database export successful"
                        false -> "Database export failed"
                    }
                }
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (exportResult) {
                                true -> {
                                    Icon(Icons.Default.DownloadDone, tint = colors.foregroundSuccess, contentDescription = "Export successful")
                                    Spacer(Modifier.width(8.dp))
                                }
                                false -> {
                                    Icon(Icons.Default.Error, tint = colors.foregroundDanger, contentDescription = "Export failed")
                                    Spacer(Modifier.width(8.dp))
                                }
                                else -> {}
                            }
                            Text(exportText)
                        }
                    },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(0, 2))
                        .clickable {
                            scope.launch {
                                exportResult = null
                                exportResult = BackupManager.exportDatabase()
                            }
                        }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                var importResult by remember { mutableStateOf<Boolean?>(null) }
                var importText by remember { mutableStateOf("Import database") }
                LaunchedEffect(importResult) {
                    importText = when (importResult) {
                        null -> "Import database"
                        true -> "Database import successful"
                        false -> "Database import failed"
                    }
                }
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (importResult) {
                                true -> {
                                    Icon(Icons.Default.DownloadDone, tint = colors.foregroundSuccess, contentDescription = "Import successful")
                                    Spacer(Modifier.width(8.dp))
                                }
                                false -> {
                                    Icon(Icons.Default.Error, tint = colors.foregroundDanger, contentDescription = "Import failed")
                                    Spacer(Modifier.width(8.dp))
                                }
                                else -> {}
                            }
                            Text(importText)
                        }
                    },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(1, 2))
                        .clickable {
                            scope.launch {
                                importResult = null
                                importResult = BackupManager.importDatabase()
                            }
                        }
                )
            }

            // Section: Backup (conditional)
            if (BackupManager.showNightlyBackupSettings()) {
                item { Spacer(Modifier.height(16.dp)) }
                item { SettingsSectionHeader("Backup") }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    var enabled by remember { mutableStateOf(false) }
                    var backupLocation by remember { mutableStateOf("Loading...") }
                    var hasBackupLocation by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        hasBackupLocation = BackupManager.getBackupDirectory() != null
                        backupLocation = BackupManager.getBackupDirectory() ?: "No directory set"
                        enabled = BackupManager.getNightlyBackup()
                    }
                    val backupGroupSize = if (hasBackupLocation) 2 else 1

                    Column {
                        ListItem(
                            headlineContent = { Text("Nightly backup directory") },
                            supportingContent = {
                                Text(backupLocation, color = colors.foregroundSupport)
                            },
                            colors = itemColors,
                            modifier = Modifier
                                .clip(getRoundedCornerShape(0, backupGroupSize))
                                .clickable {
                                    scope.launch {
                                        BackupManager.setBackupDirectory()
                                        hasBackupLocation = BackupManager.getBackupDirectory() != null
                                        backupLocation = BackupManager.getBackupDirectory() ?: "No directory set"
                                    }
                                }
                        )
                        if (hasBackupLocation) {
                            Spacer(Modifier.height(2.dp))
                            ListItem(
                                headlineContent = { Text("Nightly backup") },
                                supportingContent = {
                                    Text(if (enabled) "Enabled" else "Disabled", color = colors.foregroundSupport)
                                },
                                trailingContent = {
                                    Switch(checked = enabled, onCheckedChange = { value ->
                                        BackupManager.setNightlyBackup(value)
                                        scope.launch {
                                            enabled = BackupManager.getNightlyBackup()
                                        }
                                    })
                                },
                                colors = itemColors,
                                modifier = Modifier.clip(getRoundedCornerShape(1, 2))
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = LocalRecipesColors.current
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colors.foregroundSupport,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
