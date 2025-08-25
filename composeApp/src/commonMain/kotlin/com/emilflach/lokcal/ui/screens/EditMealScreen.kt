package com.emilflach.lokcal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.components.GramTextField
import com.emilflach.lokcal.ui.util.rememberKtorImageLoader
import com.emilflach.lokcal.viewmodel.EditMealViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealScreen(
    viewModel: EditMealViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalRecipesColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit meal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteMeal()
                        onDeleted()
                    }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete meal")
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
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundPage)
                .padding(inner)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                singleLine = true,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.imageUrl,
                onValueChange = viewModel::setImageUrl,
                singleLine = true,
                label = { Text("Image URL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.totalPortions,
                onValueChange = viewModel::setTotalPortionsText,
                singleLine = true,
                label = { Text("Total portions") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Items", style = MaterialTheme.typography.titleMedium, color = colors.foregroundDefault)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(state.items, key = { it.mealItemId }) { it ->
                    MealEditorItemRow(item = it, viewModel = viewModel)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun MealEditorItemRow(
    item: EditMealViewModel.ItemUi,
    viewModel: EditMealViewModel,
) {
    val colors = LocalRecipesColors.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val requester = remember(item.mealItemId) { androidx.compose.ui.focus.FocusRequester() }

    var gramsText by remember(item.mealItemId, item.quantityG) {
        mutableStateOf(item.quantityG.toInt().toString())
    }
    var tf by remember(item.mealItemId, item.quantityG) {
        mutableStateOf(TextFieldValue(text = gramsText))
    }

    val portion = viewModel.defaultPortionGrams(item.food)
    val portionInt = portion.toInt()

    val imageLoader = rememberKtorImageLoader()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(colors.backgroundSurface1)
            .combinedClickable(
                onClick = { requester.requestFocus(); keyboard?.show() },
                onLongClick = {}
            )
            .height(IntrinsicSize.Min)
            .padding(top = 12.dp, bottom = 12.dp, start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val imageUrl = item.food.image_url
        if (!imageUrl.isNullOrBlank()) {
            coil3.compose.AsyncImage(
                model = imageUrl,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.backgroundSurface2)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(item.food.name, style = MaterialTheme.typography.bodyLarge, color = colors.foregroundDefault)
            Spacer(Modifier.height(4.dp))
            val kcal = viewModel.computeKcalFor(item.food, viewModel.parseGrams(gramsText))
            val portionsText = viewModel.getPortionsTextFor(item.food, viewModel.parseGrams(gramsText))
            Text(text = "${kcal.toInt()} kcal • $portionsText", style = MaterialTheme.typography.bodySmall, color = colors.foregroundSupport)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                GramTextField(
                    tf = tf,
                    requester = requester,
                    onValueChange = { newTf, value ->
                        // Only update local state while typing; persist on Done to avoid cursor reset
                        tf = newTf
                        gramsText = value
                    },
                    onDone = {
                        val g = viewModel.parseGrams(gramsText)
                        viewModel.updateItemQuantity(item.mealItemId, g)
                    }
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        val newText = (viewModel.parseGrams(gramsText) + portion).toInt().toString()
                        gramsText = newText
                        tf = tf.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newText.length))
                        viewModel.updateItemQuantity(item.mealItemId, viewModel.parseGrams(newText))
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foregroundSupport),
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add a portion of the food")
                    Spacer(Modifier.width(4.dp))
                    Text(text = "${portionInt}g")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedIconButton(
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = colors.foregroundSupport),
                    onClick = {
                        val newText = (viewModel.parseGrams(gramsText) - portion).coerceAtLeast(0.0).toInt().toString()
                        gramsText = newText
                        tf = tf.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newText.length))
                        viewModel.updateItemQuantity(item.mealItemId, viewModel.parseGrams(newText))
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Subtract a portion of the food")
                }

                IconButton(onClick = { viewModel.deleteItem(item.mealItemId) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete item", tint = colors.foregroundSupport)
                }
            }
        }
    }
}
