package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.MealTimeItem
import com.emilflach.lokcal.ui.components.MealTimeTotalKcal
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.ui.components.MinuteQuantityControls
import com.emilflach.lokcal.viewmodel.ExerciseListViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalRecipesColors.current

    Scaffold(
        topBar = {
            MealTopBar(
                title = "Exercise",
                onBack = onBack,
                showSearch = false,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundPage)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    MealTimeTotalKcal(state.totalKcal.roundToInt())
                }

                itemsIndexed(state.items) { index, e ->
                    val label = when (e.exercise_type) {
                        ExerciseRepository.Type.AUTOMATIC_STEPS.dbName -> "Automatic step counter"
                        ExerciseRepository.Type.WALKING.dbName -> "Low intensity"
                        ExerciseRepository.Type.RUNNING.dbName -> "High intensity"
                        else -> e.exercise_type
                    }
                    val image = when (e.exercise_type) {
                        ExerciseRepository.Type.AUTOMATIC_STEPS.dbName -> Icons.Default.AutoGraph
                        ExerciseRepository.Type.WALKING.dbName -> Icons.AutoMirrored.Filled.DirectionsWalk
                        ExerciseRepository.Type.RUNNING.dbName -> Icons.AutoMirrored.Filled.DirectionsRun
                        else -> null
                    }

                    MealTimeItem(
                        title = label,
                        subtitle = "${e.energy_kcal_total.toInt()} kcal",
                        index = index,
                        size = state.items.size,
                        iconName = image,
                        quantityControls = { requester ->
                            MinuteQuantityControls(
                                requester = requester,
                                stateKey = e.exercise_type,
                                initialMinutes = e.duration_min,
                                onCommitMinutes = { viewModel.updateDuration(e.exercise_type, it) }
                            )
                        }
                    )
                }
            }
        }
    }
}
