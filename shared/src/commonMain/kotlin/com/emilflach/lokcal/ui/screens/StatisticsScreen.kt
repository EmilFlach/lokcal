package com.emilflach.lokcal.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.StatsMostEatenByKcal
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.theme.RecipesColors
import com.emilflach.lokcal.ui.components.InfoAlertDialog
import com.emilflach.lokcal.ui.components.PlatformPadding
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.StatisticsViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.*
import ir.ehsannarmani.compose_charts.models.DrawStyle.Stroke

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit,
    onOpenDemo: (() -> Unit)? = null
) {
    BackHandler { onBack() }

    val isLoading by viewModel.isLoading.collectAsState()
    val topFoods by viewModel.topFoods.collectAsState()
    val dailyKcal by viewModel.dailyKcal.collectAsState()
    val daysFilled by viewModel.daysFilled.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val period by viewModel.period.collectAsState()
    val weightData by viewModel.weightData.collectAsState()
    val allWeightEntries by viewModel.allWeightEntries.collectAsState()
    val colors = LocalRecipesColors.current
    val listState = rememberLazyListState()

    PlatformScaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.backgroundPage)
            )
        },
        containerColor = colors.backgroundPage,
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { paddingValues ->
        StatisticsBody(
            isLoading = isLoading,
            topFoods = topFoods,
            dailyKcal = dailyKcal,
            daysFilled = daysFilled,
            insights = insights,
            period = period,
            weightData = weightData,
            allWeightEntries = allWeightEntries,
            onPeriodChange = { viewModel.setPeriod(it) },
            onOpenDemo = onOpenDemo,
            paddingValues = paddingValues,
            listState = listState
        )
    }
}

