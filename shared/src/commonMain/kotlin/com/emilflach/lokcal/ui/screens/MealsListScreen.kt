package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.ui.util.EntityImageData
import com.emilflach.lokcal.ui.util.LocalImageCache
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.viewmodel.MealsListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MealsListScreen(
    viewModel: MealsListViewModel,
    onBack: () -> Unit,
    onOpenMeal: (Long) -> Unit,
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader(LocalImageCache.current)
    val coroutineScope = rememberCoroutineScope()

    val search by viewModel.search.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val frequencies by viewModel.itemFrequencies.collectAsState()
    val filterMissingImages by viewModel.filterMissingImages.collectAsState()
    val cachedImageMealIds by viewModel.cachedImageMealIds.collectAsState()

    val listStateData by viewModel.listState.collectAsState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = listStateData.keys.firstOrNull() ?: 0,
        initialFirstVisibleItemScrollOffset = listStateData.values.firstOrNull() ?: 0
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                viewModel.saveListState(index, offset)
            }
    }

    BackHandler {
        onBack()
    }

    val displayedMeals = if (filterMissingImages)
        meals.filter { it.image_url.isNullOrBlank() && it.id !in cachedImageMealIds }
    else meals

    PlatformScaffold(
        topBar = {
            MealTopBar(
                title = "Meals",
                onBack = onBack,
                showSearch = true,
                showOnlineSearch = false,
                query = search,
                onQueryChange = { viewModel.setSearch(it) },
                onClearQuery = { coroutineScope.launch { listState.scrollToItem(0) } },
                trailingActions = {
                    IconButton(onClick = { viewModel.toggleMissingImagesFilter() }) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "Filter",
                            tint = if (filterMissingImages) colors.foregroundBrand else colors.foregroundDefault
                        )
                    }
                }
            )
        },
        scrollState = listState,
        navBarBackgroundColor = colors.backgroundPage
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = inner.listContentPadding(),
        ) {
            items(displayedMeals, key = { it.id }) { meal ->
                ListItem(
                    leadingContent = {
                        if (!meal.image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = EntityImageData(EntityImageData.MEAL, meal.id, meal.image_url),
                                contentDescription = null,
                                imageLoader = imageLoader,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(35.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(colors.backgroundSurface2)
                            )
                        }
                    },
                    headlineContent = { Text(meal.name) },
                    supportingContent = {
                        val portions = meal.total_portions
                        var totals by remember(meal.id) { mutableStateOf(0.0 to 0.0) }
                        LaunchedEffect(meal.id) {
                            totals = viewModel.computeMealTotals(meal.id)
                        }
                        val (grams, kcal) = totals
                        val freq = frequencies["MEAL" to meal.id] ?: 0
                        Text("${portions.toInt()} portions, $kcal kcal, ${grams.toInt()}g • $freq times")
                    },
                    modifier = Modifier.clip(
                        getRoundedCornerShape(
                            index = displayedMeals.indexOf(meal),
                            size = displayedMeals.size
                        )
                    ).clickable { onOpenMeal(meal.id) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
