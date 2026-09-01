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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.backup.BackupManager
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.AppBackHandler
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.SingleInputAlertDialog
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import kotlinx.coroutines.launch
import lokcal.shared.generated.resources.Res
import lokcal.shared.generated.resources.common_cancel
import lokcal.shared.generated.resources.common_disabled
import lokcal.shared.generated.resources.common_enabled
import lokcal.shared.generated.resources.common_kcal_value
import lokcal.shared.generated.resources.common_save
import lokcal.shared.generated.resources.settings_export_database
import lokcal.shared.generated.resources.settings_export_failed
import lokcal.shared.generated.resources.settings_export_failed_desc
import lokcal.shared.generated.resources.settings_export_success
import lokcal.shared.generated.resources.settings_export_success_desc
import lokcal.shared.generated.resources.settings_github_url
import lokcal.shared.generated.resources.settings_health_connected
import lokcal.shared.generated.resources.settings_health_not_connected
import lokcal.shared.generated.resources.settings_import_database
import lokcal.shared.generated.resources.settings_import_failed
import lokcal.shared.generated.resources.settings_import_failed_desc
import lokcal.shared.generated.resources.settings_import_success
import lokcal.shared.generated.resources.settings_import_success_desc
import lokcal.shared.generated.resources.settings_kcal_field_label
import lokcal.shared.generated.resources.settings_manage_exercises
import lokcal.shared.generated.resources.settings_manage_foods
import lokcal.shared.generated.resources.settings_manage_meals
import lokcal.shared.generated.resources.settings_nightly_backup
import lokcal.shared.generated.resources.settings_nightly_backup_directory
import lokcal.shared.generated.resources.settings_no_directory_set
import lokcal.shared.generated.resources.settings_open_source_licenses
import lokcal.shared.generated.resources.settings_search_sources_subtitle
import lokcal.shared.generated.resources.settings_search_sources_title
import lokcal.shared.generated.resources.settings_section_about
import lokcal.shared.generated.resources.settings_section_backup
import lokcal.shared.generated.resources.settings_section_data
import lokcal.shared.generated.resources.settings_section_health
import lokcal.shared.generated.resources.settings_section_manage
import lokcal.shared.generated.resources.settings_section_preferences
import lokcal.shared.generated.resources.settings_set_starting_kcal_title
import lokcal.shared.generated.resources.settings_starting_kcal_title
import lokcal.shared.generated.resources.settings_title
import lokcal.shared.generated.resources.settings_view_on_github
import lokcal.shared.generated.resources.settings_weight_log
import lokcal.shared.generated.resources.common_back
import lokcal.shared.generated.resources.common_enable
import lokcal.shared.generated.resources.common_step_tracking
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMealsList: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenFoodManage: () -> Unit,
    onOpenExerciseManage: () -> Unit,
    onOpenSourcePreferences: () -> Unit,
    onOpenLicenses: () -> Unit,
    onRequestHealthPermissions: () -> Unit,
    settingsRepo: SettingsRepository,
) {
    val colors = LocalRecipesColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    AppBackHandler(onBackCompleted = {
        onBack()
    })

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.common_back))
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
            item { SettingsSectionHeader(stringResource(Res.string.settings_section_manage)) }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_manage_meals)) },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(0, 4))
                        .clickable { onOpenMealsList() }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_manage_foods)) },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(1, 4))
                        .clickable { onOpenFoodManage() }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_manage_exercises)) },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(2, 4))
                        .clickable { onOpenExerciseManage() }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_weight_log)) },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(3, 4))
                        .clickable { onOpenWeightList() }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Section: Preferences
            item { SettingsSectionHeader(stringResource(Res.string.settings_section_preferences)) }
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
                        headlineContent = { Text(stringResource(Res.string.settings_starting_kcal_title)) },
                        supportingContent = {
                            Text(stringResource(Res.string.common_kcal_value, currentKcal.toInt()), color = colors.foregroundSupport)
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
                            title = stringResource(Res.string.settings_set_starting_kcal_title),
                            fieldLabel = stringResource(Res.string.settings_kcal_field_label),
                            initialValue = kcalInput,
                            confirmText = stringResource(Res.string.common_save),
                            dismissText = stringResource(Res.string.common_cancel),
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
                    headlineContent = { Text(stringResource(Res.string.settings_search_sources_title)) },
                    supportingContent = {
                        Text(stringResource(Res.string.settings_search_sources_subtitle), color = colors.foregroundSupport)
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
                item { SettingsSectionHeader(stringResource(Res.string.settings_section_health)) }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    val healthGranted by HealthManager.permissionsGranted.collectAsState()
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.common_step_tracking)) },
                        supportingContent = {
                            Text(
                                if (healthGranted) stringResource(Res.string.settings_health_connected) else stringResource(Res.string.settings_health_not_connected),
                                color = colors.foregroundSupport
                            )
                        },
                        trailingContent = {
                            if (!healthGranted) {
                                Button(onClick = onRequestHealthPermissions) {
                                    Text(stringResource(Res.string.common_enable))
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
            item { SettingsSectionHeader(stringResource(Res.string.settings_section_data)) }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                var exportResult by remember { mutableStateOf<Boolean?>(null) }
                val exportDatabaseText = stringResource(Res.string.settings_export_database)
                val exportSuccessText = stringResource(Res.string.settings_export_success)
                val exportFailedText = stringResource(Res.string.settings_export_failed)
                var exportText by remember { mutableStateOf(exportDatabaseText) }
                LaunchedEffect(exportResult) {
                    exportText = when (exportResult) {
                        null -> exportDatabaseText
                        true -> exportSuccessText
                        false -> exportFailedText
                    }
                }
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (exportResult) {
                                true -> {
                                    Icon(Icons.Default.DownloadDone, tint = colors.foregroundSuccess, contentDescription = stringResource(Res.string.settings_export_success_desc))
                                    Spacer(Modifier.width(8.dp))
                                }
                                false -> {
                                    Icon(Icons.Default.Error, tint = colors.foregroundDanger, contentDescription = stringResource(Res.string.settings_export_failed_desc))
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
                val importDatabaseText = stringResource(Res.string.settings_import_database)
                val importSuccessText = stringResource(Res.string.settings_import_success)
                val importFailedText = stringResource(Res.string.settings_import_failed)
                var importText by remember { mutableStateOf(importDatabaseText) }
                LaunchedEffect(importResult) {
                    importText = when (importResult) {
                        null -> importDatabaseText
                        true -> importSuccessText
                        false -> importFailedText
                    }
                }
                ListItem(
                    headlineContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            when (importResult) {
                                true -> {
                                    Icon(Icons.Default.DownloadDone, tint = colors.foregroundSuccess, contentDescription = stringResource(Res.string.settings_import_success_desc))
                                    Spacer(Modifier.width(8.dp))
                                }
                                false -> {
                                    Icon(Icons.Default.Error, tint = colors.foregroundDanger, contentDescription = stringResource(Res.string.settings_import_failed_desc))
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
                item { SettingsSectionHeader(stringResource(Res.string.settings_section_backup)) }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    var enabled by remember { mutableStateOf(false) }
                    val noDirectorySetText = stringResource(Res.string.settings_no_directory_set)
                    var backupLocation by remember { mutableStateOf("Loading...") }
                    var hasBackupLocation by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        hasBackupLocation = BackupManager.getBackupDirectory() != null
                        backupLocation = BackupManager.getBackupDirectory() ?: noDirectorySetText
                        enabled = BackupManager.getNightlyBackup()
                    }
                    val backupGroupSize = if (hasBackupLocation) 2 else 1

                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(Res.string.settings_nightly_backup_directory)) },
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
                                        backupLocation = BackupManager.getBackupDirectory() ?: noDirectorySetText
                                    }
                                }
                        )
                        if (hasBackupLocation) {
                            Spacer(Modifier.height(2.dp))
                            ListItem(
                                headlineContent = { Text(stringResource(Res.string.settings_nightly_backup)) },
                                supportingContent = {
                                    Text(if (enabled) stringResource(Res.string.common_enabled) else stringResource(Res.string.common_disabled), color = colors.foregroundSupport)
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

            // Section: About
            item { SettingsSectionHeader(stringResource(Res.string.settings_section_about)) }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                val uriHandler = LocalUriHandler.current
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_view_on_github)) },
                    supportingContent = { Text(stringResource(Res.string.settings_github_url), color = colors.foregroundSupport) },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(0, 2))
                        .clickable { uriHandler.openUri("https://github.com/EmilFlach/lokcal") }
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.settings_open_source_licenses)) },
                    colors = itemColors,
                    modifier = Modifier
                        .clip(getRoundedCornerShape(1, 2))
                        .clickable { onOpenLicenses() }
                )
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
