package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.Meal
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.FocusRequesters
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Composable
fun FoodIntakeListItem(
    food: Food,
    viewModel: IntakeViewModel,
    index: Int,
    size: Int,
    requesters: FocusRequesters,
    onDone: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val state by viewModel.state.collectAsState()
    val initialGrams = state.gramsById[food.id] ?: viewModel.defaultPortionGrams(food).toInt().toString()

    IntakeListItem(
        name = food.name,
        subtitle = viewModel.subtitleForFood(food, initialGrams),
        keyId = food.id,
        imageUrl = food.image_url,
        initialValue = initialGrams,
        index = index,
        size = size,
        requesters = requesters,
        onValueChange = { viewModel.setGrams(food.id, it) },
        onAddClick = {
            viewModel.addFoodByGrams(food.id, initialGrams) { onDone() }
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
        },
        onAddByKeyboard = {
            viewModel.addFoodByGrams(food.id, initialGrams) { onDone() }
        },
        onLongPress = {
            food.product_url?.let { uriHandler.openUri(it) }
        },
        inputField = { tf, requester, onValueChange, onDone ->
            GramTextField(tf, requester, onValueChange, onDone)
        }
    )
}

@Composable
fun MealIntakeListItem(
    meal: Meal,
    viewModel: IntakeViewModel,
    index: Int,
    size: Int,
    requesters: FocusRequesters,
    onDone: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    val keyId = -meal.id
    val state by viewModel.state.collectAsState()
    val initialPortions = state.gramsById[keyId] ?: "1"

    IntakeListItem(
        name = meal.name,
        subtitle = viewModel.subtitleForMeal(meal, initialPortions),
        keyId = keyId,
        initialValue = initialPortions,
        showBorder = true,
        index = index,
        size = size,
        requesters = requesters,
        onValueChange = { viewModel.setGrams(keyId, it) },
        onAddClick = {
            viewModel.addMealByPortions(meal.id, initialPortions) { onDone() }
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
        },
        onAddByKeyboard = {
            viewModel.addMealByPortions(meal.id, initialPortions) { onDone() }
        },
        inputField = { tf, requester, onValueChange, onDone ->
            PortionsTextField(tf, requester, onValueChange, onDone)
        }
    )
}

@Composable
fun IntakeListItem(
    name: String,
    subtitle: String,
    imageUrl: String? = null,
    keyId: Long,
    initialValue: String,
    showBorder: Boolean = false,
    addButtonDescription: String = "Add",
    index: Int,
    size: Int,
    requesters: FocusRequesters,
    onValueChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onAddByKeyboard: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    inputField: @Composable (
        tf: TextFieldValue,
        requester: FocusRequester,
        onValueChange: (TextFieldValue, String) -> Unit,
        onDone: () -> Unit
    ) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = LocalRecipesColors.current
    val imageLoader = rememberKtorImageLoader()

    var tf by remember(keyId) {
        mutableStateOf(TextFieldValue(text = initialValue))
    }

    // Update tf when initialValue changes from outside (e.g. from ViewModel)
    // but only if it's not currently being edited to avoid jumping cursor?
    // Actually, since we want the ViewModel to be the source of truth:
    LaunchedEffect(initialValue) {
        if (tf.text != initialValue) {
            tf = tf.copy(text = initialValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(getRoundedCornerShape(index, size))
            .background(colors.backgroundSurface1)
            .let { modifier ->
                if (showBorder) {
                    modifier.drawBehind {
                        val borderWidth = 2.dp.toPx()
                        drawRect(
                            color = colors.backgroundBrand,
                            topLeft = Offset(0f, 0f),
                            size = Size(borderWidth, this.size.height)
                        )
                    }
                } else {
                    modifier
                }
            }
            .combinedClickable(
                onClick = {
                    requesters.request(keyId)
                    keyboard?.show()
                },
                onLongClick = onLongPress
            )
            .padding(vertical = 16.dp, horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .height(40.dp)
                        .width(35.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.backgroundSurface2)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.foregroundDefault
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foregroundSupport
                )
                Spacer(Modifier.height(4.dp))
            }

            inputField(
                tf,
                requesters[keyId],
                { newTf, value ->
                    tf = newTf
                    onValueChange(value)
                },
                onAddByKeyboard
            )

            FilledIconButton(
                modifier = Modifier.size(50.dp),
                onClick = onAddClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = addButtonDescription
                )
            }
        }
    }
}

@Composable
fun getRoundedCornerShape(index: Int, size: Int): Shape { 
    return when (index) {
        0 if size == 1 -> RoundedCornerShape(16.dp)
        0 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )
        size - 1 -> RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
            topStart = 4.dp,
            topEnd = 4.dp
        )
        else -> RoundedCornerShape(4.dp)
    }
}
