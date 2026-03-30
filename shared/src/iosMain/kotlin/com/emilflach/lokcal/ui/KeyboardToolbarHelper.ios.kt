package com.emilflach.lokcal.ui

import platform.Foundation.NSNotificationCenter

actual object KeyboardToolbarHelper {
    actual fun setIntakeFieldFocused(focused: Boolean) {
        // Post notification to Swift side
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = "IntakeFieldFocusChanged",
            `object` = null,
            userInfo = mapOf("focused" to focused)
        )
    }
}
