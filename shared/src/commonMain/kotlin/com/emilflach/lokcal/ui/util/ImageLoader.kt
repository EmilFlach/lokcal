package com.emilflach.lokcal.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*

@OptIn(ExperimentalCoilApi::class)
@Composable
fun rememberKtorImageLoader(): ImageLoader {
    val platformContext = LocalPlatformContext.current
    return remember(platformContext) {
        val httpClient = HttpClient {
            defaultRequest {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
                header(HttpHeaders.Accept, "*/*")
                header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
                header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A(Brand\";v=\"99\"")
                header("sec-ch-ua-mobile", "?1")
                header("sec-ch-ua-platform", "\"Android\"")
                header("sec-fetch-dest", "empty")
                header("sec-fetch-mode", "cors")
                header("sec-fetch-site", "cross-site")
            }

        }
        ImageLoader.Builder(platformContext)
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .build()
    }
}
