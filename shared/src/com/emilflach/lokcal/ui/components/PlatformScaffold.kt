package com.emilflach.lokcal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.util.AppPlatform
import com.emilflach.lokcal.util.currentPlatform
import com.emilflach.lokcal.util.getTopSafeAreaInset
import dev.chrisbanes.haze.*

/**
 * True when the app is running with iOS native navigation.
 * Use this only where [PlatformScaffold] padding is unavailable (e.g. screens
 * that manage their own layout outside of PlatformScaffold).
 */
val isNativeNavigation: Boolean get() = currentPlatform == AppPlatform.Ios

/**
 * Padding values passed to [PlatformScaffold] content.
 *
 * Implements [PaddingValues] so it can be passed directly to `Modifier.padding()` or
 * `LazyColumn.contentPadding`. On screens with a native-nav blur overlay the effective
 * outer padding is zeroed out (content should flow edge-to-edge), otherwise it equals
 * the normal scaffold insets.
 *
 * Use [listContentPadding] to get the correct content padding for a LazyColumn on
 * screens that use the blur scroll overlay.
 */
@Stable
class PlatformPadding internal constructor(
    private val raw: PaddingValues,
    hasScrollOverlay: Boolean,
) : PaddingValues {

    private val effective: PaddingValues =
        if (hasScrollOverlay) PaddingValues(0.dp) else raw

    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        effective.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding() = effective.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        effective.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding() = effective.calculateBottomPadding()

    /**
     * Content padding for a LazyColumn on screens that use the blur scroll overlay.
     *
     * On overlay screens: top/bottom insets from the raw scaffold padding + [horizontal] on
     * each side. On other screens: [horizontal] on all four sides.
     */
    fun listContentPadding(horizontal: Dp = 16.dp): PaddingValues =
        PaddingValues(
            top = raw.calculateTopPadding(),
            bottom = raw.calculateBottomPadding(),
            start = horizontal,
            end = horizontal,
        )
}

/**
 * Platform-aware Scaffold wrapper that handles native navigation behavior automatically.
 *
 * - iOS: hides topBar/FAB (SwiftUI handles them), adds safe-area + nav bar padding,
 *   shows a progressive blur overlay when scrolled.
 * - WASM: adds top/bottom insets on the Scaffold itself so the topBar and content both
 *   clear the phone frame's rounded corners.
 * - Android / JVM: standard Scaffold, adds 80dp bottom padding when a FAB is present.
 *
 * @param scrollState Optional LazyListState to enable nav bar blur effect on scroll (iOS only)
 * @param navBarBackgroundColor Background color for nav bar blur tint (iOS only, default: White)
 */
@Composable
fun PlatformScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = LocalRecipesColors.current.backgroundPage,
    contentColor: Color = contentColorFor(containerColor),
    hasFab: Boolean = false,
    scrollState: LazyListState? = null,
    navBarBackgroundColor: Color = Color.White,
    content: @Composable (PlatformPadding) -> Unit
) {
    when (currentPlatform) {
        AppPlatform.Ios -> IosScaffold(
            modifier = modifier,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            containerColor = containerColor,
            contentColor = contentColor,
            hasFab = hasFab,
            scrollState = scrollState,
            navBarBackgroundColor = navBarBackgroundColor,
            content = content,
        )

        AppPlatform.WasmJs -> WasmScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            hasFab = hasFab,
            content = content,
        )

        AppPlatform.Android, AppPlatform.Jvm -> DefaultScaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            hasFab = hasFab,
            content = content,
        )
    }
}

// ── iOS ──────────────────────────────────────────────────────────────────────

@Composable
private fun IosScaffold(
    modifier: Modifier,
    bottomBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    containerColor: Color,
    contentColor: Color,
    hasFab: Boolean,
    scrollState: LazyListState?,
    navBarBackgroundColor: Color,
    content: @Composable (PlatformPadding) -> Unit,
) {
    val hazeState = remember { HazeState() }

    val showNavBarBackground = remember(scrollState) {
        derivedStateOf {
            scrollState?.let { it.firstVisibleItemIndex > 0 || it.firstVisibleItemScrollOffset > 0 } ?: false
        }
    }
    val navBarBackgroundAlpha by animateFloatAsState(
        targetValue = if (showNavBarBackground.value) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
    )

    Scaffold(
        modifier = modifier,
        topBar = {},
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = {},
        containerColor = containerColor,
        contentColor = contentColor,
    ) { paddingValues ->
        val topPadding = getTopSafeAreaInset() + 80.dp
        val bottomPadding = paddingValues.calculateBottomPadding() + if (hasFab) 100.dp else 0.dp
        val adjustedPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            top = topPadding,
            bottom = bottomPadding,
        )
        val platformPadding = PlatformPadding(
            raw = adjustedPadding,
            hasScrollOverlay = scrollState != null,
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = if (scrollState != null) Modifier.hazeSource(hazeState).fillMaxSize()
                           else Modifier.fillMaxSize()
            ) {
                content(platformPadding)
            }

            if (scrollState != null) {
                val topHeight = getTopSafeAreaInset() + 80.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topHeight)
                        .align(Alignment.TopCenter)
                        .hazeEffect(state = hazeState) {
                            backgroundColor = navBarBackgroundColor
                            tints = listOf(HazeTint(color = navBarBackgroundColor))
                            progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                            alpha = navBarBackgroundAlpha
                        }
                )
            }
        }
    }
}

// ── WASM ─────────────────────────────────────────────────────────────────────

/** Platform-specific top inset to add on top of system safe-area insets. Non-zero on WASM only. */
val platformTopInset: Dp get() = if (currentPlatform == AppPlatform.WasmJs) 24.dp else 0.dp

/** Platform-specific bottom inset to add on top of system safe-area insets. Non-zero on WASM only. */
val platformBottomInset: Dp get() = if (currentPlatform == AppPlatform.WasmJs) 36.dp else 0.dp

@Composable
private fun WasmScaffold(
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    floatingActionButtonPosition: FabPosition,
    containerColor: Color,
    contentColor: Color,
    hasFab: Boolean,
    content: @Composable (PlatformPadding) -> Unit,
) {
    Scaffold(
        // Inset the whole scaffold so the topBar and content both clear the
        // phone frame's rounded corners.
        modifier = modifier.padding(top = platformTopInset, bottom = platformBottomInset),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
    ) { paddingValues ->
        val bottomPadding = paddingValues.calculateBottomPadding() + if (hasFab) 80.dp else 0.dp
        val adjustedPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            top = paddingValues.calculateTopPadding(),
            bottom = bottomPadding,
        )
        content(PlatformPadding(raw = adjustedPadding, hasScrollOverlay = false))
    }
}

// ── Android / JVM ─────────────────────────────────────────────────────────────

@Composable
private fun DefaultScaffold(
    modifier: Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    floatingActionButtonPosition: FabPosition,
    containerColor: Color,
    contentColor: Color,
    hasFab: Boolean,
    content: @Composable (PlatformPadding) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
    ) { paddingValues ->
        val bottomPadding = paddingValues.calculateBottomPadding() + if (hasFab) 80.dp else 0.dp
        val adjustedPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
            end = paddingValues.calculateEndPadding(LayoutDirection.Ltr),
            top = paddingValues.calculateTopPadding(),
            bottom = bottomPadding,
        )
        content(PlatformPadding(raw = adjustedPadding, hasScrollOverlay = false))
    }
}
