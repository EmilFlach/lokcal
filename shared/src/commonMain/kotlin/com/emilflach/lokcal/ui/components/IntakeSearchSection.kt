package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Stable
class FocusRequesters {
    private val map = mutableStateMapOf<Any, FocusRequester>()
    operator fun get(key: Any): FocusRequester = map.getOrPut(key) { FocusRequester() }
    fun request(key: Any) { map[key]?.requestFocus() }
}

fun LazyListScope.searchSection(
    title: String,
    section: IntakeViewModel.SearchSection,
    viewModel: IntakeViewModel,
    requesters: FocusRequesters,
    onDone: (itemAdded: Boolean) -> Unit
) {
    item {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = LocalRecipesColors.current.foregroundSupport,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    if (section.isSearching) {
        item {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = LocalRecipesColors.current.foregroundDefault,
                strokeWidth = 2.dp
            )
        }
    } else if (section.error != null) {
        item {
            Text(
                text = section.error,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalRecipesColors.current.foregroundSupport,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    } else if (section.noResults) {
        item {
            Text(
                text = "No matching results found",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalRecipesColors.current.foregroundSupport,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    itemsIndexed(items = section.foods) { index, item ->
        FoodIntakeListItem(
            food = item,
            viewModel = viewModel,
            index = index,
            size = section.foods.size,
            requesters = requesters,
            onDone = onDone
        )
    }
}
