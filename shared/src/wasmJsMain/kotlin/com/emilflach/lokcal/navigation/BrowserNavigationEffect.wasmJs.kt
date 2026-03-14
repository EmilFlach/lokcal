package com.emilflach.lokcal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.github.terrakok.navigation3.browser.HierarchicalBrowserNavigation
import com.github.terrakok.navigation3.browser.buildBrowserHistoryFragment

@Composable
internal actual fun BrowserNavigationEffect(
    currentDestination: State<Any?>,
    nameResolver: (Any) -> Pair<String, Map<String, String>>?,
) {
    HierarchicalBrowserNavigation(
        currentDestination = currentDestination,
        currentDestinationName = { key ->
            if (key == null) return@HierarchicalBrowserNavigation null
            val (name, params) = nameResolver(key) ?: return@HierarchicalBrowserNavigation null
            buildBrowserHistoryFragment(name, params)
        },
    )
}
