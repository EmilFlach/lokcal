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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.health.HealthManager
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.MealTopBar
import com.emilflach.lokcal.ui.components.PlatformScaffold
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.ui.util.EntityImageData
import com.emilflach.lokcal.ui.util.LocalImageCache
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.viewmodel.ExerciseManageViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ExerciseManageScreen(
    viewModel: ExerciseManageViewModel,
    onBack: () -> Unit,
    onOpenEdit: (Long?) -> Unit,
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader(LocalImageCache.current)
    val types by viewModel.types.collectAsState()
    val healthGranted by HealthManager.permissionsGranted.collectAsState()
    val displayedTypes = if (healthGranted) types
        else types.filter { it.name != ExerciseRepository.AUTOMATIC_STEPS_KEY }
    val listState = rememberLazyListState()

    BackHandler { onBack() }

    PlatformScaffold(
        topBar = {
            MealTopBar(
                title = "Exercises",
                onBack = onBack,
                showSearch = false,
                trailingActions = {
                    IconButton(onClick = { onOpenEdit(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add exercise type")
                    }
                }
            )
        },
        scrollState = listState,
        navBarBackgroundColor = MaterialTheme.colorScheme.background
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = inner.listContentPadding(),
        ) {
            items(displayedTypes, key = { it.id }) { type ->
                ListItem(
                    leadingContent = {
                        if (!type.image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = EntityImageData(EntityImageData.EXERCISE_TYPE, type.id, type.image_url),
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
                    headlineContent = { Text(ExerciseRepository.displayName(type.name)) },
                    supportingContent = { Text("${type.kcal_per_hour.toInt()} kcal/hour") },
                    modifier = Modifier
                        .clip(getRoundedCornerShape(displayedTypes.indexOf(type), displayedTypes.size))
                        .clickable { onOpenEdit(type.id) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
