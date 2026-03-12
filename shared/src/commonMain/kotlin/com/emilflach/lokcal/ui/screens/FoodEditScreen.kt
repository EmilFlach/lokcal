package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.dialogs.StealImageDialog
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FoodEditScreen(
    viewModel: com.emilflach.lokcal.viewmodel.FoodEditViewModel,
    foodId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
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

    fun save() {
        viewModel.save(onSaved)
    }

    val scope = rememberCoroutineScope()

    Scaffold(
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
                    } else {
                        IconButton(onClick = {
                            viewModel.openImportDialog()
                        }) {
                            Icon(Icons.Filled.AddLink, contentDescription = "Import food")
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
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { save() },
                containerColor = colors.backgroundBrand,
                contentColor = colors.onBackgroundBrand
            ) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = "Save food")
            }
        }
    ) { inner ->
        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.energyText,
                onValueChange = { viewModel.updateEnergyText(it) },
                label = { Text("Energy kcal per 100g") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
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
                value = state.brandName,
                onValueChange = { viewModel.updateBrandName(it) },
                label = { Text("Brand name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.englishName,
                onValueChange = { viewModel.updateEnglishName(it) },
                label = { Text("English name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.dutchName,
                onValueChange = { viewModel.updateDutchName(it) },
                label = { Text("Dutch name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.productUrl,
                onValueChange = { viewModel.updateProductUrl(it) },
                label = { Text("Product URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.imageUrl,
                onValueChange = { viewModel.updateImageUrl(it) },
                label = { Text("Image URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val url = "https://www.google.com/search?q=${state.name.encodeURLParameter()}&udm=2&tbs=isz:i"
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
            OutlinedTextField(
                value = state.gtin13,
                onValueChange = { viewModel.updateGtin13(it) },
                label = { Text("GTIN-13") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.source,
                onValueChange = { viewModel.updateSource(it) },
                label = { Text("Source") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(64.dp))
        }

        if (state.showUrlDialog) {
            AlertDialog(
                onDismissRequest = { if (!state.isImporting) viewModel.closeImportDialog() },
                confirmButton = {
                    TextButton(onClick = {
                        if (!state.isImporting) {
                            scope.launch { viewModel.importFromUrl(state.urlInput) }
                        }
                    }, enabled = !state.isImporting) {
                        Text(if (state.isImporting) "Importing..." else "Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (!state.isImporting) viewModel.closeImportDialog() }, enabled = !state.isImporting) {
                        Text("Cancel")
                    }
                },
                title = { Text("Import from URL") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.urlInput,
                            onValueChange = { viewModel.setUrlInput(it) },
                            label = { Text("Albert Heijn product URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        val err = state.importError
                        if (err != null) {
                            Text(err, color = colors.backgroundBrand)
                        }
                    }
                }
            )
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
