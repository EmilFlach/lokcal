package com.emilflach.lokcal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Get the top safe area inset (navigation bar height) on iOS, 0 on other platforms
 */
@Composable
expect fun getTopSafeAreaInset(): Dp

/**
 * Get the bottom safe area inset (home indicator) on iOS, 0 on other platforms
 */
@Composable
expect fun getBottomSafeAreaInset(): Dp

/**
 * Get the top padding needed for native navigation.
 * On iOS with native navigation, this includes safe area + navigation bar height (80dp).
 * On other platforms, returns 0dp.
 */
@Composable
fun getTopPaddingForNativeNavigation(): Dp {
    return if (usesNativeNavigation) {
        getTopSafeAreaInset() + 80.dp
    } else {
        0.dp
    }
}
