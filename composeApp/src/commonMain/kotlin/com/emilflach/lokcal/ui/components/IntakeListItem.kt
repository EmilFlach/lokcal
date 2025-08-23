package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.FocusRequesters
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Composable
fun IntakeListItem(
    item: Food,
    index: Int,
    size: Int,
    viewModel: IntakeViewModel,
    gramsById: MutableMap<Long, String>,
    requesters: FocusRequesters,
    onDone: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val initialGrams = gramsById.getOrPut(item.id) { viewModel.defaultPortionGrams(item).toInt().toString() }
    var tf by rememberSaveable(item.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = gramsById.getOrPut(item.id) { initialGrams }
            )
        )
    }
    val onAddClick = {
        val gramsToAdd = viewModel.parseGrams(gramsById.getOrPut(item.id) { initialGrams })
        if (gramsToAdd > 0.0) {
            viewModel.logPortion(item.id, gramsToAdd)
            onDone()
        }
    }

    val colors = LocalRecipesColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(getRoundedCornerShape(index, size))
            .background(colors.backgroundSurface1)
            .clickable {
                requesters.request(item.id)
                keyboard?.show()
            }
            .padding(vertical = 24.dp, horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = item.name, style = MaterialTheme.typography.bodyLarge, color = colors.foregroundDefault)
                Text(
                    text = viewModel.buildSubtitle(
                        food = item,
                        gramsText = gramsById.getOrPut(item.id) { initialGrams },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foregroundSupport
                )
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
                        val cleaned = newVal.text.filter { it.isDigit() }.take(5)
                        gramsById[item.id] = cleaned
                        tf = newVal.copy(
                            text = cleaned,
                            selection = TextRange(
                                newVal.selection.start.coerceIn(
                                    0,
                                    cleaned.length
                                )
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onAddClick()
                        }
                    ),

                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.foregroundDefault,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(colors.foregroundDefault),
                    modifier = Modifier
                        .width(50.dp)
                        .height(50.dp)
                        .background(
                            colors.backgroundSurface2,
                            MaterialTheme.shapes.small
                        )
                        .focusRequester(requesters[item.id])
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                tf = tf.copy(selection = TextRange(0, tf.text.length))
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onAddClick()
                                true
                            } else {
                                false
                            }
                        },
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            innerTextField()
                        }
                    }
                )
            }

            val btnColors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.backgroundBrand,
                contentColor = colors.onBackgroundBrand
            )
            FilledIconButton(modifier = Modifier.size(50.dp), colors = btnColors, onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add portion"
                )
            }
        }
    }
}

@Composable
fun getRoundedCornerShape(index: Int, size: Int): Shape {
    return when {
        index == 0 && size == 1 -> RoundedCornerShape(4.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )
        index == size - 1 -> RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
            topStart = 4.dp,
            topEnd = 4.dp
        )
        else -> RoundedCornerShape(4.dp)
    }
}
