package com.emilflach.lokcal.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.emilflach.lokcal.theme.LocalRecipesColors
import lokcal.shared.generated.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

@Composable
fun GradientBackground(percentageLeft: Float) {
    val colors = LocalRecipesColors.current
    val isDarkTheme = colors.isDark

    val targetMiddleColor = if (percentageLeft >= 0)
        colors.backgroundSurface2
    else
        if (isDarkTheme) colors.backgroundDangerSubtle else colors.backgroundDanger.copy(alpha = 0.3f)

    var noiseBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        noiseBitmap = Res.readBytes("drawable/noise.png").decodeToImageBitmap()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .drawBehind {
            val transitionPosition = (size.height * 1.5f * percentageLeft.coerceIn(0.15f, 0.95f))
            val circleRadius = size.width * 1.5f
            val circleX = size.width / 2

            drawRect(
                color = colors.backgroundPage,
                size = size
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to colors.backgroundSurface1.copy(0.5f),
                        percentageLeft.coerceIn(0f, 0.85f) * 1.5f to colors.backgroundSurface1.copy(0.5f),
                        1.0f to colors.backgroundSurface2
                    )
                ),
                size = size
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        targetMiddleColor,
                        Color.Transparent,
                    ),
                    center = Offset(circleX, transitionPosition),
                    radius = circleRadius
                ),
                center = Offset(circleX, transitionPosition),
                radius = circleRadius
            )
            noiseBitmap?.let { bitmap ->
                val bitmapWidth = bitmap.width.toFloat()
                val bitmapHeight = bitmap.height.toFloat()

                for (x in 0..size.width.toInt() step bitmapWidth.toInt()) {
                    for (y in 0..size.height.toInt() step bitmapHeight.toInt()) {
                        drawImage(
                            image = bitmap,
                            topLeft = Offset(x.toFloat(), y.toFloat()),
                            alpha = if (isDarkTheme) 0.15f else 0.01f,
                            blendMode = BlendMode.Multiply
                        )
                    }
                }
            }
        }
    )
}


