package com.emilflach.lokcal.util

expect val showBarcodeScanner: Boolean

/**
 * True if the platform uses native navigation (iOS with SwiftUI NavigationStack).
 * When true, Compose screens should hide their TopAppBars since navigation is handled natively.
 */
expect val usesNativeNavigation: Boolean
