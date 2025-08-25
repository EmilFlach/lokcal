package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.IntakeListItem
import com.emilflach.lokcal.ui.components.IntakeMealListItem
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: () -> Unit,
    autoFocusSearch: Boolean = false,
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MealTopBar(
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
                .background(LocalRecipesColors.current.backgroundPage)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            val gramsById = remember { mutableStateMapOf<Long, String>() }
            val requesters = rememberFocusRequesters()

            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val totalSize = state.meals.size + state.foods.size
                itemsIndexed(items = state.meals) { index, item ->
                    IntakeMealListItem(
                        item = item,
                        index = index,
                        size = totalSize,
                        viewModel = viewModel,
                        gramsById = gramsById,
                        requesters = requesters,
                        onDone = onDone,
                    )
                }
                itemsIndexed(items = state.foods) { index, item ->
                    IntakeListItem(
                        item = item,
                        index = state.meals.size + index,
                        size = totalSize,
                        viewModel = viewModel,
                        gramsById = gramsById,
                        requesters = requesters,
                        onDone = onDone,
                    )
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