@Composable
internal fun StatisticsBody(
    isLoading: Boolean = false,
    topFoods: List<StatsMostEatenByKcal>,
    dailyKcal: List<StatisticsViewModel.DailyKcal>,
    daysFilled: Long,
    insights: StatisticsViewModel.Insights?,
    period: StatisticsViewModel.Period,
    weightData: List<WeightLog>,
    allWeightEntries: List<WeightLog>,
    onPeriodChange: (StatisticsViewModel.Period) -> Unit,
    onOpenDemo: (() -> Unit)? = null,
    paddingValues: PlatformPadding,
    listState: LazyListState
) {
    if (isLoading) return
    val colors = LocalRecipesColors.current
    val showOnboarding = daysFilled < 7 || allWeightEntries.isEmpty()
    var activeTooltip by remember { mutableStateOf<Pair<String, String>?>(null) }
    val showTooltip: (String, String) -> Unit = { title, body -> activeTooltip = title to body }

    // Tooltip dialog — rendered outside LazyColumn so it floats on top
    activeTooltip?.let { (title, body) ->
        InfoAlertDialog(
            title = title,
            body = body,
            confirmText = "Got it",
            onDismiss = { activeTooltip = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = paddingValues.listContentPadding(),
        state = listState
    ) {

        // Onboarding card
        if (showOnboarding) {
            item {
                OnboardingCard(
                    daysFilled = daysFilled,
                    hasWeightData = allWeightEntries.isNotEmpty(),
                    onOpenDemo = onOpenDemo,
                    colors = colors
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        // Period selector
        if (dailyKcal.isNotEmpty() || insights != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.backgroundSurface1)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatisticsViewModel.Period.entries.forEach { p ->
                        val selected = period == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) colors.foregroundBrand else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(
                                onClick = { onPeriodChange(p) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) Color.White else colors.foregroundSupport
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = when (p) {
                                        StatisticsViewModel.Period.MONTH -> "30 days"
                                        StatisticsViewModel.Period.THREE_MONTHS -> "90 days"
                                        StatisticsViewModel.Period.ALL -> "All time"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // Summary card
        insights?.let { ins ->
            item {
                val net = ins.avgNetIntake
                val budget = ins.dailyBudget
                val delta = (net - budget).toInt()
                val overBudget = delta > 100
                val underBudget = delta < -100
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSurface1
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Daily averages",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foregroundSupport
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    "Eaten kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${ins.avgEaten.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foregroundDefault
                                )
                            }
                            if (ins.avgBurned > 10) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Burned kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.foregroundSupport
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${ins.avgBurned.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.foregroundSuccess
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Net kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.foregroundSupport
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${net.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.foregroundDefault
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    TooltipIcon(
                                        title = "Net intake",
                                        body = "Calories eaten minus calories burned through exercise. This is what actually drives weight change — not your raw eaten total.",
                                        onShow = showTooltip,
                                        colors = colors
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            when {
                                underBudget -> "Net is ${-delta} kcal below your ${budget.toInt()} kcal goal"
                                overBudget  -> "Net is $delta kcal above your ${budget.toInt()} kcal goal"
                                else        -> "Net is on target (${budget.toInt()} kcal goal)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                overBudget  -> colors.foregroundDanger
                                underBudget -> colors.foregroundSuccess
                                else        -> colors.foregroundSupport
                            }
                        )
                        ins.impliedMaintenanceKcal?.let { maint ->
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = colors.foregroundSupport.copy(alpha = 0.1f))
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Maintenance calories",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.foregroundSupport
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    TooltipIcon(
                                        title = "Maintenance calories",
                                        body = "The calorie level at which your weight stays constant, estimated from your real data. If your weight is stable, your net intake equals your maintenance.",
                                        onShow = showTooltip,
                                        colors = colors
                                    )
                                }
                                Text(
                                    "~${maint.toInt()} kcal/day",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.foregroundDefault
                                )
                            }
                        }
                        ins.weightTrendKgPerWeek?.let { trend ->
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = colors.foregroundSupport.copy(alpha = 0.1f))
                            Spacer(Modifier.height(14.dp))
                            val totalChange = ins.impliedTotalWeightChangeKg ?: 0.0
                            val trendText = when {
                                trend > StatisticsViewModel.TREND_THRESHOLD_KG_PER_WEEK || totalChange > StatisticsViewModel.TOTAL_CHANGE_THRESHOLD_KG  -> "↑ Gaining ${trend.roundToTwoDecimals()} kg/week"
                                trend < -StatisticsViewModel.TREND_THRESHOLD_KG_PER_WEEK || totalChange < -StatisticsViewModel.TOTAL_CHANGE_THRESHOLD_KG -> "↓ Losing ${(-trend).roundToTwoDecimals()} kg/week"
                                else -> "→ Stable (${if (trend >= 0) "+" else ""}${trend.roundToTwoDecimals()} kg/week)"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Weight trend",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.foregroundSupport
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    TooltipIcon(
                                        title = "Weight trend",
                                        body = "How much your weight is changing per week, based on a trend line through all your weigh-ins. More reliable than comparing just your first and last measurement.",
                                        onShow = showTooltip,
                                        colors = colors
                                    )
                                }
                                Text(
                                    trendText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        trend > 0.3  -> colors.foregroundDanger
                                        trend < -0.3 -> colors.foregroundSuccess
                                        else         -> colors.foregroundDefault
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }

        // Recommendation card
        insights?.let { ins ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSurface1
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "What this means",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.foregroundSupport
                            )
                            Spacer(Modifier.width(3.dp))
                            TooltipIcon(
                                title = "How this works",
                                body = "Based on your net calorie intake and actual weight change over the selected period. The more consistently you log, the more accurate this becomes.",
                                onShow = showTooltip,
                                colors = colors
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            ins.recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.foregroundDefault
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // Net intake + weight overlay chart
        if (dailyKcal.isNotEmpty()) {
            item {
                val netIntakeValues = dailyKcal.map { it.eaten - it.burned }
                val smoothedNetIntake = remember(netIntakeValues) { rollingAverage(netIntakeValues, 7) }
                val days = dailyKcal.map { it.day }
                val budget = insights?.dailyBudget ?: 0.0
                val minNet = smoothedNetIntake.minOrNull() ?: 0.0
                val maxNet = smoothedNetIntake.maxOrNull() ?: 2000.0
                val pad = (maxNet - minNet).coerceAtLeast(200.0) * 0.15
                val chartMin = minOf(minNet, budget) - pad
                val chartMax = maxOf(maxNet, budget) + pad
                val graphTextStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundSupport)

                val interpolatedWeights = remember(weightData, days) {
                    interpolateWeightsForDays(weightData, days)
                }
                val smoothedWeights = remember(interpolatedWeights) {
                    interpolatedWeights?.let { rollingAverage(it, 5) }
                }
                val minWeight = smoothedWeights?.minOrNull()
                val maxWeight = smoothedWeights?.maxOrNull()
                val weightScaleMin = if (minWeight != null && maxWeight != null) {
                    val center = (minWeight + maxWeight) / 2.0
                    val halfRange = maxOf((maxWeight - minWeight) / 2.0, 2.5)
                    center - halfRange
                } else null
                val weightScaleMax = if (minWeight != null && maxWeight != null) {
                    val center = (minWeight + maxWeight) / 2.0
                    val halfRange = maxOf((maxWeight - minWeight) / 2.0, 2.5)
                    center + halfRange
                } else null
                val normalizedWeights = remember(smoothedWeights, chartMin, chartMax, weightScaleMin, weightScaleMax) {
                    smoothedWeights?.let { weights ->
                        normalizeToScale(weights, chartMin, chartMax, weightScaleMin!!, weightScaleMax!!)
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Net daily calories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.foregroundDefault
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendDot(color = colors.foregroundBrand, label = "Net intake", style = graphTextStyle)
                            LegendDot(color = colors.foregroundSupport.copy(alpha = 0.5f), label = "Goal", style = graphTextStyle)
                            if (normalizedWeights != null) {
                                LegendDot(color = colors.foregroundSuccess, label = "Weight", style = graphTextStyle)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .height(210.dp)
                            .fillMaxWidth()
                    ) {
                        val lines = remember(smoothedNetIntake, normalizedWeights, budget) {
                            buildList {
                                add(Line(
                                    label = "Net intake",
                                    values = smoothedNetIntake,
                                    color = SolidColor(colors.foregroundBrand),
                                    firstGradientFillColor = colors.foregroundBrand.copy(alpha = 0.3f),
                                    secondGradientFillColor = Color.Transparent,
                                    strokeAnimationSpec = tween(200, easing = EaseInOutCubic),
                                    gradientAnimationSpec = tween(200, easing = EaseInOutCubic),
                                    gradientAnimationDelay = 100,
                                    drawStyle = Stroke(2.dp),
                                ))
                                if (smoothedNetIntake.isNotEmpty()) {
                                    add(Line(
                                        label = "Goal",
                                        values = List(smoothedNetIntake.size) { budget },
                                        color = SolidColor(colors.foregroundSupport.copy(alpha = 0.5f)),
                                        firstGradientFillColor = Color.Transparent,
                                        secondGradientFillColor = Color.Transparent,
                                        strokeAnimationSpec = tween(200, easing = EaseInOutCubic),
                                        gradientAnimationSpec = tween(200, easing = EaseInOutCubic),
                                        gradientAnimationDelay = 100,
                                        drawStyle = Stroke(1.dp),
                                    ))
                                }
                                normalizedWeights?.let {
                                    add(Line(
                                        label = "Weight",
                                        values = it,
                                        color = SolidColor(colors.foregroundSuccess),
                                        firstGradientFillColor = Color.Transparent,
                                        secondGradientFillColor = Color.Transparent,
                                        strokeAnimationSpec = tween(200, easing = EaseInOutCubic),
                                        gradientAnimationSpec = tween(200, easing = EaseInOutCubic),
                                        gradientAnimationDelay = 100,
                                        drawStyle = Stroke(2.dp),
                                    ))
                                }
                            }
                        }
                        LineChart(
                            modifier = Modifier.fillMaxSize(),
                            minValue = chartMin,
                            maxValue = chartMax,
                            data = lines,
                            dividerProperties = DividerProperties(enabled = false),
                            gridProperties = GridProperties(enabled = false),
                            labelProperties = LabelProperties(enabled = false),
                            labelHelperProperties = LabelHelperProperties(enabled = false),
                            indicatorProperties = HorizontalIndicatorProperties(enabled = false, textStyle = graphTextStyle),
                            zeroLineProperties = ZeroLineProperties(enabled = false),
                            curvedEdges = true,
                            animationMode = AnimationMode.Together { it * 2L },
                        )
                        Text(
                            text = "${minNet.toInt()} kcal",
                            style = graphTextStyle,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 4.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(colors.backgroundSurface1)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                        Text(
                            text = "${maxNet.toInt()} kcal",
                            style = graphTextStyle,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(horizontal = 4.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(colors.backgroundSurface1)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                        if (weightScaleMin != null && weightScaleMax != null) {
                            Text(
                                text = "${weightScaleMin.roundToOneDecimal()} kg",
                                style = graphTextStyle,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(horizontal = 4.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colors.backgroundSurface1)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                            Text(
                                text = "${weightScaleMax.roundToOneDecimal()} kg",
                                style = graphTextStyle,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(horizontal = 4.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colors.backgroundSurface1)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }

                    if (weightData.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Add daily weigh-ins to see your weight trend alongside calories",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foregroundSupport
                        )
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }

        // Top calorie sources
        if (topFoods.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Top calorie sources",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.foregroundDefault
                    )
                    TooltipIcon(
                        title = "Top calorie sources",
                        body = "The foods that contributed the most calories in this period. Small changes to your top items — like portion size or frequency — tend to have the biggest impact on your overall intake.",
                        onShow = showTooltip,
                        colors = colors
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            itemsIndexed(topFoods) { index, item ->
                TopFoodItem(item = item, index = index, size = topFoods.size, colors = colors)
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// ── Onboarding ────────────────────────────────────────────────────────────────

@Composable
private fun OnboardingCard(
    daysFilled: Long,
    hasWeightData: Boolean,
    onOpenDemo: (() -> Unit)? = null,
    colors: RecipesColors
) {
    val foodDone = daysFilled >= 7
    val weightDone = hasWeightData
    val stepsComplete = (if (foodDone) 1 else 0) + (if (weightDone) 1 else 0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.backgroundSurface1
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header with progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Getting started",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.foregroundDefault
                )
                Text(
                    "$stepsComplete / 2 done",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (stepsComplete == 2) colors.foregroundSuccess else colors.foregroundSupport
                )
            }

            if (stepsComplete < 2) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { stepsComplete / 2f },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = colors.foregroundBrand,
                    trackColor = colors.foregroundSupport.copy(alpha = 0.12f)
                )
            }

            // Intro text when completely fresh
            if (daysFilled == 0L && !hasWeightData) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Statistics show the relationship between what you eat, your exercise, and how your weight changes over time. Two things will unlock all the insights:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foregroundSupport
                )
            }

            Spacer(Modifier.height(18.dp))

            // Step 1: Food logging
            OnboardingStep(
                done = foodDone,
                title = "Log food for 7 days",
                statusLabel = if (!foodDone) "${daysFilled} / 7 days" else null,
                unlocks = "Daily averages · calorie chart · top food sources",
                colors = colors
            )

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = colors.foregroundSupport.copy(alpha = 0.1f))
            Spacer(Modifier.height(14.dp))

            // Step 2: Weight tracking
            OnboardingStep(
                done = weightDone,
                title = "Add daily weigh-ins",
                statusLabel = null,
                unlocks = "Weight trend · maintenance calories · personalized recommendations",
                colors = colors
            )

            // Contextual tip when food is done but weight is the blocker
            if (foodDone && !weightDone) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Tip: weigh yourself at the same time each morning for the most accurate trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foregroundSupport
                )
            }

            if (onOpenDemo != null) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = colors.foregroundSupport.copy(alpha = 0.1f))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onOpenDemo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Explore demo", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    done: Boolean,
    title: String,
    statusLabel: String?,
    unlocks: String,
    colors: RecipesColors
) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = if (done) "✓" else "○",
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) colors.foregroundSuccess else colors.foregroundSupport,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (done) FontWeight.Normal else FontWeight.SemiBold,
                    color = if (done) colors.foregroundSupport else colors.foregroundDefault
                )
                if (statusLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.foregroundBrand
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Unlocks: $unlocks",
                style = MaterialTheme.typography.bodySmall,
                color = colors.foregroundSupport.copy(alpha = 0.7f)
            )
        }
    }
}

// ── Tooltip ───────────────────────────────────────────────────────────────────

@Composable
private fun TooltipIcon(
    title: String,
    body: String,
    onShow: (String, String) -> Unit,
    colors: RecipesColors
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clickable { onShow(title, body) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Info: $title",
            modifier = Modifier.size(14.dp),
            tint = colors.foregroundSupport.copy(alpha = 0.45f)
        )
    }
}

// ── Supporting composables ────────────────────────────────────────────────────

@Composable
private fun TopFoodItem(
    item: StatsMostEatenByKcal,
    index: Int,
    size: Int,
    colors: RecipesColors
) {
    val totalKcal = item.total_kcal ?: 0.0
    val totalG = item.total_quantity_g ?: 0.0
    val kcalDensity = if (totalG > 0) totalKcal / totalG * 100.0 else null

    ListItem(
        headlineContent = {
            Text(item.item_name, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            val densityText = kcalDensity?.let { "${it.toInt()} kcal/100g" } ?: ""
            if (densityText.isNotEmpty()) {
                Text(densityText, style = MaterialTheme.typography.bodySmall, color = colors.foregroundSupport)
            }
        },
        trailingContent = {
            Text(
                text = "${totalKcal.toInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        },
        modifier = Modifier.clip(getRoundedCornerShape(index = index, size = size))
    )
}

@Composable
internal fun LegendDot(color: Color, label: String, style: androidx.compose.ui.text.TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(label, style = style)
    }
}

// ── Math utilities ────────────────────────────────────────────────────────────

internal fun interpolateWeightsForDays(
    weightData: List<WeightLog>,
    days: List<String>
): List<Double>? {
    if (weightData.isEmpty()) return null
    return days.map { day ->
        val exact = weightData.firstOrNull { it.date == day }
        if (exact != null) {
            exact.weight_kg
        } else {
            val before = weightData.lastOrNull { it.date < day }
            val after = weightData.firstOrNull { it.date > day }
            when {
                before != null && after != null -> {
                    val totalDays = daysBetweenIso(before.date, after.date).toDouble()
                    val elapsed = daysBetweenIso(before.date, day).toDouble()
                    val t = if (totalDays > 0) elapsed / totalDays else 0.0
                    before.weight_kg + t * (after.weight_kg - before.weight_kg)
                }
                before != null -> before.weight_kg
                after != null  -> after.weight_kg
                else           -> weightData.first().weight_kg
            }
        }
    }
}

internal fun normalizeToScale(
    values: List<Double>,
    targetMin: Double,
    targetMax: Double,
    sourceMin: Double = values.minOrNull() ?: 0.0,
    sourceMax: Double = values.maxOrNull() ?: 1.0,
): List<Double> {
    if (sourceMax == sourceMin) return values.map { (targetMin + targetMax) / 2.0 }
    return values.map { v -> (v - sourceMin) / (sourceMax - sourceMin) * (targetMax - targetMin) + targetMin }
}

internal fun daysBetweenIso(from: String, to: String): Long {
    fun toEpochDay(d: String): Long {
        val y = d.substring(0, 4).toInt()
        val m = d.substring(5, 7).toInt()
        val day = d.substring(8, 10).toInt()
        val a = (14 - m) / 12
        val yr = y + 4800 - a
        val mo = m + 12 * a - 3
        return day + (153 * mo + 2) / 5 + 365L * yr + yr / 4 - yr / 100 + yr / 400 - 32045
    }
    return toEpochDay(to) - toEpochDay(from)
}

internal fun rollingAverage(values: List<Double>, windowSize: Int): List<Double> {
    if (values.size <= windowSize) return values
    val half = windowSize / 2
    return values.mapIndexed { i, _ ->
        val from = (i - half).coerceAtLeast(0)
        val to = (i + half + 1).coerceAtMost(values.size)
        values.subList(from, to).average()
    }
}

internal fun Double.roundToOneDecimal(): Double = (this * 10).toInt() / 10.0
internal fun Double.roundToTwoDecimals(): Double = (this * 100).toInt() / 100.0
