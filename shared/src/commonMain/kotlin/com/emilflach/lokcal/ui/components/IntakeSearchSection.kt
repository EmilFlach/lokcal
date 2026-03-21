package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.viewmodel.IntakeViewModel
import com.emilflach.lokcal.viewmodel.OnlineSearchManager

@Stable
class FocusRequesters {
    private val map = mutableStateMapOf<Any, FocusRequester>()
    operator fun get(key: Any): FocusRequester = map.getOrPut(key) { FocusRequester() }
    fun request(key: Any) { map[key]?.requestFocus() }
}

fun LazyListScope.searchSection(
    title: String,
    section: OnlineSearchManager.SearchSection,
    viewModel: IntakeViewModel,
    requesters: FocusRequesters,
    onDone: (itemAdded: Boolean) -> Unit
) {

    item {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val color = LocalRecipesColors.current
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = color.foregroundSupport,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (section.error != null) {
        item {
            val color = LocalRecipesColors.current
            Text(
                text = section.error,
                style = MaterialTheme.typography.bodyMedium,
                color = color.foregroundSupport,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    } else if (section.isSearching) {
        item {
            val color = LocalRecipesColors.current
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = color.foregroundSupport,
                trackColor = color.backgroundSurface1
            )
        }
    } else if (section.noResults && section.foods.isEmpty()) {
        item {
            Text(
                text = "No results found",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalRecipesColors.current.foregroundSupport,
                modifier = Modifier.padding(bottom = 16.dp)
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
