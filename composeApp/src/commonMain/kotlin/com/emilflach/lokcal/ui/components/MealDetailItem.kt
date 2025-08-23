package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
            .padding(vertical = 12.dp, horizontal = 12.dp),
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
                color = colors.foregroundDefault
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${entry.energy_kcal_total.toInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                color = colors.foregroundSupport
            )
            Spacer(Modifier.height(8.dp))

            var tf by rememberSaveable(entry.id, stateSaver = TextFieldValue.Saver) {
                mutableStateOf(TextFieldValue(text = gramsText))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicTextField(
                    value = tf,
                    onValueChange = { newVal ->
                        val cleaned = viewModel.sanitizeGramsInput(newVal.text)
                        gramsText = cleaned
                        tf = newVal.copy(
                            text = cleaned,
                            selection = TextRange(
                                newVal.selection.start.coerceIn(0, cleaned.length)
                            )
                        )
                        if (cleaned.isNotEmpty()) persistIfValid(cleaned)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.foregroundDefault, textAlign = TextAlign.Center),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.foregroundDefault),
                    modifier = Modifier
                        .width(64.dp)
                        .height(40.dp)
                        .background(colors.backgroundSurface2, MaterialTheme.shapes.small)
                        .focusRequester(requester)
                        .onFocusChanged { st ->
                            if (st.isFocused) {
                                tf = tf.copy(selection = TextRange(0, tf.text.length))
                            }
                        },
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // shift numeric left a bit to compensate for trailing g
                            Box(Modifier.padding(end = 10.dp)) { inner() }
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

                Spacer(Modifier.width(16.dp))

                val portion = viewModel.portionForEntry(entry)
                val portionInt = portion.toInt()
                OutlinedButton(
                    onClick = {
                        val newText = viewModel.addPortionText(gramsText, entry)
                        gramsText = newText
                        tf = tf.copy(text = newText, selection = TextRange(newText.length))
                        persistIfValid(newText)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.backgroundSurface1,
                        contentColor = colors.foregroundDefault,
                    )
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(text = "${portionInt}g")
                }
                Spacer(Modifier.weight(1f))

                IconButton(onClick = { viewModel.deleteItem(entry.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = colors.foregroundSupport
                    )
                }
            }
        }
    }
}
