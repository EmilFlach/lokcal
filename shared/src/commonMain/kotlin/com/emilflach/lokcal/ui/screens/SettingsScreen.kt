package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.backup.BackupManager
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.LocalRecipesColors
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

    BackHandler {
        onBack()
    }

    Scaffold(
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val itemColors = ListItemDefaults.colors(containerColor = colors.backgroundSurface1)

            // Section: Manage
            SettingsSectionHeader("Manage")
            Spacer(Modifier.height(4.dp))
            ListItem(
                headlineContent = { Text("Manage meals") },
                colors = itemColors,
                modifier = Modifier
                    .clip(getRoundedCornerShape(0, 3))
                    .clickable { onOpenMealsList() }
            )
            Spacer(Modifier.height(2.dp))
            ListItem(
                headlineContent = { Text("Manage foods") },
                colors = itemColors,
                modifier = Modifier
                    .clip(getRoundedCornerShape(1, 3))
                    .clickable { onOpenFoodManage() }
            )
            Spacer(Modifier.height(2.dp))
            ListItem(
                headlineContent = { Text("Weight log") },
                colors = itemColors,
                modifier = Modifier
                    .clip(getRoundedCornerShape(2, 3))
                    .clickable { onOpenWeightList() }
            )

            Spacer(Modifier.height(16.dp))

            // Section: Preferences
            SettingsSectionHeader("Preferences")
            Spacer(Modifier.height(4.dp))
            var currentKcal by remember { mutableStateOf(0.0) }
            LaunchedEffect(Unit) {
                currentKcal = settingsRepo.getStartingKcal()
            }
            var showKcalDialog by remember { mutableStateOf(false) }
            var kcalInput by remember(currentKcal) { mutableStateOf(currentKcal.toInt().toString()) }
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
            Spacer(Modifier.height(2.dp))
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
            if (showKcalDialog) {
                AlertDialog(
                    containerColor = colors.backgroundSurface1,
                    onDismissRequest = { showKcalDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val v = kcalInput.trim().toDoubleOrNull()
                            if (v != null && v > 0) {
                                scope.launch {
                                    settingsRepo.setStartingKcal(v)
                                    currentKcal = settingsRepo.getStartingKcal()
                                    showKcalDialog = false
                                }
                            }
                        }) { Text("Save") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showKcalDialog = false }) { Text("Cancel") }
                    },
                    title = { Text("Set starting kcal") },
                    text = {
                        Column {
                            OutlinedTextField(
                                label = { Text("kcal") },
                                value = kcalInput,
                                onValueChange = { kcalInput = it.filter { ch -> ch.isDigit() } },
                                singleLine = true
                            )
                        }
                    }
                )
            }

            // Section: Health (conditional)
            if (HealthManager.showAutomaticExerciseLogging()) {
                Spacer(Modifier.height(16.dp))
                SettingsSectionHeader("Health")
                Spacer(Modifier.height(4.dp))
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

            Spacer(Modifier.height(16.dp))

            // Section: Data
            SettingsSectionHeader("Data")
            Spacer(Modifier.height(4.dp))
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
            Spacer(Modifier.height(2.dp))
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



            // Section: Backup (conditional)
            if (BackupManager.showNightlyBackupSettings()) {
                Spacer(Modifier.height(16.dp))
                SettingsSectionHeader("Backup")
                Spacer(Modifier.height(4.dp))
                var enabled by remember { mutableStateOf(false) }
                var backupLocation by remember { mutableStateOf("Loading...") }
                var hasBackupLocation by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    hasBackupLocation = BackupManager.getBackupDirectory() != null
                    backupLocation = BackupManager.getBackupDirectory() ?: "No directory set"
                    enabled = BackupManager.getNightlyBackup()
                }
                val backupGroupSize = if (hasBackupLocation) 2 else 1
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

            Spacer(Modifier.height(16.dp))
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
