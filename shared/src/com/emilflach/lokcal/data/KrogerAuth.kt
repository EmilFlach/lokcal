package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.time.Clock

@Serializable
private data class KrogerTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
)

/**
 * Manages Kroger OAuth2 client-credentials access tokens.
 * Tokens are cached in memory and re-fetched when within 60 seconds of expiry.
 */
open class KrogerAuth(
    private val client: HttpClient = defaultClient
) {
    companion object {
        internal const val TOKEN_URL = "https://api.kroger.com/v1/connect/oauth2/token"
        internal const val SCOPE = "product.compact"

        private val json = Json { ignoreUnknownKeys = true }

        private val defaultClient by lazy {
            HttpClient {
                install(Logging) { level = LogLevel.INFO }
            }
        }
    }

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L  // epoch milliseconds

    suspend fun getAccessToken(): String {
        val now = Clock.System.now().toEpochMilliseconds()
        val token = cachedToken
        if (token != null && now < tokenExpiresAt - 60_000L) {
            return token
        }
        return refreshToken()
    }

    private suspend fun refreshToken(): String {
        val responseJson = fetchTokenJson()
        val response = json.decodeFromString<KrogerTokenResponse>(responseJson)
        cachedToken = response.accessToken
        tokenExpiresAt = Clock.System.now().toEpochMilliseconds() + response.expiresIn * 1000L
        return response.accessToken
    }

    protected open suspend fun fetchTokenJson(): String {
        val credentials = Base64.encode("${KrogerConfig.CLIENT_ID}:${KrogerConfig.CLIENT_SECRET}".encodeToByteArray())
        return client.post(TOKEN_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            header(HttpHeaders.Authorization, "Basic $credentials")
            setBody("grant_type=client_credentials&scope=$SCOPE")
        }.body()
    }
}
