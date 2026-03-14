package com.emilflach.lokcal.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// On desktop the OS handles camera access; no explicit permission prompt needed.
@Composable
internal actual fun RequestCameraPermission(onResult: (granted: Boolean) -> Unit) {
    LaunchedEffect(Unit) { onResult(true) }
}
