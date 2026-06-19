package com.emilflach.lokcal.ui.components.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.RecipesColors
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSummaryHeader(
    formattedDate: String,
    selectedDate: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    colors: RecipesColors,
    showWeightBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val epochMillis = selectedDate.toEpochDays() * 86_400_000L
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = epochMillis)
        val datePickerColors = DatePickerDefaults.colors(
            containerColor = colors.backgroundSurface1,
            titleContentColor = colors.foregroundSupport,
            headlineContentColor = colors.foregroundDefault,
            weekdayContentColor = colors.foregroundSupport,
            subheadContentColor = colors.foregroundSupport,
            navigationContentColor = colors.foregroundDefault,
            yearContentColor = colors.foregroundDefault,
            currentYearContentColor = colors.foregroundBrand,
            selectedYearContentColor = colors.onBackgroundBrand,
            selectedYearContainerColor = colors.backgroundBrand,
            dayContentColor = colors.foregroundDefault,
            disabledDayContentColor = colors.foregroundDisabled,
            selectedDayContentColor = colors.onBackgroundBrand,
            selectedDayContainerColor = colors.backgroundBrand,
            todayContentColor = colors.foregroundBrand,
            todayDateBorderColor = colors.borderBrand,
            dividerColor = colors.borderSeparator,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Row {
                    Spacer(Modifier.width(16.dp))
                    TextButton(onClick = {
                        onDateSelect(LocalDate.parse(currentDateIso()))
                        showDatePicker = false
                    }) { Text("Today", color = colors.foregroundBrand) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = colors.foregroundSupport)
                    }
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val days = (millis / 86_400_000L).toInt()
                            onDateSelect(LocalDate.fromEpochDays(days))
                        }
                        showDatePicker = false
                    }) { Text("OK", color = colors.foregroundBrand) }
                }
            },
            colors = datePickerColors
        ) {
            DatePicker(state = datePickerState, colors = datePickerColors)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .requiredHeight(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedDate,
                    color = colors.foregroundSupport,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.TopEnd) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = if (showWeightBadge) onOpenWeightToday else onOpenWeightList,
                    modifier = Modifier.requiredSize(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MonitorWeight,
                        contentDescription = "Weight",
                        tint = colors.foregroundSupport,
                    )
                }
            }
            if (showWeightBadge) {
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(colors.foregroundBrand)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = onOpenStatistics, modifier = Modifier.requiredSize(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.InsertChart,
                    contentDescription = "Statistics",
                    tint = colors.foregroundSupport,
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = onOpenSettings, modifier = Modifier.requiredSize(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = colors.foregroundSupport,
                )
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}
