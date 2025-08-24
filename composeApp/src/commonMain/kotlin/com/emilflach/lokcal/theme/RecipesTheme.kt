package com.emilflach.lokcal.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand palette
val BrandDarkest = Color(0xFF865908)
val BrandDarker = Color(0xFFbb7d0b)
val BrandDefault = Color(0xFFD9910D)
val BrandLighter = Color(0xFFf4b644)
val BrandLightest = Color(0xFFf7cb79)

// Neutral palette
val Neutral50 = Color(0xFFeef0fc)
val Neutral100 = Color(0xFFccd3f7)
val Neutral200 = Color(0xFFaab6f2)
val Neutral300 = Color(0xFF8898ec)
val Neutral400 = Color(0xFF4b63e3)
val Neutral500 = Color(0xFF2340dc)
val Neutral600 = Color(0xFF16298c)
val Neutral700 = Color(0xFF101d64)
val Neutral800 = Color(0xFF09123c)
val Neutral900 = Color(0xFF030614)

// Contextual colors
val InfoLight = Color(0xFF90CAF9)
val Info = Color(0xFF2196F3)
val InfoDark = Color(0xFF0D47A1)

val SuccessLight = Color(0xFFA5D6A7)
val Success = Color(0xFF4CAF50)
val SuccessDark = Color(0xFF1B5E20)

val WarningLight = Color(0xFFFFE082)
val Warning = Color(0xFFFFC107)
val WarningDark = Color(0xFFF57F17)

val DangerLight = Color(0xFFEF9A9A)
val Danger = Color(0xFFF44336)
val DangerDark = Color(0xFFB71C1C)

/**
 * Semantic color system for Recipes app
 */
