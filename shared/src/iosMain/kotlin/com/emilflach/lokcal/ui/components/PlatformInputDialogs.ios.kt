package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.input.KeyboardType
import platform.UIKit.*

private fun KeyboardType.toUIKeyboardType() = when (this) {
    KeyboardType.Number -> UIKeyboardTypeNumberPad
    KeyboardType.Decimal -> UIKeyboardTypeDecimalPad
    else -> UIKeyboardTypeDefault
}

@Composable
actual fun SingleInputAlertDialog(
    title: String,
    fieldLabel: String,
    initialValue: String,
    confirmText: String,
    dismissText: String,
    keyboardType: KeyboardType,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = null,
            preferredStyle = UIAlertControllerStyleAlert
        )
        alert.addTextFieldWithConfigurationHandler { textField ->
            textField?.placeholder = fieldLabel
            textField?.text = initialValue
            textField?.keyboardType = keyboardType.toUIKeyboardType()
        }
        alert.addAction(UIAlertAction.actionWithTitle(confirmText, UIAlertActionStyleDefault) {
            val value = (alert.textFields?.firstOrNull() as? UITextField)?.text ?: ""
            onConfirm(value)
            onDismiss()
        })
        alert.addAction(UIAlertAction.actionWithTitle(dismissText, UIAlertActionStyleCancel) {
            onDismiss()
        })
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            alert, animated = true, completion = null
        )
        onDispose {
            if (alert.presentingViewController != null) {
                alert.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
}

@Composable
actual fun InfoAlertDialog(
    title: String,
    body: String,
    confirmText: String,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = body,
            preferredStyle = UIAlertControllerStyleAlert
        )
        alert.addAction(UIAlertAction.actionWithTitle(confirmText, UIAlertActionStyleDefault) {
            onDismiss()
        })
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            alert, animated = true, completion = null
        )
        onDispose {
            if (alert.presentingViewController != null) {
                alert.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
}

@Composable
actual fun DualInputAlertDialog(
    title: String,
    field1Label: String,
    field1Initial: String,
    field1KeyboardType: KeyboardType,
    field2Label: String,
    field2Initial: String,
    field2KeyboardType: KeyboardType,
    confirmText: String,
    dismissText: String,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    DisposableEffect(Unit) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = null,
            preferredStyle = UIAlertControllerStyleAlert
        )
        alert.addTextFieldWithConfigurationHandler { textField ->
            textField?.placeholder = field1Label
            textField?.text = field1Initial
            textField?.keyboardType = field1KeyboardType.toUIKeyboardType()
        }
        alert.addTextFieldWithConfigurationHandler { textField ->
            textField?.placeholder = field2Label
            textField?.text = field2Initial
            textField?.keyboardType = field2KeyboardType.toUIKeyboardType()
        }
        alert.addAction(UIAlertAction.actionWithTitle(confirmText, UIAlertActionStyleDefault) {
            val value1 = (alert.textFields?.getOrNull(0) as? UITextField)?.text ?: ""
            val value2 = (alert.textFields?.getOrNull(1) as? UITextField)?.text ?: ""
            onConfirm(value1, value2)
            onDismiss()
        })
        alert.addAction(UIAlertAction.actionWithTitle(dismissText, UIAlertActionStyleCancel) {
            onDismiss()
        })
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            alert, animated = true, completion = null
        )
        onDispose {
            if (alert.presentingViewController != null) {
                alert.dismissViewControllerAnimated(true, completion = null)
            }
        }
    }
}
