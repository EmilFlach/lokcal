package com.emilflach.lokcal.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * Get the top safe area inset (status bar + notch) on iOS, 0 on other platforms.
 * Used by PlatformScaffold to calculate proper top padding for native navigation.
 */
@Composable
expect fun getTopSafeAreaInset(): Dp

/**
 * Get the bottom safe area inset (home indicator) on iOS, 0 on other platforms.
 */
@Composable
expect fun getBottomSafeAreaInset(): Dp
