package com.emilflach.lokcal.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember


internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()
    val isDarkState = remember(systemIsDark) { mutableStateOf(systemIsDark) }
    CompositionLocalProvider(
        LocalThemeIsDark provides isDarkState,
        LocalRecipesColors provides if (isDarkState.value) RecipesColors.Dark else RecipesColors.Light
    ) {
        val isDark by isDarkState
        SystemAppearance(!isDark)
        val recipes = LocalRecipesColors.current
        MaterialTheme(
            colorScheme = colorSchemeFromRecipes(recipes),
            content = { Surface(content = content) }
        )
    }
}


private fun colorSchemeFromRecipes(c: RecipesColors) =
    if (c.isDark) {
        darkColorScheme(
            primary = c.backgroundBrand,
            onPrimary = c.onBackgroundBrand,
            secondary = c.backgroundSurface2,
            onSecondary = c.foregroundDefault,
            tertiary = c.backgroundSuccess,
            onTertiary = c.onBackgroundSuccess,
            error = c.backgroundDanger,
            onError = c.onBackgroundDanger,
            background = c.backgroundPage,
            onBackground = c.foregroundDefault,
            surface = c.backgroundSurface1,
            onSurface = c.foregroundDefault,
            surfaceVariant = c.backgroundSurface2,
            onSurfaceVariant = c.foregroundDefault,
            outline = c.borderDefault,
        )
    } else {
        lightColorScheme(
            primary = c.backgroundBrand,
            onPrimary = c.onBackgroundBrand,
            secondary = c.backgroundSurface2,
            onSecondary = c.foregroundDefault,
            tertiary = c.backgroundSuccess,
            onTertiary = c.onBackgroundSuccess,
            error = c.backgroundDanger,
            onError = c.onBackgroundDanger,
            background = c.backgroundPage,
            onBackground = c.foregroundDefault,
            surface = c.backgroundSurface1,
            onSurface = c.foregroundDefault,
            surfaceVariant = c.backgroundSurface2,
            onSurfaceVariant = c.foregroundDefault,
            outline = c.borderDefault,
        )
    }

@Composable
internal expect fun SystemAppearance(isDark: Boolean)
