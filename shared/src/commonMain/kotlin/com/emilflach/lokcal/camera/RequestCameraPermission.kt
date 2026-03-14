package com.emilflach.lokcal.camera

import androidx.compose.runtime.Composable

@Composable
internal expect fun RequestCameraPermission(onResult: (granted: Boolean) -> Unit)
