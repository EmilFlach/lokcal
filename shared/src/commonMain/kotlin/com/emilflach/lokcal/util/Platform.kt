package com.emilflach.lokcal.util

expect val showBarcodeScanner: Boolean

enum class AppPlatform { Android, Ios, Jvm, WasmJs }

expect val currentPlatform: AppPlatform
