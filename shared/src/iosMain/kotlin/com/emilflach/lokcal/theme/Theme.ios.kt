package com.emilflach.lokcal.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.launch
import platform.UIKit.*

private data class IosLikeIndication(val overlayColor: Color) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        IosIndicationNode(interactionSource, overlayColor)
}

private class IosIndicationNode(
    private val interactionSource: InteractionSource,
    private val overlayColor: Color,
) : Modifier.Node(), DrawModifierNode {
    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> alpha.snapTo(0.1f)
                    is PressInteraction.Release, is PressInteraction.Cancel ->
                        alpha.animateTo(0f, animationSpec = tween(durationMillis = 300))
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = alpha.value
        if (a > 0f) {
            drawRect(color = overlayColor.copy(alpha = a), size = size)
        }
    }
}

@Composable
internal actual fun PlatformRippleOverride(content: @Composable () -> Unit) {
    val overlayColor = LocalRecipesColors.current.foregroundDefault
    val indication = remember(overlayColor) { IosLikeIndication(overlayColor) }
    CompositionLocalProvider(
        LocalRippleConfiguration provides null,
        LocalIndication provides indication
    ) {
        content()
    }
}

@Composable
internal actual fun SystemAppearance(isDark: Boolean) {
    LaunchedEffect(isDark) {
        UIApplication.sharedApplication.setStatusBarStyle(
            if (isDark) UIStatusBarStyleDarkContent else UIStatusBarStyleLightContent
        )
    }
}

@Composable
internal actual fun getSystemIsDarkTheme(): Boolean {
    val keyWindow = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val traitCollection = keyWindow?.traitCollection ?: UIScreen.mainScreen.traitCollection
    return traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
}