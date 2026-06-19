package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalRecipesColors.current

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foregroundSupport),
        border = BorderStroke(1.dp, colors.borderDefault),
        content = content
    )
}

@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = LocalRecipesColors.current

    OutlinedIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = colors.foregroundSupport),
        border = BorderStroke(1.dp, colors.borderDefault),
        content = content
    )
}
