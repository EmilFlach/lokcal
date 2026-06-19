package com.emilflach.lokcal.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// On iOS the system camera permission dialog is shown automatically by the
// scanner when it first accesses the camera.
@Composable
internal actual fun RequestCameraPermission(onResult: (granted: Boolean) -> Unit) {
    LaunchedEffect(Unit) { onResult(true) }
}
