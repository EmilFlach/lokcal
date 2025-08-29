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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.emilflach.lokcal.theme.LocalRecipesColors
import com.emilflach.lokcal.ui.screens.FocusRequesters

@Composable
fun IntakeListItem(
    name: String,
    subtitle: String,
    keyId: Long,
    initialValue: String,
    showBorder: Boolean = false,
    addButtonDescription: String = "Add",
    index: Int,
    size: Int,
    requesters: FocusRequesters,
    gramsById: MutableMap<Long, String>,
    onAddClick: () -> Unit,
    inputField: @Composable (
        tf: TextFieldValue,
        requester: androidx.compose.ui.focus.FocusRequester,
        onValueChange: (TextFieldValue, String) -> Unit,
        onDone: () -> Unit
    ) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = LocalRecipesColors.current

    var tf by rememberSaveable(keyId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = initialValue))
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
                            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(borderWidth, this.size.height)
                        )
                    }
                } else {
                    modifier
                }
            }
            .clickable {
                requesters.request(keyId)
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
                modifier = Modifier.weight(1f)
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
            }

            inputField(
                tf,
                requesters[keyId],
                { newTf, value ->
                    tf = newTf
                    gramsById[keyId] = value
                },
                onAddClick
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
