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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader

@Composable
fun MealTimeItem(
    title: String,
    subtitle: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    isMeal: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    quantityControls: @Composable (requester: FocusRequester) -> Unit,
) {
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader()
    val keyboard = LocalSoftwareKeyboardController.current
    val requester = remember(title, subtitle) { FocusRequester() }

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
                    if (isMeal) onLongPress?.invoke()
                }
            )
            .height(IntrinsicSize.Min)
            .padding(top = 12.dp, bottom = 12.dp, start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.foregroundDefault,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.foregroundSupport,
                modifier = Modifier.padding(end = 8.dp),
            )
            Spacer(Modifier.height(12.dp))

            quantityControls(requester)
        }
    }
}