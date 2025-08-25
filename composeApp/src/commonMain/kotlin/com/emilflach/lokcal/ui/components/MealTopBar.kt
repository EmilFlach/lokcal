package com.emilflach.lokcal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.lerp
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
    scrollBehavior: TopAppBarScrollBehavior? = null,
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
            colors = colors,
            scrollBehavior = scrollBehavior,
        )

        if (showSearch) {
            TopAppBarSurface(colors = colors, scrollBehavior = scrollBehavior) {
                val color = LocalRecipesColors.current
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search food") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color.borderFocus,
                        focusedTextColor = color.foregroundDefault,
                        focusedLabelColor = color.foregroundDefault,
                        cursorColor = color.foregroundDefault,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .focusRequester(focusRequester)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBarSurface(
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val colorTransitionFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
    val fraction = if (colorTransitionFraction > 0.01f) 1f else 0f
    val appBarContainerColor by animateColorAsState(
        targetValue = lerp(
            colors.containerColor,
            colors.scrolledContainerColor,
            FastOutLinearInEasing.transform(fraction),
        ),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TopBarSurfaceContainerColorAnimation",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = appBarContainerColor,
        shadowElevation = if (colorTransitionFraction > 0f) 8.dp else 0.dp,
        content = content,
    )
}
