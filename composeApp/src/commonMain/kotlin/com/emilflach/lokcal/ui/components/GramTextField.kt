package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@Composable
fun GramTextField(
    tf: TextFieldValue,
    requester: FocusRequester,
    onValueChange: (TextFieldValue, String) -> Unit,
    onDone: () -> Unit = {},
) {
    val colors = LocalRecipesColors.current
    fun sanitizeGramsInput(text: String, maxDigits: Int = 5): String = text.filter { it.isDigit() }.take(maxDigits)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun handleDone() {
        keyboard?.hide()
        focusManager.clearFocus()
        onDone()
    }

    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.foregroundDefault,
            backgroundColor = colors.foregroundDefault.copy(alpha = 0.2f)
        )
    ) {
        BasicTextField(
            value = tf,
            onValueChange = { newVal ->
                val cleaned = sanitizeGramsInput(newVal.text)
                val updatedTf = newVal.copy(
                    text = cleaned,
                    selection = TextRange(
                        newVal.selection.start.coerceIn(0, cleaned.length)
                    )
                )
                onValueChange(updatedTf, cleaned)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = colors.foregroundDefault,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { handleDone() }),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.foregroundDefault),
            modifier = Modifier
                .width(64.dp)
                .height(50.dp)
                .background(colors.backgroundSurface2, MaterialTheme.shapes.small)
                .focusRequester(requester)
                .onFocusChanged { st ->
                    if (st.isFocused) {
                        onValueChange(tf.copy(selection = TextRange(0, tf.text.length)), tf.text)
                    }
                }.onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp &&
                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        handleDone()
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.padding(end = 8.dp)) { inner() }
                    Text(
                        text = "g",
                        color = colors.foregroundSupport,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                    )
                }
            }
        )
    }
}