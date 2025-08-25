package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.viewmodel.EditExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    viewModel: EditExerciseViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(title = { Text("Edit exercise") }, scrollBehavior = scrollBehavior)
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
                        onClick = { viewModel.setType(t) }
                    ) {
                        Text(
                            text = when (t) {
                                ExerciseRepository.Type.WALKING -> "Walking"
                                ExerciseRepository.Type.RUNNING -> "Running"
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
                label = { Text("Minutes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("Will burn approx. ${state.kcalPreview.toInt()} kcal")
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { viewModel.save(); onSaved() }) { Text("Save") }
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.delete(); onDeleted() }) { Text("Delete") }
            }
        }
    }
}
