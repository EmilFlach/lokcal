package com.emilflach.lokcal.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.ui.util.EntityImageData
import com.emilflach.lokcal.ui.util.LocalImageCache
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodDialog(
    onDismissRequest: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    results: List<Food>,
    subtitleForFood: (Food) -> String,
    onFoodSelected: (Food) -> Unit
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader(LocalImageCache.current)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100.milliseconds)
        focusRequester.requestFocus()
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .clip(MaterialTheme.shapes.large)
            .background(colors.backgroundPage)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Add food",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.foregroundDefault
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.foregroundDefault)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Search foods") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.foregroundDefault,
                    unfocusedTextColor = colors.foregroundDefault,
                    cursorColor = colors.foregroundDefault,
                    focusedBorderColor = colors.foregroundDefault,
                    unfocusedBorderColor = colors.foregroundSupport
                )
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results) { food ->
                    ListItem(
                        leadingContent = {
                            if (!food.image_url.isNullOrBlank()) {
                                val imageModel = EntityImageData(
                                    EntityImageData.FOOD,
                                    food.id,
                                    food.image_url
                                )
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = null,
                                    imageLoader = imageLoader,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(35.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface2)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(35.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(colors.backgroundSurface2)
                                )
                            }
                        },
                        headlineContent = {
                            Text(food.name, color = colors.foregroundDefault)
                        },
                        supportingContent = {
                            Text(subtitleForFood(food), color = colors.foregroundSupport)
                        },
                        modifier = Modifier
                            .clip(
                                getRoundedCornerShape(
                                    index = results.indexOf(food),
                                    size = results.size
                                )
                            )
                            .clickable { onFoodSelected(food) },
                        colors = ListItemDefaults.colors(
                            containerColor = colors.backgroundSurface1
                        )
                    )
                }
            }
        }
    }
}
