package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditScreen(
    viewModel: com.emilflach.lokcal.viewmodel.FoodEditViewModel,
    foodId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    val colors = LocalRecipesColors.current

    // Initialize edit state when entering screen
    LaunchedEffect(foodId) {
        viewModel.startEditing(foodId)
    }
    val state by viewModel.edit.collectAsState()

    val isEdit = state.isEdit

    fun save() {
        val id = viewModel.save()
        if (id != null) onSaved()
    }

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
                            viewModel.delete()
                            onDeleted()
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
                modifier = Modifier.fillMaxWidth()
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
    }
}
