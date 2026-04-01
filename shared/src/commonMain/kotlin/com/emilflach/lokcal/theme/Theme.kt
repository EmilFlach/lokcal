package com.emilflach.lokcal.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

@Composable
internal expect fun PlatformRippleOverride(content: @Composable () -> Unit)


internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    val isDark = getSystemIsDarkTheme()

    CompositionLocalProvider(
        LocalThemeIsDark provides remember { mutableStateOf(isDark) },
        LocalRecipesColors provides if (isDark) RecipesColors.Dark else RecipesColors.Light
    ) {
        val colorScheme = colorSchemeFromRecipes(LocalRecipesColors.current)

        SystemAppearance(!isDark)
        MaterialTheme(colorScheme = colorScheme) {
            Surface(
                color = colorScheme.background,
                contentColor = colorScheme.onBackground,
            ) {
                PlatformRippleOverride(content)
            }
        }
    }
}

@Composable
internal expect fun getSystemIsDarkTheme(): Boolean


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
