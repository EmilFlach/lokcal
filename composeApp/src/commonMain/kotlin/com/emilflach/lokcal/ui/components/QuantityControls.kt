package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.data.PortionService
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.util.NumberUtils.formatDecimalTrimmed
import com.emilflach.lokcal.util.NumberUtils.parseDecimal

@Composable
fun GramQuantityControls(
    requester: FocusRequester,
    stateKey: Any,
    initialGrams: Double,
    portionGrams: Double,
    onCommitGrams: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    val portionService = PortionService()
    val colors = LocalRecipesColors.current
    val haptic = LocalHapticFeedback.current
    var text by rememberSaveable(stateKey) { mutableStateOf(initialGrams.toInt().toString()) }
    var tf by rememberSaveable(stateKey, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = text))
    }

    fun commitFromText(text: String) {
        val g = parseDecimal(text)
        onCommitGrams(g)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        GramTextField(
            tf = tf,
            requester = requester,
            onValueChange = { newTf, value ->
                tf = newTf
                text = value
                commitFromText(value)
            },
            onDone = {
                commitFromText(tf.text)
            }
        )
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                val (newText, commitVal) = portionService.addPortionGrams(text, portionGrams)
                tf = tf.copy(text = newText)
                text = newText
                onCommitGrams(commitVal)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion")
            Spacer(Modifier.width(4.dp))
            Text(text = "${portionGrams.toInt()}g")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton(
            onClick = {
                val (newText, commitVal) = portionService.subtractPortionGrams(text, portionGrams)
                tf = tf.copy(text = newText)
                text = newText
                onCommitGrams(commitVal)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion")
        }
        IconButton(onClick = {
            onDelete()
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete item",
                tint = colors.foregroundSupport
            )
        }
    }
}

@Composable
fun PortionQuantityControls(
    requester: FocusRequester,
    stateKey: Any,
    initialGrams: Double,
    portionGrams: Double,
    onCommitPortions: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalRecipesColors.current
    val haptic = LocalHapticFeedback.current
    val portionService = PortionService()
    val initialPortions = portionService.portions(initialGrams, portionGrams)
    var text by rememberSaveable(stateKey) { mutableStateOf(formatDecimalTrimmed(initialPortions)) }
    var tf by rememberSaveable(stateKey, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = text))
    }

    fun commitFromText(text: String) {
        val portions = parseDecimal(text)
        onCommitPortions(portions)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        PortionsTextField(
            tf = tf,
            requester = requester,
            onValueChange = { newTf, value ->
                tf = newTf
                text = value
                commitFromText(value)
            },
            onDone = {
                commitFromText(tf.text)
            }
        )

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                val (newText, commitVal) = portionService.addPortionCount(text)
                tf = tf.copy(text = newText)
                text = newText
                onCommitPortions(commitVal)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion")
            Spacer(Modifier.width(4.dp))
            Text(text = "1")
        }

        Spacer(Modifier.width(8.dp))

        OutlinedIconButton(
            onClick = {
                val (newText, commitVal) = portionService.subtractPortionCount(text)
                tf = tf.copy(text = newText)
                text = newText
                onCommitPortions(commitVal)
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            }
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion")
        }

        IconButton(onClick = {
            onDelete()
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete item",
                tint = colors.foregroundSupport
            )
        }
    }
}