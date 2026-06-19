package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
expect fun SingleInputAlertDialog(
    title: String,
    fieldLabel: String,
    initialValue: String,
    confirmText: String,
    dismissText: String,
    keyboardType: KeyboardType,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
)

@Composable
expect fun DualInputAlertDialog(
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
)

@Composable
internal fun SingleInputAlertDialogCompose(
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
    var value by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(fieldLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.focusRequester(focusRequester)
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }
}

@Composable
expect fun InfoAlertDialog(
    title: String,
    body: String,
    confirmText: String,
    onDismiss: () -> Unit
)

@Composable
internal fun InfoAlertDialogCompose(
    title: String,
    body: String,
    confirmText: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(confirmText) }
        }
    )
}

@Composable
internal fun DualInputAlertDialogCompose(
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
    var value1 by remember { mutableStateOf(field1Initial) }
    var value2 by remember { mutableStateOf(field2Initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(value1, value2) }) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
        title = { Text(title) },
        text = {
            Column {
                Text(field1Label)
                OutlinedTextField(
                    value = value1,
                    onValueChange = { value1 = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = field1KeyboardType),
                )
                Spacer(Modifier.height(16.dp))
                Text(field2Label)
                OutlinedTextField(
                    value = value2,
                    onValueChange = { value2 = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = field2KeyboardType),
                )
            }
        }
    )
}
