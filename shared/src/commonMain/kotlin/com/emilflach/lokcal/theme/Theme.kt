package com.emilflach.lokcal.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*


internal val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }
internal val LocalOnThemeToggle = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
internal fun AppTheme(
    content: @Composable () -> Unit
) {
    val systemIsDark = getSystemIsDarkTheme()
    // Use remember without key so we can toggle it manually
    var isDark by remember { mutableStateOf(systemIsDark) }

    println("AppTheme: systemIsDark = $systemIsDark, isDark = $isDark")

    CompositionLocalProvider(
        LocalThemeIsDark provides remember { mutableStateOf(isDark) },
        LocalRecipesColors provides if (isDark) RecipesColors.Dark else RecipesColors.Light
    ) {
        val recipes = LocalRecipesColors.current
        val colorScheme = colorSchemeFromRecipes(recipes)

        println("AppTheme: recipes.isDark = ${recipes.isDark}")
        println("AppTheme: background = ${colorScheme.background}, surface = ${colorScheme.surface}")

        SystemAppearance(!isDark)
        MaterialTheme(
            colorScheme = colorScheme,
            content = {
                Surface(
                    color = colorScheme.background,
                    contentColor = colorScheme.onBackground,
                    content = {
                        // Debug: Theme toggle button accessible to all screens
                        CompositionLocalProvider(
                            LocalOnThemeToggle provides { isDark = !isDark }
                        ) {
                            content()
                        }
                    }
                )
            }
        )
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
