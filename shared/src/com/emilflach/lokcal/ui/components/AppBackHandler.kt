package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationEventHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
internal fun AppBackHandler(
    enabled: Boolean = true,
    onBackCompleted: () -> Unit,
) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationEventHandler(
        state = state,
        isBackEnabled = enabled,
        onBackCompleted = onBackCompleted,
    )
}
