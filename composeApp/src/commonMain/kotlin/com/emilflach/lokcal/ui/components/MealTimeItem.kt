package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.Intake
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.viewmodel.MealTimeViewModel

@Composable
fun MealTimeItem(
    entry: Intake,
    viewModel: MealTimeViewModel,
    modifier: Modifier = Modifier,
    onLongPress: ((Intake) -> Unit)? = null,
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader()
    val keyboard = LocalSoftwareKeyboardController.current
    val requester = remember(entry.id) { FocusRequester() }
    var portionsLabel by remember(entry.id, entry.quantity_g) {
        mutableStateOf(viewModel.getPortionsLabel(entry))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.backgroundSurface1)
            .combinedClickable(
                onClick = {
                    requester.requestFocus(); keyboard?.show()
                },
                onLongClick = {
                    if (entry.source_meal_id != null) onLongPress?.invoke(entry)
                }
            )
            .height(IntrinsicSize.Min)
            .padding(top = 12.dp, bottom = 12.dp, start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = remember(entry.source_food_id) {
            entry.source_food_id?.let { viewModel.imageUrlForFoodId(it) }
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.backgroundSurface2)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.item_name,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.foregroundDefault,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${entry.energy_kcal_total.toInt()} kcal • $portionsLabel",
                style = MaterialTheme.typography.bodySmall,
                color = colors.foregroundSupport,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(12.dp))

            val isMeal = entry.source_meal_id != null
            if(isMeal) {
                MealQuantityControls(
                    requester = requester,
                    viewModel = viewModel,
                    entry = entry,
                )
            } else {
                FoodQuantityControls(
                    requester = requester,
                    viewModel = viewModel,
                    entry = entry,
                )
            }
        }
    }
}