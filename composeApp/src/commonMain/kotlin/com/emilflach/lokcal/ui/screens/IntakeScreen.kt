package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
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
import com.emilflach.lokcal.ui.components.IntakeListItem
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

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior()

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
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            val gramsById = remember { mutableStateMapOf<Long, String>() }
            val requesters = rememberFocusRequesters()

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    items = state.foods,
                    key = { it.id },
                    contentType = { "food" }
                ) { item ->
                    IntakeListItem(
                        item = item,
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
