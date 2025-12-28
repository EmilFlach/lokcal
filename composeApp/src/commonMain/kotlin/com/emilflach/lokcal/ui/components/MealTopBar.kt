package com.emilflach.lokcal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTopBar(
    title: String,
    onBack: () -> Unit,
    showSearch: Boolean,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    autoFocusSearch: Boolean = false,
    onSearchOnline: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    isSearchingOnline: Boolean = false,
    trailingActions: (@Composable () -> Unit)? = null,
    colors: TopAppBarColors = run {
        val color = LocalRecipesColors.current
        TopAppBarDefaults.topAppBarColors(
            containerColor = color.backgroundPage,
            scrolledContainerColor = color.backgroundSurface1,
            titleContentColor = color.foregroundDefault,
            navigationIconContentColor = color.foregroundDefault,
            actionIconContentColor = color.foregroundDefault,
        )
    },
) {
    val focusRequester = remember { FocusRequester() }
    var searchVisible by remember { mutableStateOf(false) }

    LaunchedEffect(showSearch) {
        searchVisible = showSearch
    }

    LaunchedEffect(autoFocusSearch, searchVisible) {
        if (autoFocusSearch && searchVisible) {
            delay(10)
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy((-1).dp)
    ) {
        TopAppBar(
            modifier = Modifier
                .fillMaxWidth(),
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            title = {
                Text(title.lowercase().replaceFirstChar { it.titlecase() })
            },
            actions = {
                trailingActions?.invoke()
            },
            colors = colors
        )

        AnimatedVisibility(
            visible = searchVisible,
            enter = fadeIn(animationSpec = tween(250))
        ) {
            val color = LocalRecipesColors.current
            Row {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = color.backgroundSurface1,
                        unfocusedContainerColor = color.backgroundSurface1,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            onQueryChange("")
                            focusRequester.requestFocus()
                        }, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",

                                )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp, end = 8.dp, bottom = 16.dp)
                        .focusRequester(focusRequester)
                )
                Surface(
                    onClick = { onSearchOnline() },
                    color = color.backgroundSurface2,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .height(57.dp)
                        .width(57.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSearchingOnline) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Stop searching",
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.ManageSearch,
                                contentDescription = "Search the internet",
                            )
                        }
                    }
                }
                Surface(
                    onClick = { onScanBarcode() },
                    color = color.backgroundSurface2,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .clip(MaterialTheme.shapes.small)
                        .height(57.dp)
                        .width(47.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Scan a barcode",
                        )
                    }
                }
            }
        }
    }
}
