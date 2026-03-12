package com.emilflach.lokcal.ui.components.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.InsertChart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.RecipesColors

@Composable
fun MainSummaryHeader(
    formattedDate: String,
    onDateClick: () -> Unit,
    onOpenWeightList: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    colors: RecipesColors,
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
        IconButton(
            onClick = onOpenWeightList,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MonitorWeight,
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
