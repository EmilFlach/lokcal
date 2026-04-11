package com.emilflach.lokcal.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
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
import io.ktor.http.*
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
    }
}

private class EntityImageKeyer : Keyer<EntityImageData> {
    override fun key(data: EntityImageData, options: Options): String =
        "${data.entityType.lowercase()}:${data.entityId}"
}

/**
 * Four-tier fetch: SQLite BLOB → Coil disk cache (migration bridge) → network URL → save to SQLite.
 *
 * The Coil disk cache bridge (L2.5) ensures that images downloaded by the old URL-keyed loader
 * are not abandoned when an existing user upgrades. On first access after migration, the bytes
 * are read from Coil's disk cache and saved permanently to SQLite.
 */
private class DatabaseImageFetcher(
    private val data: EntityImageData,
    private val options: Options,
    private val imageCache: ImageCacheRepository,
    private val httpClient: HttpClient,
    private val coilDiskCache: DiskCache?,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val isPersistent = data.entityId > 0

        // L2: check SQLite cache first (only for real, persisted entities)
        if (isPersistent) {
            imageCache.getImage(data.entityType, data.entityId)?.let { (bytes, mime) ->
                return SourceFetchResult(
                    source = ImageSource(Buffer().apply { write(bytes) }, options.fileSystem),
                    mimeType = mime,
                    dataSource = DataSource.DISK,
                )
            }
        }

        val url = data.fallbackUrl.takeIf { it.isNotBlank() }

        // L2.5: migration bridge — check Coil's existing disk cache (only for persistent entities)
        // Coil keys disk cache entries by the URL string for URL-based requests.
        // Images cached before this migration live here; without this tier they would be abandoned.
        if (isPersistent && url != null && coilDiskCache != null) {
            try {
                coilDiskCache.openSnapshot(url)?.use { snapshot ->
                    val bytes = coilDiskCache.fileSystem.read(snapshot.data) { readByteArray() }
                    if (bytes.isNotEmpty()) {
                        imageCache.saveImage(data.entityType, data.entityId, bytes, "image/jpeg")
                        coilDiskCache.remove(url) // clean up old URL-keyed entry after migrating
                        return SourceFetchResult(
                            source = ImageSource(Buffer().apply { write(bytes) }, options.fileSystem),
                            mimeType = "image/jpeg",
                            dataSource = DataSource.DISK,
                        )
                    }
                }
            } catch (_: Exception) {
                // Disk cache read failure is non-fatal; fall through to network
            }
        }

        // L3: fetch from network
        if (url == null) return null
        val response = try {
            httpClient.get(url)
        } catch (_: Exception) {
            return null
        }
        if (!response.status.isSuccess()) return null

        val bytes = try {
            response.body<ByteArray>()
        } catch (_: Exception) {
            return null
        }
        val mime = response.contentType()?.toString() ?: "image/jpeg"

        // Only persist to SQLite for real entities — transient search results use temp negative IDs
        if (isPersistent) {
            imageCache.saveImage(data.entityType, data.entityId, bytes, mime)
        }

        return SourceFetchResult(
            source = ImageSource(Buffer().apply { write(bytes) }, options.fileSystem),
            mimeType = mime,
            dataSource = DataSource.NETWORK,
        )
    }

    class Factory(
        private val imageCache: ImageCacheRepository,
        private val httpClient: HttpClient,
    ) : Fetcher.Factory<EntityImageData> {
        override fun create(data: EntityImageData, options: Options, imageLoader: ImageLoader): Fetcher =
            // imageLoader is fully built here — diskCache is accessible without circular dependency
            DatabaseImageFetcher(data, options, imageCache, httpClient, imageLoader.diskCache)
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
            .diskCachePolicy(CachePolicy.READ_ONLY) // SQLite is the write cache; prevent Coil from duplicating data on disk
            .components {
                add(EntityImageKeyer())
                add(DatabaseImageFetcher.Factory(imageCache, httpClient))
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }
}
