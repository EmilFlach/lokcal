package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.util.getTopPaddingForNativeNavigation
import com.emilflach.lokcal.util.usesNativeNavigation
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
    val imageLoader = rememberKtorImageLoader()
    val coroutineScope = rememberCoroutineScope()

    val search by viewModel.search.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val frequencies by viewModel.itemFrequencies.collectAsState()
    val missingImages by viewModel.missingImages.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val allListStateData by viewModel.allListState.collectAsState()
    val allListState = rememberLazyListState(
        initialFirstVisibleItemIndex = allListStateData.keys.firstOrNull() ?: 0,
        initialFirstVisibleItemScrollOffset = allListStateData.values.firstOrNull() ?: 0
    )

    LaunchedEffect(allListState) {
        snapshotFlow { allListState.firstVisibleItemIndex to allListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                viewModel.saveListState(MealsListViewModel.Tab.ALL, index, offset)
            }
    }

    val missingListStateData by viewModel.missingListState.collectAsState()
    val missingListState = rememberLazyListState(
        initialFirstVisibleItemIndex = missingListStateData.keys.firstOrNull() ?: 0,
        initialFirstVisibleItemScrollOffset = missingListStateData.values.firstOrNull() ?: 0
    )

    LaunchedEffect(missingListState) {
        snapshotFlow { missingListState.firstVisibleItemIndex to missingListState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                viewModel.saveListState(MealsListViewModel.Tab.MISSING_IMAGES, index, offset)
            }
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        topBar = if (!usesNativeNavigation) {
            {
                TopAppBar(
                    title = { Text("Meals") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.backgroundPage,
                        titleContentColor = colors.foregroundDefault,
                        navigationIconContentColor = colors.foregroundDefault,
                        actionIconContentColor = colors.foregroundDefault,
                    )
                )
            }
        } else {
            {}
        }
    ) { inner ->
        val topPadding = getTopPaddingForNativeNavigation()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (usesNativeNavigation) Modifier.padding(top = topPadding) else Modifier.padding(inner))
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = colors.foregroundBrand,
                divider = {}
            ) {
                MealsListViewModel.Tab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setSelectedTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    MealsListViewModel.Tab.ALL -> "All"
                                    MealsListViewModel.Tab.MISSING_IMAGES -> "Missing Images"
                                }
                            )
                        },
                        selectedContentColor = colors.foregroundBrand,
                        unselectedContentColor = colors.foregroundSupport
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                MealsListViewModel.Tab.ALL -> {
                    TextField(
                        value = search,
                        onValueChange = { viewModel.setSearch(it) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.backgroundSurface1,
                            unfocusedContainerColor = colors.backgroundSurface1,
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
                                viewModel.setSearch("")
                                coroutineScope.launch {
                                    allListState.scrollToItem(0)
                                }
                            }, modifier = Modifier.padding(end = 8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        state = allListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(meals, key = { it.id }) { meal ->
                            ListItem(
                                leadingContent = {
                                    if (!meal.image_url.isNullOrBlank()) {
                                        AsyncImage(
                                            model = meal.image_url,
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
                                        index = meals.indexOf(meal),
                                        size = meals.size
                                    )
                                ).clickable { onOpenMeal(meal.id) }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                MealsListViewModel.Tab.MISSING_IMAGES -> {
                    LazyColumn(
                        state = missingListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (missingImages.isEmpty()) {
                            item {
                                Text(
                                    "No meals missing images!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.foregroundSupport
                                )
                            }
                        }
                        items(missingImages) { item ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        item.item_name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        "Tracked ${item.frequency} times",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.foregroundSupport
                                    )
                                },
                                modifier = Modifier.clip(
                                    getRoundedCornerShape(
                                        index = missingImages.indexOf(item),
                                        size = missingImages.size
                                    )
                                ).clickable {
                                    if (item.source_meal_id != null) {
                                        onOpenMeal(item.source_meal_id)
                                    }
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}
