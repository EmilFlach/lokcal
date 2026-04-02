package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.*
import com.emilflach.lokcal.viewmodel.ExerciseListViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseListViewModel,
    onBack: () -> Unit,
    onEnableHealth: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalRecipesColors.current
    val listState = rememberLazyListState()

    val healthSupported = HealthManager.showAutomaticExerciseLogging()
    val healthGranted by HealthManager.permissionsGranted.collectAsState()

    val showHealthBanner = healthSupported && !healthGranted
    val displayedItems = if (healthGranted) {
        state.items
    } else {
        state.items.filter { it.exercise_type != ExerciseRepository.Type.AUTOMATIC_STEPS.dbName }
    }

    BackHandler {
        onBack()
    }

    PlatformScaffold(
        topBar = {
            MealTopBar(
                title = "Exercise",
                onBack = onBack,
                showSearch = false,
            )
        },
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { paddingValues ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = paddingValues.listContentPadding(),
            state = listState
        ) {
            item {
                MealTimeTotalKcal(state.totalKcal.roundToInt())
            }

            if (showHealthBanner) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(getRoundedCornerShape(0, 1))
                            .background(colors.backgroundSurface1)
                            .clickable { onEnableHealth() }
                            .height(IntrinsicSize.Min)
                            .padding(top = 12.dp, bottom = 12.dp, start = 12.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = colors.foregroundSupport,
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(72.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(colors.backgroundSurface2)
                                .padding(horizontal = 10.dp)
                        )
                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Step tracking",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.foregroundDefault,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Connect Health Connect to track steps automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foregroundSupport,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onEnableHealth) {
                            Text(text = "Enable")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            itemsIndexed(displayedItems) { index, e ->
                val actualIndex = if (showHealthBanner) index + 1 else index
                val totalSize = if (showHealthBanner) displayedItems.size + 1 else displayedItems.size
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
                    index = actualIndex,
                    size = totalSize,
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
