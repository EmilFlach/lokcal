package com.emilflach.lokcal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.emilflach.lokcal.util.NumberUtils.sanitizeDecimalInput

@Composable
fun PortionsTextField(
    tf: TextFieldValue,
    requester: FocusRequester,
    onValueChange: (TextFieldValue, String) -> Unit,
    onDone: () -> Unit = {},
) {

    IntakeTextField(
        tf = tf,
        requester = requester,
        onValueChange = { newTf, text ->
            val cleaned = sanitizeDecimalInput(text)
            val updatedTf = newTf.copy(
                text = cleaned,
                selection = if (text != cleaned) TextRange(cleaned.length) else newTf.selection
            )
            onValueChange(updatedTf, cleaned)
        },
        unit = "x",
        onDone = onDone,
    )
}
