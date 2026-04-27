package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.viewmodel.ExerciseManageViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ExerciseTypeEditScreen(
    viewModel: ExerciseManageViewModel,
    exerciseTypeId: Long?,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val colors = LocalRecipesColors.current

    BackHandler { onBack() }

    LaunchedEffect(exerciseTypeId) {
        viewModel.startEditing(exerciseTypeId)
    }

    val state by viewModel.edit.collectAsState()
    val isEdit = state.isEdit
    val listState = rememberLazyListState()

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit exercise type" else "Add exercise type") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEdit && !state.isBuiltIn) {
                        IconButton(onClick = { viewModel.delete(onDeleted) }) {
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
                    value = ExerciseRepository.displayName(state.name),
                    onValueChange = { viewModel.setName(it) },
                    label = { Text("Name") },
                    singleLine = true,
                    readOnly = state.isBuiltIn,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.kcalText,
                    onValueChange = { viewModel.setKcal(it) },
                    label = { Text("Calories burned per hour (kcal)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.imageUrl,
                    onValueChange = { viewModel.setImageUrl(it) },
                    label = { Text("Image URL (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
