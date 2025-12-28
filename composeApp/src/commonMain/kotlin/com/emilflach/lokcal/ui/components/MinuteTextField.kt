package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun MinuteTextField(
    tf: TextFieldValue,
    requester: FocusRequester,
    onValueChange: (TextFieldValue, String) -> Unit,
    onDone: () -> Unit = {},
) {
    IntakeTextField(
        tf = tf,
        requester = requester,
        onValueChange = { newTf, text ->
            val cleaned = text.filter { it.isDigit() }.take(4)
            val updatedTf = newTf.copy(
                text = cleaned,
                selection = if (text != cleaned) TextRange(cleaned.length) else newTf.selection
            )
            onValueChange(updatedTf, cleaned)
        },
        unit = "min",
        onDone = onDone,
    )
}
