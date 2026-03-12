package com.emilflach.lokcal.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory

@Composable
fun rememberKtorImageLoader(): ImageLoader {
    val platformContext = LocalPlatformContext.current
    return remember(platformContext) {
        ImageLoader.Builder(platformContext)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }
}
