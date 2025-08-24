package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.viewmodel.MealDetailViewModel

@Composable
fun MealDetailItem(
    entry: Intake,
    viewModel: MealDetailViewModel,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader()

    var gramsText by remember(entry.id, entry.quantity_g) { mutableStateOf(entry.quantity_g.toInt().toString()) }

    fun persistIfValid(text: String) {
        val g = viewModel.parseGrams(text)
        viewModel.updateQuantity(entry.id, g)
    }

    val keyboard = LocalSoftwareKeyboardController.current
    val requester = remember(entry.id) { FocusRequester() }

    var tf by rememberSaveable(entry.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = gramsText))
    }

    val portion = viewModel.portionForEntry(entry)
    val portionInt = portion.toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.backgroundSurface1)
            .clickable {
                requester.requestFocus()
                keyboard?.show()
            }
            .height(IntrinsicSize.Min)
            .padding(top = 12.dp, bottom = 12.dp, start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = remember(entry.source_food_id) {
            entry.source_food_id?.let { viewModel.imageUrlForFoodId(it) }
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.backgroundSurface2)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.item_name,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.foregroundDefault,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${entry.energy_kcal_total.toInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = colors.foregroundSupport,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GramTextField(
                    tf = tf,
                    requester = requester,
                    onValueChange = { newTf, value ->
                        tf = newTf
                        gramsText = value
                        if (value.isNotEmpty()) persistIfValid(value)
                    }
                )
                Spacer(Modifier.weight(1f))

                OutlinedButton(
                    onClick = {
                        val newText = viewModel.addPortionText(gramsText, entry)
                        gramsText = newText
                        tf = tf.copy(text = newText, selection = TextRange(newText.length))
                        persistIfValid(newText)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    border = BorderStroke(1.dp, colors.foregroundSupport),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foregroundSupport),
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion of the food")
                    Spacer(Modifier.width(4.dp))
                    Text(text = "${portionInt}g")
                }
                Spacer(Modifier.width(8.dp))

                OutlinedIconButton(
                    border = BorderStroke(1.dp, colors.foregroundSupport),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = colors.foregroundSupport),
                    onClick = {
                        val newText = viewModel.subtractPortionText(gramsText, entry)
                        gramsText = newText
                        tf = tf.copy(text = newText, selection = TextRange(newText.length))
                        persistIfValid(newText)
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion of the food")
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
    }
}
