package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MealTypeSelector(
    selected: String,
    onSelect: (String) -> Unit,
    types: List<String> = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK"),
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        types.forEach { type ->
            val isSelected = selected == type
            if (isSelected) {
                Button(onClick = { onSelect(type) }) { Text(type.lowercase().replaceFirstChar { it.titlecase() }) }
            } else {
                OutlinedButton(onClick = { onSelect(type) }) { Text(type.lowercase().replaceFirstChar { it.titlecase() }) }
            }
        }
    }
}
