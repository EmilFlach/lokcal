package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.backup.BackupManager
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.theme.LocalRecipesColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMealsList: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenFoodManage: () -> Unit,
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
        ) {
            ListItem(
                headlineContent = { Text("Manage meals") },
                modifier = Modifier.clickable { onOpenMealsList() }
            )
            ListItem(
                headlineContent = { Text("Manage foods") },
                modifier = Modifier.clickable { onOpenFoodManage() }
            )
            ListItem(
                headlineContent = { Text("Weight log") },
                modifier = Modifier.clickable { onOpenWeightList() }
            )

            // Starting kcal setting
            var currentKcal by remember { mutableStateOf(settingsRepo.getStartingKcal()) }
            var showKcalDialog by remember { mutableStateOf(false) }
            var kcalInput by remember { mutableStateOf(currentKcal.toInt().toString()) }
            ListItem(
                headlineContent = { Text("Starting kcal") },
                supportingContent = { Text("${currentKcal.toInt()} kcal") },
                modifier = Modifier.clickable {
                    kcalInput = currentKcal.toInt().toString()
                    showKcalDialog = true
                }
            )
            if (showKcalDialog) {
                AlertDialog(
                    containerColor = colors.backgroundSurface1,
                    onDismissRequest = { showKcalDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            val v = kcalInput.trim().toDoubleOrNull()
                            if (v != null && v > 0) {
                                settingsRepo.setStartingKcal(v)
                                currentKcal = settingsRepo.getStartingKcal()
                                showKcalDialog = false
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

            var exportResult by remember { mutableStateOf<Boolean?>(null) }
            var exportText by remember { mutableStateOf("Export database") }
            LaunchedEffect(exportResult) {
                exportText = when (exportResult) {
                    null -> "Export database"
                    true -> "✅ Database export successful"
                    false -> "❌ Database export failed"
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            ListItem(
                headlineContent = { Text(exportText) },
                modifier = Modifier.clickable {
                    scope.launch {
                        exportResult = null
                        exportResult = BackupManager.exportDatabase()
                    }
                }
            )

            var importResult by remember { mutableStateOf<Boolean?>(null) }
            var importText by remember { mutableStateOf("Import database") }
            LaunchedEffect(importResult) {
                importText = when (importResult) {
                    null -> "Import database"
                    true -> "✅ Database import successful"
                    false -> "❌ Database import failed"
                }
            }
            ListItem(
                headlineContent = { Text(importText) },
                modifier = Modifier.clickable {
                    scope.launch {
                        importResult = null
                        importResult = BackupManager.importDatabase()
                    }
                }
            )

            if(BackupManager.showNightlyBackupSettings()) {
                Spacer(modifier = Modifier.height(16.dp))
                var enabled by remember { mutableStateOf(false) }
                var backupLocation by remember { mutableStateOf("Loading...") }
                var hasBackupLocation by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    hasBackupLocation = BackupManager.getBackupDirectory() != null
                    backupLocation = BackupManager.getBackupDirectory() ?: "No directory set"
                    enabled = BackupManager.getNightlyBackup()
                }
                ListItem(
                    headlineContent = { Text("Nightly backup directory") },
                    supportingContent = { Text(backupLocation) },
                    modifier = Modifier.clickable {
                        scope.launch {
                            BackupManager.setBackupDirectory()
                            hasBackupLocation = BackupManager.getBackupDirectory() != null
                            backupLocation = BackupManager.getBackupDirectory() ?: "No directory set"
                        }
                    }
                )
                if(hasBackupLocation) {
                    ListItem(
                        headlineContent = { Text("Nightly backup") },
                        supportingContent = { Text(if(enabled) "Enabled" else "Disabled") },
                        trailingContent = {
                            Switch(checked = enabled, onCheckedChange = { value ->
                                BackupManager.setNightlyBackup(value)
                                scope.launch {
                                    enabled = BackupManager.getNightlyBackup()
                                }
                            })
                        }
                    )
                }

            }
        }
    }
}
