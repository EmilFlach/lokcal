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
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.getRoundedCornerShape
import com.emilflach.lokcal.ui.util.EntityImageData
import com.emilflach.lokcal.ui.util.LocalImageCache
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class StealImageItem(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val type: String // "FOOD" or "MEAL"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StealImageDialog(
    onDismissRequest: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    results: List<StealImageItem>,
    onItemSelected: (StealImageItem) -> Unit
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
                    "Steal Image URL",
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
                placeholder = { Text("Search foods or meals") },
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
                items(results) { item ->
                    ListItem(
                        leadingContent = {
                            if (!item.imageUrl.isNullOrBlank()) {
                                val imageModel = EntityImageData(
                                    if (item.type == "FOOD") EntityImageData.FOOD else EntityImageData.MEAL,
                                    item.id,
                                    item.imageUrl
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
                            Text(item.name, color = colors.foregroundDefault)
                        },
                        supportingContent = {
                            Text(item.type, color = colors.foregroundSupport)
                        },
                        modifier = Modifier
                            .clip(
                                getRoundedCornerShape(
                                    index = results.indexOf(item),
                                    size = results.size
                                )
                            )
                            .clickable { onItemSelected(item) },
                        colors = ListItemDefaults.colors(
                            containerColor = colors.backgroundSurface1
                        )
                    )
                }
            }
        }
    }
}
