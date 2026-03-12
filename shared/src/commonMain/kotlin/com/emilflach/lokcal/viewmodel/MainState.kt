package com.emilflach.lokcal.viewmodel

import androidx.compose.ui.graphics.vector.ImageVector
import com.emilflach.lokcal.Intake
import kotlinx.datetime.LocalDate

data class MealSummary(
    val mealType: String,
    val items: List<Intake>,
    val totalKcal: Double,
    val summaryText: String,
    val mealIcon: ImageVector
)

data class DayDelta(
    val date: LocalDate,
    val deltaKcal: Double // positive => over, negative => under
)

data class DayState(
    val summaries: List<MealSummary> = emptyList(),
    val percentageLeft: Double = 0.0,
    val leftKcal: Double = 0.0,
    val burnedKcal: Double = 0.0,
    val eatenKcal: Double = 0.0,
    val startingKcal: Double = 0.0,
    val showWeightPrompt: Boolean = false
)

data class MainUiState(
    val selectedDate: LocalDate,
    val dayState: DayState = DayState(),
    val last7Deltas: List<DayDelta> = emptyList()
)
