package com.emilflach.lokcal.ui.components.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.RecipesColors

@Composable
fun MainSummaryHeader(
    formattedDate: String,
    onDateClick: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenWeightToday: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    colors: RecipesColors,
    showWeightBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(14.dp))
        Text(
            text = formattedDate,
            color = colors.foregroundSupport,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable { onDateClick() }
        )
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.TopEnd) {
            IconButton(
                onClick = if (showWeightBadge) onOpenWeightToday else onOpenWeightList,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MonitorWeight,
                    contentDescription = "Weight log",
                    tint = colors.foregroundSupport,
                )
            }
            if (showWeightBadge) {
                val infiniteTransition = rememberInfiniteTransition()
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    )
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(colors.foregroundBrand)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        IconButton(
            onClick = onOpenStatistics,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.InsertChart,
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
}
