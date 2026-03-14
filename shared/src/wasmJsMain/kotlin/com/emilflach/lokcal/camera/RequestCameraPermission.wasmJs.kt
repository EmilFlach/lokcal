package com.emilflach.lokcal.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// On web the browser prompts for camera access automatically when the media
// stream is first requested by the scanner.
@Composable
internal actual fun RequestCameraPermission(onResult: (granted: Boolean) -> Unit) {
    LaunchedEffect(Unit) { onResult(true) }
}
