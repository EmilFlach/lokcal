package com.emilflach.lokcal.util

import androidx.compose.runtime.Composable

@Composable
expect fun SystemBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)
