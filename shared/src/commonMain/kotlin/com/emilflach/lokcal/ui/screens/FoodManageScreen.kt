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
import androidx.compose.material.icons.filled.Add
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
import com.emilflach.lokcal.viewmodel.FoodEditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FoodManageScreen(
    viewModel: FoodEditViewModel,
    onBack: () -> Unit,
    onOpenEdit: (Long?) -> Unit,
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader(LocalImageCache.current)
    val coroutineScope = rememberCoroutineScope()

    val search by viewModel.search.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val frequencies by viewModel.itemFrequencies.collectAsState()
    val filterMissingImages by viewModel.filterMissingImages.collectAsState()
    val cachedImageFoodIds by viewModel.cachedImageFoodIds.collectAsState()

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

    val displayedFoods = if (filterMissingImages)
        foods.filter { it.image_url.isNullOrBlank() && it.id !in cachedImageFoodIds }
    else foods

    PlatformScaffold(
        topBar = {
            MealTopBar(
                title = "Foods",
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
                    IconButton(onClick = { onOpenEdit(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add food")
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
            items(displayedFoods, key = { it.id }) { food ->
                ListItem(
                    leadingContent = {
                        if (!food.image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = EntityImageData(EntityImageData.FOOD, food.id, food.image_url),
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
                    headlineContent = { Text(food.name) },
                    supportingContent = {
                        val kcal = food.energy_kcal_per_100g
                        val servingSize = food.serving_size
                        val freq = frequencies["FOOD" to food.id] ?: 0
                        Text("${kcal.toInt()} kcal • ${servingSize}g • $freq times")
                    },
                    modifier = Modifier.clip(
                        getRoundedCornerShape(
                            index = displayedFoods.indexOf(food),
                            size = displayedFoods.size
                        )
                    ).clickable { onOpenEdit(food.id) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
