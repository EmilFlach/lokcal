package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType

@Composable
actual fun SingleInputAlertDialog(
    title: String, fieldLabel: String, initialValue: String,
    confirmText: String, dismissText: String, keyboardType: KeyboardType,
    error: String?, onConfirm: (String) -> Unit, onDismiss: () -> Unit
) = SingleInputAlertDialogCompose(title, fieldLabel, initialValue, confirmText, dismissText, keyboardType, error, onConfirm, onDismiss)

@Composable
actual fun InfoAlertDialog(
    title: String, body: String, confirmText: String, onDismiss: () -> Unit
) = InfoAlertDialogCompose(title, body, confirmText, onDismiss)

@Composable
actual fun DualInputAlertDialog(
    title: String, field1Label: String, field1Initial: String, field1KeyboardType: KeyboardType,
    field2Label: String, field2Initial: String, field2KeyboardType: KeyboardType,
    confirmText: String, dismissText: String, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit
) = DualInputAlertDialogCompose(title, field1Label, field1Initial, field1KeyboardType, field2Label, field2Initial, field2KeyboardType, confirmText, dismissText, onConfirm, onDismiss)
