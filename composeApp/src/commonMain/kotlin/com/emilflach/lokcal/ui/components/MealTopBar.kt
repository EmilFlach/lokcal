package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealTopBar(
    title: String,
    onBack: () -> Unit,
    showSearch: Boolean,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    autoFocusSearch: Boolean = false,
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
    LaunchedEffect(autoFocusSearch) {
        if (autoFocusSearch && showSearch) focusRequester.requestFocus()
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

        if (showSearch) {
            val color = LocalRecipesColors.current
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
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.padding(start = 16.dp)
                    )
                },
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
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .focusRequester(focusRequester)
            )
        }
    }
}
