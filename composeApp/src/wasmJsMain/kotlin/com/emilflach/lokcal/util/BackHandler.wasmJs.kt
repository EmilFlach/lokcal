package com.emilflach.lokcal.util

import androidx.compose.runtime.Composable

@Composable
actual fun SystemBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // No-op on Web (Wasm/JS)
}
