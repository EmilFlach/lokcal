package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.Food
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.FocusRequesters
import com.emilflach.lokcal.viewmodel.IntakeViewModel

@Composable
fun IntakeListItem(
    item: Food,
    index: Int,
    size: Int,
    viewModel: IntakeViewModel,
    gramsById: MutableMap<Long, String>,
    requesters: FocusRequesters,
    onDone: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val initialGrams = gramsById.getOrPut(item.id) { viewModel.defaultPortionGrams(item).toInt().toString() }
    var tf by rememberSaveable(item.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = gramsById.getOrPut(item.id) { initialGrams }
            )
        )
    }
    val onAddClick = {
        val gramsToAdd = viewModel.parseGrams(gramsById.getOrPut(item.id) { initialGrams })
        if (gramsToAdd > 0.0) {
            viewModel.logPortion(item.id, gramsToAdd)
            onDone()
        }
    }

    val colors = LocalRecipesColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(getRoundedCornerShape(index, size))
            .background(colors.backgroundSurface1)
            .clickable {
                requesters.request(item.id)
                keyboard?.show()
            }
            .padding(vertical = 24.dp, horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(text = item.name, style = MaterialTheme.typography.bodyLarge, color = colors.foregroundDefault)
                Text(
                    text = viewModel.buildSubtitle(
                        food = item,
                        gramsText = gramsById.getOrPut(item.id) { initialGrams },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.foregroundSupport
                )
            }

            GramTextField(
                tf = tf,
                requester = requesters[item.id],
                onValueChange = { newTf, value ->
                    tf = newTf
                    gramsById[item.id] = value
                },
                onDone = { onAddClick() }
            )

            FilledIconButton(modifier = Modifier.size(50.dp), onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add portion"
                )
            }
        }
    }
}

@Composable
fun getRoundedCornerShape(index: Int, size: Int): Shape {
    return when {
        index == 0 && size == 1 -> RoundedCornerShape(4.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )
        index == size - 1 -> RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
            topStart = 4.dp,
            topEnd = 4.dp
        )
        else -> RoundedCornerShape(4.dp)
    }
}
