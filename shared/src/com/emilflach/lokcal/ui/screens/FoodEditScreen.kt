package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.SingleInputAlertDialog
import com.emilflach.lokcal.ui.dialogs.StealImageDialog
import io.ktor.http.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FoodEditScreen(
    viewModel: com.emilflach.lokcal.viewmodel.FoodEditViewModel,
    foodId: Long?,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val colors = LocalRecipesColors.current
    val uriHandler = LocalUriHandler.current

    BackHandler {
        onBack()
    }

    // Initialize edit state when entering screen
    LaunchedEffect(foodId) {
        viewModel.startEditing(foodId)
    }
    val state by viewModel.edit.collectAsState()
    val isEdit = state.isEdit
    val listState = rememberLazyListState()

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit food" else "Add food") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit) {
                        IconButton(onClick = {
                            viewModel.delete(onDeleted)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
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
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = inner.listContentPadding(),
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.energyText,
                    onValueChange = { viewModel.updateEnergyText(it) },
                    label = { Text("Energy kcal per 100g") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.servingSize,
                    onValueChange = { viewModel.updateServingSize(it) },
                    label = { Text("Serving size") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Text("Optional fields")
                OutlinedTextField(
                    value = state.productUrl,
                    onValueChange = { viewModel.updateProductUrl(it) },
                    label = { Text("Product URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.imageUrl,
                    onValueChange = { viewModel.updateImageUrl(it) },
                    label = { Text("Image URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = {
                                val url =
                                    "https://www.google.com/search?q=${state.name.encodeURLParameter()}&udm=2&tbs=isz:i"
                                uriHandler.openUri(url)
                            }) {
                                Icon(Icons.Default.ImageSearch, contentDescription = "Google Image Search")
                            }
                            IconButton(onClick = { viewModel.openStealDialog() }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Steal image URL")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.gtin13,
                    onValueChange = { viewModel.updateGtin13(it) },
                    label = { Text("GTIN-13") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.source,
                    onValueChange = { viewModel.updateSource(it) },
                    label = { Text("Source") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                // Alias management section (only show when editing existing food)
                if (isEdit && state.id != null) {
                    Spacer(Modifier.height(16.dp))
                    AliasManagementSection(
                        aliases = state.aliases,
                        onAddAlias = { alias, type ->
                            viewModel.addAlias(alias, type)
                        },
                        onDeleteAlias = { aliasId ->
                            viewModel.deleteAlias(aliasId)
                        }
                    )
                }
            }
        }

        if (state.showStealDialog) {
            StealImageDialog(
                onDismissRequest = { viewModel.closeStealDialog() },
                searchQuery = state.stealSearchQuery,
                onSearchQueryChange = { viewModel.setStealSearchQuery(it) },
                results = state.stealResults,
                onItemSelected = { viewModel.stealImage(it) }
            )
        }
    }
}

@Composable
private fun AliasManagementSection(
    aliases: List<com.emilflach.lokcal.FoodAlias>,
    onAddAlias: (String, String) -> Unit,
    onDeleteAlias: (Long) -> Unit
) {
    val colors = LocalRecipesColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Aliases", style = MaterialTheme.typography.titleMedium)
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Add Alias")
            }
        }

        if (aliases.isEmpty()) {
            Text(
                "No aliases yet. Add brand names, translations, or alternative names.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.foregroundSupport
            )
        } else {
            aliases.forEach { alias ->
                AliasCard(
                    alias = alias,
                    onDelete = { onDeleteAlias(alias.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddAliasDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { aliasText ->
                onAddAlias(aliasText, "name")
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AliasCard(
    alias: com.emilflach.lokcal.FoodAlias,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(alias.alias, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete alias")
            }
        }
    }
}

@Composable
private fun AddAliasDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    SingleInputAlertDialog(
        title = "Add Alias",
        fieldLabel = "Alias (brand name, translation, etc.)",
        initialValue = "",
        confirmText = "Add",
        dismissText = "Cancel",
        keyboardType = KeyboardType.Text,
        error = null,
        onConfirm = { value -> if (value.isNotBlank()) onAdd(value.trim()) },
        onDismiss = onDismiss
    )
}
