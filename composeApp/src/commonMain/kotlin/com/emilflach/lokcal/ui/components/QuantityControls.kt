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
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.util.NumberUtils.parseDecimal
import com.emilflach.lokcal.viewmodel.MealTimeViewModel

@Composable
fun MealQuantityControls(requester: FocusRequester = FocusRequester(), viewModel: MealTimeViewModel, entry: Intake) {
    val colors = LocalRecipesColors.current
    val portion = viewModel.portionForEntry(entry)
    var tf by rememberSaveable(entry.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = viewModel.getPortions(entry)))
    }

    fun updatePortions(newTf: TextFieldValue, text: String) {
        tf = newTf
        if(text.isEmpty()) return
        val p = parseDecimal(text)
        val grams = p * portion
        viewModel.updateQuantity(entry.id, grams)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        PortionsTextField(
            tf = tf,
            requester = requester,
            onValueChange = { newTf, value ->
                updatePortions(newTf,value)
            }
        )

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                val newText = viewModel.addMealPortion(entry)
                updatePortions(tf.copy(text = newText), newText)
            },
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion")
            Spacer(Modifier.width(4.dp))
            Text(text = "1")
        }

        Spacer(Modifier.width(8.dp))

        OutlinedIconButton(
            onClick = {
                val newText = viewModel.subtractMealPortion(entry)
                updatePortions(tf.copy(text = newText), newText)
            }
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion")
        }

        IconButton(onClick = { viewModel.deleteItem(entry.id) }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete item",
                tint = colors.foregroundSupport
            )
        }
    }
}

@Composable
fun FoodQuantityControls(requester: FocusRequester = FocusRequester(), viewModel: MealTimeViewModel, entry: Intake) {
    val colors = LocalRecipesColors.current
    val portion = viewModel.portionForEntry(entry)
    var tf by rememberSaveable(entry.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = entry.quantity_g.toInt().toString()))
    }

    fun updateGrams(newTf: TextFieldValue, text: String) {
        tf = newTf
        if(text.isEmpty()) return
        val g = parseDecimal(text)
        viewModel.updateQuantity(entry.id, g)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        GramTextField(
            tf = tf,
            requester = requester,
            onValueChange = { newTf, value ->
                updateGrams(newTf, value)
            }
        )
        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                val newText = viewModel.addPortion(entry)
                updateGrams(tf.copy(text = newText), newText)
            },
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion")
            Spacer(Modifier.width(4.dp))
            Text(text = "${portion.toInt()}g")
        }

        Spacer(Modifier.width(8.dp))

        OutlinedIconButton(
            onClick = {
                val newText = viewModel.subtractPortion(entry)
                updateGrams(tf.copy(text = newText), newText)
            }
        ) {
            Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion")
        }

        IconButton(onClick = { viewModel.deleteItem(entry.id) }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Delete item",
                tint = colors.foregroundSupport
            )
        }
    }
}