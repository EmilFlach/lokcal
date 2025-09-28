package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.viewmodel.ExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Add exercise") }, navigationIcon = {}, scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Type")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ExerciseRepository.Type.WALKING, ExerciseRepository.Type.RUNNING).forEach { t ->
                    OutlinedCard(
                        modifier = Modifier,
                        onClick = { viewModel.setType(t) }
                    ) {
                        Text(
                            text = when (t) {
                                ExerciseRepository.Type.WALKING -> "Walking"
                                ExerciseRepository.Type.RUNNING -> "Running"
                                ExerciseRepository.Type.AUTOMATIC_STEPS -> "Automatic Steps"
                            },
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.minutesText,
                onValueChange = viewModel::setMinutesText,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Minutes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Will burn approx. ${state.kcalPreview.toInt()} kcal")
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                viewModel.save()
                onSaved()
            }) {
                Text("Add exercise")
            }
        }
    }
}
