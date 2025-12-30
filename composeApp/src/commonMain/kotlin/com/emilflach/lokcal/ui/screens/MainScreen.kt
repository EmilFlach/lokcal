package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GradientBackground
import com.emilflach.lokcal.ui.components.WeeklyKcalGraph
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenMeal: (String, String) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenStatistics: () -> Unit
) {
    val summaries by viewModel.summaries.collectAsState()
    val percentageLeft by viewModel.percentageLeft.collectAsState()
    val left by viewModel.leftKcal.collectAsState()
    val burned by viewModel.burnedKcal.collectAsState()
    val eaten by viewModel.eatenKcal.collectAsState()
    val startingKcal by viewModel.startingKcal.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val showWeightPrompt by viewModel.showWeightPrompt.collectAsState()
    val last7 by viewModel.last7Deltas.collectAsState()

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

    Box(modifier = Modifier.fillMaxSize()) {
        GradientBackground(percentageLeft.toFloat())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp)
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

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.backgroundPage, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Spacer(Modifier.width(16.dp))

                        Text(
                            text = viewModel.formattedDate(),
                            color = colors.foregroundSupport,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.clickable { viewModel.setToCurrentDate() }
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onOpenWeightList,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Weight log",
                                tint = colors.foregroundSupport,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = onOpenStatistics,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Food statistics",
                                tint = colors.foregroundSupport,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
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
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        Surface(
                            color = Color.Transparent,
                            modifier = Modifier
                                .weight(4f)
                                .fillMaxHeight()
                                .background(colors.backgroundSurface2, MaterialTheme.shapes.medium)
                                .padding(16.dp)
                                .alpha(fadeAlpha.value)
                        ) {
                            Column {
                                Text(
                                    text = if (left > 0) "Remaining" else "Above goal",
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = if (left > 0) "${left.roundToInt()}" else "${left.roundToInt() * -1}",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Left,
                                    color = colors.foregroundDefault,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colors.foregroundBrand.copy(alpha = 0.1f))
                                ) {
                                    val total = (startingKcal + burned).coerceAtLeast(eaten).coerceAtLeast(1.0)
                                    val eatenRatio = (eaten / total).toFloat()
                                    val remainingGoal = (startingKcal - eaten).coerceAtLeast(0.0)
                                    val remainingGoalRatio = (remainingGoal / total).toFloat()
                                    val remainingBurned = (total - eaten - remainingGoal).coerceAtLeast(0.0)
                                    val remainingBurnedRatio = (remainingBurned / total).toFloat()

                                    if (eatenRatio > 0) {
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .weight(eatenRatio)
                                                .background(colors.foregroundBrand)
                                        )
                                    }
                                    if (remainingBurnedRatio > 0) {
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .weight(remainingBurnedRatio)
                                                .background(colors.foregroundBrand.copy(alpha = 0.6f))
                                        )
                                    }
                                    if (remainingGoalRatio > 0) {
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .weight(remainingGoalRatio)
                                                .background(colors.foregroundBrand.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column (
                            Modifier
                                .weight(3f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium.copy(
                                        bottomStart = MaterialTheme.shapes.extraSmall.bottomStart,
                                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd))
                                    .background(colors.backgroundSurface1)
                                    .clickable { onOpenExercise(selectedDate.toString()) }
                                    .padding(horizontal = 16.dp)
                                    .alpha(fadeAlpha.value)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Burned",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.foregroundSupport,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = burned.roundToInt().toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.foregroundSupport
                                    )
                                }
                            }
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium.copy(
                                        topStart = MaterialTheme.shapes.extraSmall.topStart,
                                        topEnd = MaterialTheme.shapes.extraSmall.topEnd))
                                    .background(colors.backgroundSurface1)
                                    .padding(horizontal = 16.dp)
                                    .alpha(fadeAlpha.value)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Eaten",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.foregroundSupport
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        text = eaten.roundToInt().toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = colors.foregroundSupport
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    WeeklyKcalGraph(last7, this@BoxWithConstraints.maxWidth)
                    if (showWeightPrompt) {
                        Spacer(Modifier.height(16.dp))
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
            }



            Spacer(Modifier.weight(1f))

            summaries.forEach { s ->
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .clip(getRoundedCornerShape(index = summaries.indexOf(s), summaries.size))
                        .background(colors.backgroundPage)
                        .clickable { onOpenMeal(s.mealType, selectedDate.toString()) }
                ) {
                    Row(Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = s.mealIcon,
                                    contentDescription = null,
                                    tint = if(s.totalKcal > 0 ) colors.foregroundBrand else colors.foregroundDefault
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    s.mealType.lowercase().replaceFirstChar { it.titlecase() },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (s.summaryText.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = s.summaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(modifier = Modifier.width(80.dp), horizontalAlignment = Alignment.End){
                            Text(
                                "kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foregroundSupport,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${s.totalKcal.roundToInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )

                        }
                    }
                }
            }
        }
    }
}