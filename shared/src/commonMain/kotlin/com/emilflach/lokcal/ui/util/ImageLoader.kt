package com.emilflach.lokcal.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.Options
import com.emilflach.lokcal.data.ImageCacheRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import okio.Buffer

val LocalImageCache = compositionLocalOf<ImageCacheRepository> {
    error("No ImageCacheRepository provided")
}

/**
 * Stable model type for entity images (foods and meals). Uses [entityType] + [entityId] as the
 * Coil cache key so images survive URL changes. Falls back to [fallbackUrl] for initial fetch.
 */
data class EntityImageData(
    val entityType: String,
    val entityId: Long,
    val fallbackUrl: String,
) {
    companion object {
        const val FOOD = "FOOD"
        const val MEAL = "MEAL"
        const val EXERCISE_TYPE = "EXERCISE_TYPE"
    }
}

private class EntityImageKeyer : Keyer<EntityImageData> {
    override fun key(data: EntityImageData, options: Options): String =
        "${data.entityType.lowercase()}:${data.entityId}"
}

private fun String.toWsrvUrl(): String {
    if (startsWith("https://wsrv.nl/") || startsWith("http://wsrv.nl/")) return this
    val encoded = encodeURLParameter()
    return "https://wsrv.nl/?url=$encoded&w=200&h=200&fit=cover&output=jpg&q=75"
}

private class DatabaseImageFetcher(
    private val data: EntityImageData,
    private val options: Options,
    private val imageCache: ImageCacheRepository,
    private val httpClient: HttpClient,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val isPersistent = data.entityId > 0
        val url = data.fallbackUrl.takeIf { it.isNotBlank() }

        // SQLite cache (persistent entities only)
        if (isPersistent) {
            val cached = imageCache.getImage(data.entityType, data.entityId)
            if (cached != null) {
                val urlChanged = url != null && (cached.sourceUrl == null || cached.sourceUrl != url)
                if (urlChanged) {
                    imageCache.deleteImage(data.entityType, data.entityId)
                } else {
                    return SourceFetchResult(
                        source = ImageSource(Buffer().apply { write(cached.bytes) }, options.fileSystem),
                        mimeType = cached.mimeType,
                        dataSource = DataSource.DISK,
                    )
                }
            }
        }

        // Network fetch via wsrv.nl proxy
        if (url == null) return null
        val response = try {
            httpClient.get(url.toWsrvUrl())
        } catch (_: Exception) {
            return null
        }
        if (!response.status.isSuccess()) return null

        val bytes = try {
            response.body<ByteArray>()
        } catch (_: Exception) {
            return null
        }
        if (isPersistent) {
            imageCache.saveImage(data.entityType, data.entityId, bytes, "image/jpeg", url)
        }

        return SourceFetchResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.fileSystem),
            mimeType = "image/jpeg",
            dataSource = DataSource.NETWORK,
        )
    }

    class Factory(
        private val imageCache: ImageCacheRepository,
        private val httpClient: HttpClient,
    ) : Fetcher.Factory<EntityImageData> {
        override fun create(data: EntityImageData, options: Options, imageLoader: ImageLoader): Fetcher =
            DatabaseImageFetcher(data, options, imageCache, httpClient)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun rememberKtorImageLoader(imageCache: ImageCacheRepository): ImageLoader {
    val platformContext = LocalPlatformContext.current
    return remember(platformContext, imageCache) {
        val httpClient = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }
        ImageLoader.Builder(platformContext)
            .diskCachePolicy(CachePolicy.DISABLED)
            .components {
                add(EntityImageKeyer())
                add(DatabaseImageFetcher.Factory(imageCache, httpClient))
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }
}
