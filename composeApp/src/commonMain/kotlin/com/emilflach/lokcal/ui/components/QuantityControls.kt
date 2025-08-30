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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.util.NumberUtils.formatDecimalTrimmed
import com.emilflach.lokcal.util.NumberUtils.parseDecimal
import com.emilflach.lokcal.util.PortionsCalculator

@Composable
fun GramQuantityControls(
    requester: FocusRequester,
    stateKey: Any,
    initialGrams: Double,
    portionGrams: Double,
    onCommitGrams: (Double) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalRecipesColors.current
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
            onDone = { commitFromText(tf.text) }
        )
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                val (newText, commitVal) = PortionsCalculator.addPortionGrams(text, portionGrams)
                tf = tf.copy(text = newText)
                text = newText
                onCommitGrams(commitVal)
            },
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion")
            Spacer(Modifier.width(4.dp))
            Text(text = "${portionGrams.toInt()}g")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton(
            onClick = {
                val (newText, commitVal) = PortionsCalculator.subtractPortionGrams(text, portionGrams)
                tf = tf.copy(text = newText)
                text = newText
                onCommitGrams(commitVal)
            }
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion")
        }
        IconButton(onClick = onDelete) {
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
    val initialPortions = PortionsCalculator.portions(initialGrams, portionGrams)
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
            onDone = { commitFromText(tf.text) }
        )

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                val (newText, commitVal) = PortionsCalculator.addPortionCount(text)
                tf = tf.copy(text = newText)
                text = newText
                onCommitPortions(commitVal)
            },
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion")
            Spacer(Modifier.width(4.dp))
            Text(text = "1")
        }

        Spacer(Modifier.width(8.dp))

        OutlinedIconButton(
            onClick = {
                val (newText, commitVal) = PortionsCalculator.subtractPortionCount(text)
                tf = tf.copy(text = newText)
                text = newText
                onCommitPortions(commitVal)
            }
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion")
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete item",
                tint = colors.foregroundSupport
            )
        }
    }
}