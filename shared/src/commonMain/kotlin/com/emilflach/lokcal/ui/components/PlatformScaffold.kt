package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.util.getTopSafeAreaInset
import com.emilflach.lokcal.util.usesNativeNavigation

/**
 * Platform-aware Scaffold wrapper that handles native navigation behavior automatically.
 *
 * On iOS with native navigation:
 * - Hides the topBar (since iOS uses SwiftUI navigation bar)
 * - Provides top padding for the native navigation bar (safe area + 80dp)
 * - Adds extra bottom padding: 100dp with FAB, 0dp without
 *
 * On other platforms:
 * - Behaves like a standard Scaffold
 * - Adds extra bottom padding: 80dp with FAB, 0dp without
 *
 * Simply use the padding provided to content:
 * ```
 * PlatformScaffold(
 *     topBar = { ... },
 *     floatingActionButton = { FloatingActionButton(...) { ... } }
 * ) { padding ->
 *     Column(Modifier.padding(padding)) { ... }
 * }
 * ```
 */
@Composable
fun PlatformScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = contentColorFor(containerColor),
    hasFab: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = if (usesNativeNavigation) { {} } else topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor
    ) { paddingValues ->
        val adjustedPadding = if (usesNativeNavigation) {
            // On iOS with native nav, provide top padding = safe area + nav bar height (80dp)
            val topPadding = getTopSafeAreaInset() + 80.dp
            val bottomPadding = paddingValues.calculateBottomPadding() + if (hasFab) 100.dp else 0.dp
            PaddingValues(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                top = topPadding,
                bottom = bottomPadding
            )
        } else {
            // On other platforms, add 80dp bottom padding when there's a FAB
            val bottomPadding = paddingValues.calculateBottomPadding() + if (hasFab) 80.dp else 0.dp
            PaddingValues(
                start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
                top = paddingValues.calculateTopPadding(),
                bottom = bottomPadding
            )
        }
        content(adjustedPadding)
    }
}
