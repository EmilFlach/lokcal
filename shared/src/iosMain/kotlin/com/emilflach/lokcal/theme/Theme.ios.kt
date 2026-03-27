package com.emilflach.lokcal.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.*

@Composable
internal actual fun SystemAppearance(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        )
    }
}

@Composable
internal actual fun getSystemIsDarkTheme(): Boolean {
    // Get the actual window's trait collection
    val windows = UIApplication.sharedApplication.windows
    val keyWindow = windows.firstOrNull() as? platform.UIKit.UIWindow

    val traitCollection = keyWindow?.traitCollection ?: UIScreen.mainScreen.traitCollection

    val style = traitCollection.userInterfaceStyle
    val isDark = style == UIUserInterfaceStyle.UIUserInterfaceStyleDark

    // Debug logging
    println("iOS Dark Mode Detection:")
    println("  keyWindow: $keyWindow")
    println("  userInterfaceStyle value: $style")
    println("  UIUserInterfaceStyleLight value: ${UIUserInterfaceStyle.UIUserInterfaceStyleLight}")
    println("  UIUserInterfaceStyleDark value: ${UIUserInterfaceStyle.UIUserInterfaceStyleDark}")
    println("  Comparison result: $isDark")

    return isDark
}