package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.meshGradient
import com.emilflach.lokcal.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenMeal: (String, String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWeightToday: () -> Unit,
) {
    val summaries by viewModel.summaries.collectAsState()
    val percentageLeft by viewModel.percentageLeft.collectAsState()
    val left by viewModel.leftKcal.collectAsState()
    val burned by viewModel.burnedKcal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val showWeightPrompt by viewModel.showWeightPrompt.collectAsState()

    val density = LocalDensity.current
    val colors = LocalRecipesColors.current
    val thresholdPx = remember(density) { with(density) { 64.dp.toPx() } }
    val coroutineScope = rememberCoroutineScope()
    var animationTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.fetchAndLogHealthData()
    }

    DisposableEffect(Unit) {
        val job = coroutineScope.launch {
            while (isActive) {
                delay(10000)
                viewModel.fetchAndLogHealthData()
                animationTrigger++
            }
        }
        onDispose {
            job.cancel()
        }
    }

    val gradientPosition = remember(left) { percentageLeft.coerceIn(0.1, 0.95).toFloat() }
    val middleGradientColor = remember(left) { if (left > 0) colors.backgroundSurface1 else colors.backgroundDangerSubtle}
    val bottomGradientColor = remember(left) { if (left > 0) colors.backgroundSurface1 else colors.backgroundPage}
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .fillMaxSize()
            .meshGradient(
                points = listOf(
                    listOf(
                        Offset(0f, 0f) to colors.backgroundPage,
                        Offset(.5f, 0f) to colors.backgroundPage,
                        Offset(1f, 0f) to colors.backgroundPage,
                    ),
                    listOf(
                        Offset(0f, .8f) to colors.backgroundPage,
                        Offset(.5f, gradientPosition) to middleGradientColor,
                        Offset(1f, .8f) to colors.backgroundPage,
                    ),
                    listOf(
                        Offset(0f, 1f) to bottomGradientColor,
                        Offset(.5f, 1f) to bottomGradientColor,
                        Offset(1f, 1f) to bottomGradientColor,
                    ),
                ),
                resolutionX = 100,
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp)
                .pointerInput(Unit) {
                    var accumX = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                accumX > thresholdPx -> viewModel.previousDay()
                                accumX < -thresholdPx -> viewModel.nextDay()
                            }
                            accumX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            accumX += dragAmount
                        }
                    )
                }
        ) {
            // Selected date label
            Column (
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundPage, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = selectedDate.toString(),
                        color = colors.foregroundSupport,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = colors.foregroundSupport,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Spacer(Modifier.height(12.dp))

                val fadeAlpha = remember { Animatable(1f) }
                LaunchedEffect(animationTrigger) {
                    if (animationTrigger > 0) {
                        fadeAlpha.animateTo(
                            targetValue = 0.5f,
                            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                        )
                        fadeAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 300, easing = LinearEasing)
                        )

                    }
                }
                Row {
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(4f)
                            .background(colors.backgroundSurface2, MaterialTheme.shapes.medium)
                            .padding(16.dp)
                            .alpha(fadeAlpha.value)
                        ,
                    ) {
                        Column {
                            Text(
                                text = if (left > 0) "kcal left" else "kcal over",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = if (left > 0) "${left.toInt()}" else "${left.toInt() * -1}",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Left,
                                color = colors.foregroundDefault,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .weight(3f)
                            .background(colors.backgroundSurface1, MaterialTheme.shapes.medium)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onOpenExercise(selectedDate.toString()) }
                            .padding(16.dp)
                            .alpha(fadeAlpha.value)
                    )  {
                        Column {
                            Text(
                                text = "kcal burned",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.foregroundSupport,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = burned.toInt().toString(),
                                style = MaterialTheme.typography.displayLarge,
                                color = colors.foregroundSupport,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                    }
                }

                Spacer(Modifier.height(8.dp))

                // Thursday prompt to log weight (from view model)
                if (showWeightPrompt) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = colors.backgroundBrand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.backgroundPage)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { onOpenWeightToday() }
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                            Text("It's Thursday, log your weight!", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            summaries.forEach { s ->
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .padding(vertical = 6.dp)
                        .background(colors.backgroundPage)

                        .let { modifier ->
                            if (s.totalKcal.toInt() > 0) {
                                modifier.drawBehind {
                                    val borderWidth = 2.dp.toPx()
                                    drawRect(
                                        color = colors.backgroundBrand,
                                        topLeft = Offset(0f, 0f),
                                        size = Size(borderWidth, this.size.height)
                                    )
                                }
                            } else {
                                modifier
                            }
                        }
                        .clickable { onOpenMeal(s.mealType, selectedDate.toString()) }
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                s.mealType.lowercase().replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${s.totalKcal.toInt()} kcal",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        if (s.summaryText.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = s.summaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foregroundSupport,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}