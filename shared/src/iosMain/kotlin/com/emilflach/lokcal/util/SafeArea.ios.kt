package com.emilflach.lokcal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun getTopSafeAreaInset(): Dp {
    val windows = UIApplication.sharedApplication.windows
    val keyWindow = windows.firstOrNull() as? platform.UIKit.UIWindow
    val insets = keyWindow?.safeAreaInsets
    return insets?.size?.dp ?: 0.dp
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun getBottomSafeAreaInset(): Dp {
    val windows = UIApplication.sharedApplication.windows
    val keyWindow = windows.firstOrNull() as? platform.UIKit.UIWindow
    val insets = keyWindow?.safeAreaInsets
    return insets?.size?.dp ?: 0.dp
}
