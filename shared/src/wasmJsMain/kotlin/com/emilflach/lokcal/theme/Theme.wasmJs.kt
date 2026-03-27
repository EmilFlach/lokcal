package com.emilflach.lokcal.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun SystemAppearance(isDark: Boolean) {
}

@Composable
internal actual fun getSystemIsDarkTheme(): Boolean = isSystemInDarkTheme()