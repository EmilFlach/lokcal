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
import com.emilflach.lokcal.ui.components.GramTextField
import com.emilflach.lokcal.ui.components.IntakeListItem
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.ui.components.PortionsTextField
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntakeScreen(
    viewModel: IntakeViewModel,
    onDone: () -> Unit,
    autoFocusSearch: Boolean = false,
) {
    val colors = LocalRecipesColors.current
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
                .background(colors.backgroundPage)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {

            val gramsById = remember { mutableStateMapOf<Long, String>() }
            val requesters = remember { FocusRequesters() }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val totalSize = state.meals.size + state.foods.size

                itemsIndexed(items = state.meals) { index, item ->
                    val keyId = -item.id // Negative to avoid collision with food IDs
                    val initialPortions = gramsById.getOrPut(keyId) { "1" }

                    IntakeListItem(
                        name = item.name,
                        subtitle = viewModel.subtitleForMeal(item, initialPortions),
                        keyId = keyId,
                        initialValue = initialPortions,
                        showBorder = true,
                        index = index,
                        size = totalSize,
                        requesters = requesters,
                        gramsById = gramsById,
                        onAddClick = {
                            viewModel.addMealByPortions(item.id, initialPortions) { onDone() }
                        },
                        inputField = { tf, requester, onValueChange, onDone ->
                            PortionsTextField(tf, requester, onValueChange, onDone)
                        }
                    )
                }

                itemsIndexed(items = state.foods) { index, item ->
                    val keyId = item.id
                    val initialGrams = gramsById.getOrPut(keyId) {
                        viewModel.defaultPortionGrams(item).toInt().toString()
                    }

                    IntakeListItem(
                        name = item.name,
                        subtitle = viewModel.subtitleForFood(item, initialGrams),
                        keyId = keyId,
                        initialValue = initialGrams,
                        index = state.meals.size + index,
                        size = totalSize,
                        requesters = requesters,
                        gramsById = gramsById,
                        onAddClick = {
                            viewModel.addFoodByGrams(item.id, initialGrams) { onDone() }
                        },
                        inputField = { tf, requester, onValueChange, onDone ->
                            GramTextField(tf, requester, onValueChange, onDone)
                        }
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
