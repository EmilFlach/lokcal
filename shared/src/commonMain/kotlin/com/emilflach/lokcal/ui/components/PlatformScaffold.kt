package com.emilflach.lokcal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.util.getTopSafeAreaInset
import com.emilflach.lokcal.util.usesNativeNavigation
import dev.chrisbanes.haze.*

/**
 * Platform-aware Scaffold wrapper that handles native navigation behavior automatically.
 *
 * On iOS with native navigation:
 * - Hides the topBar (uses SwiftUI navigation bar instead)
 * - Provides top padding for native navigation bar (safe area + 80dp)
 * - Bottom padding: 100dp with FAB, 0dp without
 * - When scrollState provided: shows progressive blur effect on nav bar background
 *
 * On other platforms:
 * - Standard Scaffold behavior
 * - Bottom padding: 80dp with FAB, 0dp without
 *
 * @param scrollState Optional LazyListState to enable nav bar blur effect on scroll
 * @param navBarBackgroundColor Background color for nav bar blur tint (default: White)
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
    scrollState: LazyListState? = null,
    navBarBackgroundColor: Color = Color.White,
    content: @Composable (PaddingValues) -> Unit
) {
    // Create HazeState for blur effect
    val hazeState = remember { HazeState() }

    // Determine if we should show the nav bar background (scrolled state)
    val showNavBarBackground = remember(scrollState) {
        derivedStateOf {
            scrollState?.let {
                it.firstVisibleItemIndex > 0 || it.firstVisibleItemScrollOffset > 0
            } ?: false
        }
    }

    // Animate the nav bar background overlay
    val navBarBackgroundAlpha by animateFloatAsState(
        targetValue = if (showNavBarBackground.value) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    Scaffold(
        modifier = modifier,
        topBar = if (usesNativeNavigation) { {} } else topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = if (usesNativeNavigation) { {} } else floatingActionButton,
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

        Box(modifier = Modifier.fillMaxSize()) {
            // Apply hazeSource modifier to content if using native navigation with scroll state
            val contentModifier = if (usesNativeNavigation && scrollState != null) {
                Modifier.hazeSource(state = hazeState)
            } else {
                Modifier
            }

            Box(modifier = contentModifier.fillMaxSize()) {
                content(adjustedPadding)
            }

            // Show nav bar background overlay with blur when scrolled (iOS native nav only)
            if (usesNativeNavigation && scrollState != null) {
                val topHeight = getTopSafeAreaInset() + 80.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topHeight)
                        .align(Alignment.TopCenter)
                        .hazeEffect(state = hazeState) {
                            backgroundColor = navBarBackgroundColor
                            tints = listOf(
                                HazeTint(
                                    color = navBarBackgroundColor,
                                )
                            )
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                            alpha = navBarBackgroundAlpha
                        }
                )
            }
        }
    }
}