@Immutable
class RecipesColors(
    // Background Colors
    val backgroundPage: Color,
    val backgroundSurface1: Color,
    val backgroundSurface2: Color,
    val backgroundSurface1Hover: Color,
    val backgroundSurface1Pressed: Color,

    val backgroundBrand: Color,
    val backgroundBrandSubtle: Color,
    val backgroundBrandHover: Color,
    val backgroundBrandPressed: Color,

    val backgroundInfo: Color,
    val backgroundInfoSubtle: Color,
    val backgroundSuccess: Color,
    val backgroundSuccessSubtle: Color,
    val backgroundWarning: Color,
    val backgroundWarningSubtle: Color,
    val backgroundDanger: Color,
    val backgroundDangerSubtle: Color,

    val backgroundDisabled: Color,
    val backgroundSelected: Color,
    val backgroundLoading: Color,

    // On Background Colors
    val onBackgroundBrand: Color,
    val onBackgroundBrandSubtle: Color,
    val onBackgroundInfo: Color,
    val onBackgroundInfoSubtle: Color,
    val onBackgroundSuccess: Color,
    val onBackgroundSuccessSubtle: Color,
    val onBackgroundWarning: Color,
    val onBackgroundWarningSubtle: Color,
    val onBackgroundDanger: Color,
    val onBackgroundDangerSubtle: Color,

    // Foreground Colors
    val foregroundDefault: Color,
    val foregroundSupport: Color,
    val foregroundBrand: Color,
    val foregroundInfo: Color,
    val foregroundSuccess: Color,
    val foregroundWarning: Color,
    val foregroundDanger: Color,
    val foregroundDisabled: Color,

    // Border Colors
    val borderDefault: Color,
    val borderStrong: Color,
    val borderBrand: Color,
    val borderInfo: Color,
    val borderSuccess: Color,
    val borderWarning: Color,
    val borderDanger: Color,
    val borderDisabled: Color,
    val borderFocus: Color,
    val borderSeparator: Color,

    // Link Colors
    val linkDefault: Color,
    val linkHover: Color,
    val linkPressed: Color,
    val linkVisited: Color,

    // Is Dark Theme
    val isDark: Boolean
) {
    companion object {
        // Light theme colors
        val Light = RecipesColors(
            // Background Colors
            backgroundPage = Neutral50,
            backgroundSurface1 = Neutral100,
            backgroundSurface2 = Neutral200,
            backgroundSurface1Hover = Neutral200,
            backgroundSurface1Pressed = Neutral300,

            backgroundBrand = BrandDefault,
            backgroundBrandSubtle = BrandLightest,
            backgroundBrandHover = BrandDarker,
            backgroundBrandPressed = BrandDarkest,

            backgroundInfo = Info,
            backgroundInfoSubtle = InfoLight.copy(alpha = 0.15f),
            backgroundSuccess = Success,
            backgroundSuccessSubtle = SuccessLight.copy(alpha = 0.15f),
            backgroundWarning = Warning,
            backgroundWarningSubtle = WarningLight.copy(alpha = 0.15f),
            backgroundDanger = Danger,
            backgroundDangerSubtle = DangerLight.copy(alpha = 0.15f),

            backgroundDisabled = Neutral300,
            backgroundSelected = BrandLightest,
            backgroundLoading = Neutral300,

            // On Background Colors
            onBackgroundBrand = Color.White,
            onBackgroundBrandSubtle = BrandDefault,
            onBackgroundInfo = Color.White,
            onBackgroundInfoSubtle = Info,
            onBackgroundSuccess = Color.White,
            onBackgroundSuccessSubtle = Success,
            onBackgroundWarning = Neutral900,
            onBackgroundWarningSubtle = WarningDark,
            onBackgroundDanger = Color.White,
            onBackgroundDangerSubtle = Danger,

            // Foreground Colors
            foregroundDefault = Neutral900,
            foregroundSupport = Neutral700,
            foregroundBrand = BrandDefault,
            foregroundInfo = Info,
            foregroundSuccess = Success,
            foregroundWarning = Warning,
            foregroundDanger = Danger,
            foregroundDisabled = Neutral600,

            // Border Colors
            borderDefault = Neutral200,
            borderStrong = Neutral700,
            borderBrand = BrandDefault,
            borderInfo = Info,
            borderSuccess = Success,
            borderWarning = Warning,
            borderDanger = Danger,
            borderDisabled = Neutral300,
            borderFocus = Neutral200,
            borderSeparator = Neutral300,

            // Link Colors
            linkDefault = BrandDefault,
            linkHover = BrandDarker,
            linkPressed = BrandDarkest,
            linkVisited = BrandDarker,

            isDark = false
        )

        // Dark theme colors with darkened blue/purple hues
        val Dark = RecipesColors(
            // Background Colors
            backgroundPage = Neutral900,
            backgroundSurface1 = Neutral800,
            backgroundSurface2 = Neutral700,
            backgroundSurface1Hover = Neutral700,
            backgroundSurface1Pressed = Neutral600,

            backgroundBrand = BrandDefault,
            backgroundBrandSubtle = BrandDarker,
            backgroundBrandHover = BrandLighter,
            backgroundBrandPressed = BrandLightest,

            backgroundInfo = Info,
            backgroundInfoSubtle = Info.copy(alpha = 0.2f),
            backgroundSuccess = Success,
            backgroundSuccessSubtle = Success.copy(alpha = 0.2f),
            backgroundWarning = Warning,
            backgroundWarningSubtle = Warning.copy(alpha = 0.2f),
            backgroundDanger = Danger,
            backgroundDangerSubtle = Danger.copy(alpha = 0.2f),

            backgroundDisabled = Neutral600,
            backgroundSelected = BrandDarkest,
            backgroundLoading = Neutral600,

            // On Background Colors
            onBackgroundBrand = Color.White,
            onBackgroundBrandSubtle = BrandDefault,
            onBackgroundInfo = Color.White,
            onBackgroundInfoSubtle = InfoLight,
            onBackgroundSuccess = Color.White,
            onBackgroundSuccessSubtle = SuccessLight,
            onBackgroundWarning = Neutral900,
            onBackgroundWarningSubtle = WarningLight,
            onBackgroundDanger = Color.White,
            onBackgroundDangerSubtle = DangerLight,

            // Foreground Colors
            foregroundDefault = Neutral50,
            foregroundSupport = Neutral100,
            foregroundBrand = BrandDefault,
            foregroundInfo = InfoLight,
            foregroundSuccess = SuccessLight,
            foregroundWarning = WarningLight,
            foregroundDanger = DangerLight,
            foregroundDisabled = Neutral300,

            // Border Colors
            borderDefault = Neutral100,
            borderStrong = Neutral200,
            borderBrand = BrandDefault,
            borderInfo = InfoLight,
            borderSuccess = SuccessLight,
            borderWarning = WarningLight,
            borderDanger = DangerLight,
            borderDisabled = Neutral600,
            borderFocus = Neutral200,
            borderSeparator = Neutral600,

            // Link Colors
            linkDefault = BrandDefault,
            linkHover = BrandLighter,
            linkPressed = BrandLightest,
            linkVisited = BrandDarker,

            isDark = true
        )
    }
}

// Local composition for providing RecipesColors
val LocalRecipesColors = staticCompositionLocalOf { RecipesColors.Light }
