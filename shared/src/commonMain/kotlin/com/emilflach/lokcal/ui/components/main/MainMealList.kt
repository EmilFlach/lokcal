package com.emilflach.lokcal.ui.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.viewmodel.DayState
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Composable
fun MainMealList(
    state: DayState,
    selectedDate: LocalDate,
    onOpenMeal: (String, String) -> Unit,
    isCompact: Boolean = false,
) {
    val colors = LocalRecipesColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        state.summaries.forEach { s ->
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .clip(getRoundedCornerShape(index = state.summaries.indexOf(s), state.summaries.size))
                    .background(colors.backgroundPage)
                    .clickable { onOpenMeal(s.mealType, selectedDate.toString()) }
            ) {
                Row(
                    Modifier
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
                                tint = if (s.totalKcal > 0) colors.foregroundBrand else colors.foregroundDefault
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
                                maxLines = if (isCompact) 1 else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Column(modifier = Modifier.width(80.dp), horizontalAlignment = Alignment.End) {
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
        Spacer(Modifier.height(16.dp))
    }
}
