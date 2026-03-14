package com.emilflach.lokcal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

@Composable
internal expect fun BrowserNavigationEffect(
    currentDestination: State<Any?>,
    nameResolver: (Any) -> Pair<String, Map<String, String>>?,
)
