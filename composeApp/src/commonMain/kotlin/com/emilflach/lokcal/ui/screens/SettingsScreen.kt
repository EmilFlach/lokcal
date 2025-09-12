package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.backup.BackupManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMealsList: () -> Unit,
    onOpenWeightList: () -> Unit,
) {
    val colors = LocalRecipesColors.current
    val scope = rememberCoroutineScope()


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
                headlineContent = { Text("Weight") },
                supportingContent = { Text("View and log your weight") },
                modifier = Modifier.clickable { onOpenWeightList() }
            )
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
                var enabled by remember { mutableStateOf(BackupManager.getNightlyBackup()) }
                var backupLocation by remember { mutableStateOf("Loading...") }
                var hasBackupLocation by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    hasBackupLocation = BackupManager.getBackupDirectory() != null
                    backupLocation = BackupManager.getBackupDirectory() ?: "No directory set"
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
                                enabled = BackupManager.getNightlyBackup()
                            })
                        }
                    )
                }

            }
        }
    }
}
