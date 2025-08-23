package com.emilflach.lokcal.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: () -> Unit,
    autoFocusSearch: Boolean = false,
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            com.emilflach.lokcal.ui.components.MealTopBar(
                title = state.selectedMealType,
                onBack = onDone,
                showSearch = true,
                query = state.query,
                onQueryChange = viewModel::setQuery,
                autoFocusSearch = autoFocusSearch,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            val gramsById = remember { mutableStateMapOf<Long, String>() }
            val portionsById = remember { mutableStateMapOf<Long, String>() }
            val requesters = rememberFocusRequesters()
            val keyboard = LocalSoftwareKeyboardController.current

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.foods,
                    key = { it.id },
                    contentType = { "food" }
                ) { item ->
                    val defaultPortionG: Double = viewModel.defaultPortionGrams(item)
                    val initialGrams = gramsById[item.id] ?: defaultPortionG.toInt().toString()
                    if (!gramsById.containsKey(item.id)) gramsById[item.id] = initialGrams
                    val initialPortions = portionsById[item.id] ?: "1"
                    if (!portionsById.containsKey(item.id)) portionsById[item.id] = initialPortions

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                requesters.request(item.id)
                                keyboard?.show()
                            }
                            .padding(16.dp),
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
                                Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = viewModel.buildSubtitle(
                                        food = item,
                                        gramsText = gramsById[item.id] ?: initialGrams,
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            var tf by rememberSaveable(item.id, stateSaver = TextFieldValue.Saver) {
                                mutableStateOf(
                                    TextFieldValue(
                                        text = gramsById[item.id] ?: initialGrams
                                    )
                                )
                            }

                            val onAddClick: () -> Unit = {
                                val gramsToAdd = viewModel.parseGrams(gramsById[item.id] ?: initialGrams)
                                if (gramsToAdd > 0.0) {
                                    viewModel.logPortion(item.id, gramsToAdd)
                                    onDone()
                                }
                            }

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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(50.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
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

                            FilledIconButton(modifier = Modifier.size(50.dp), onClick = {
                                onAddClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add portion"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Stable
class FocusRequesters {
    private val map = mutableStateMapOf<Any, FocusRequester>()
    operator fun get(key: Any): FocusRequester = map.getOrPut(key) { FocusRequester() }
    fun request(key: Any) { map[key]?.requestFocus() }
}

@Composable
fun rememberFocusRequesters(): FocusRequesters = remember { FocusRequesters() }
