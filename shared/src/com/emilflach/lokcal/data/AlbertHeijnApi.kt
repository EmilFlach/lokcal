package com.emilflach.lokcal.data

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.time.Clock

/**
 * Thrown when Albert Heijn answers with a non-2xx status. Carrying the status matters: the nightly
 * liveness check uses it to tell "we are being blocked at the edge" (401/403) apart from "the API
 * changed shape" (2xx that no longer parses).
 */
class AlbertHeijnApiException(
    val status: HttpStatusCode,
    val bodySnippet: String,
) : Exception("Albert Heijn API returned $status: ${bodySnippet.take(200)}")

@Serializable
private data class AnonymousTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long = 0L,
)

@Serializable
private data class SearchResponse(
    val products: List<SearchProduct> = emptyList(),
)

@Serializable
private data class SearchProduct(
    val webshopId: Int? = null,
    val title: String? = null,
    // Multipacks ("3-pack", "voordeelverpakking" bundles) carry no tradeItem at all — no GTIN and
    // no nutrition — so they are useless to a calorie tracker and get filtered out.
    val isVirtualBundle: Boolean? = null,
)

/**
 * Transport for the Albert Heijn Appie mobile API (`api.ah.nl`).
 *
 * The public storefront (`www.ah.nl`, including its `/gql` endpoint) sits behind an Akamai WAF that
 * answers non-browser clients with `403 Access Denied`, which is why scraping it stopped working.
 * The mobile API is reachable with an anonymous token and the app's own header set.
 *
 * Product detail is fetched through GraphQL rather than `/mobile-services/product/detail/v4/fir/{id}`
 * for two reasons: aliased queries fetch every product in a single request instead of one request
 * each, and the `tradeItem.nutritions[]` shape it returns is the same one this app has always
 * parsed, so [AlbertHeijnProductParser] needed no new parsing logic.
 */
open class AlbertHeijnApi(
    private val client: HttpClient = defaultClient,
    /**
     * Minimum pause between two outgoing requests. The app leaves this at 0 (a search is only two
     * sequential requests anyway); the nightly liveness check raises it to spread its handful of
     * requests out.
     */
    private val minRequestSpacingMs: Long = 0L,
) {
    companion object {
        internal const val API_BASE = "https://api.ah.nl"
        internal const val WEB_BASE = "https://www.ah.nl"

        internal const val TOKEN_URL = "$API_BASE/mobile-auth/v1/auth/token/anonymous"
        internal const val SEARCH_URL = "$API_BASE/mobile-services/product/search/v2"
        internal const val GRAPHQL_URL = "$API_BASE/graphql"

        // Identify as the iOS Appie client. Ktor's default "ktor-client" User-Agent is rejected.
        internal const val USER_AGENT = "Appie/9.28 (iPhone17,3; iPhone; CPU OS 26_1 like Mac OS X)"
        internal const val CLIENT_ID = "appie-ios"
        internal const val CLIENT_VERSION = "9.28"
        internal const val X_APPLICATION = "AHWEBSHOP"

        /** Fields requested per product. Mirrors what [AlbertHeijnProductParser] reads. */
        private const val PRODUCT_FIELDS =
            "id title webPath imagePack { small { url } } " +
                "tradeItem { gtin nutritions { basisQuantity nutrients { type value } } }"

        private val json = Json { ignoreUnknownKeys = true }

        private val defaultClient by lazy {
            HttpClient {
                install(Logging) { level = LogLevel.INFO }
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 15000
                    socketTimeoutMillis = 15000
                }
                install(DefaultRequest) {
                    header(HttpHeaders.UserAgent, USER_AGENT)
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                    header("x-application", X_APPLICATION)
                    header("x-client-name", CLIENT_ID)
                    header("x-client-version", CLIENT_VERSION)
                }
            }
        }
    }

    /** A single relevance-ordered search result, before product detail is fetched. */
    data class SearchHit(val webshopId: Int, val title: String)

    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L // epoch milliseconds
    private var lastRequestAt: Long = 0L // epoch milliseconds

    /**
     * Searches products by relevance, dropping virtual bundles. Ask for more than you need: a
     * search for a staple like "melk" can come back majority-bundles.
     */
    open suspend fun searchProducts(query: String, size: Int = 20): List<SearchHit> {
        if (query.isBlank()) return emptyList()
        val response = json.decodeFromString<SearchResponse>(withToken { fetchSearchJson(query, size, it) })
        return response.products.mapNotNull { product ->
            val id = product.webshopId ?: return@mapNotNull null
            val title = product.title ?: return@mapNotNull null
            if (product.isVirtualBundle == true) null else SearchHit(id, title)
        }
    }

    /**
     * Fetches product detail for [ids] in a single aliased GraphQL request. Ids with no product in
     * the response are simply absent from the result.
     */
    open suspend fun fetchProducts(ids: List<Int>): Map<Int, JsonObject> {
        if (ids.isEmpty()) return emptyMap()

        val aliases = ids.mapIndexed { index, id -> "p$index: product(id: $id) { $PRODUCT_FIELDS }" }
        val body = buildJsonObject {
            put("operationName", "FetchProducts")
            put("query", "query FetchProducts { ${aliases.joinToString(" ")} }")
        }.toString()

        val payload = json.parseToJsonElement(withToken { fetchGraphQlJson(body, it) }).jsonObject
        val data = payload["data"] as? JsonObject
        if (data == null) {
            val errors = payload["errors"]?.toString().orEmpty()
            error("Albert Heijn GraphQL returned no data: ${errors.take(200)}")
        }

        return ids.withIndex().mapNotNull { (index, id) ->
            (data["p$index"] as? JsonObject)?.let { id to it }
        }.toMap()
    }

    /** Fetches a single product, or null when Albert Heijn does not know the id. */
    suspend fun fetchProduct(id: Int): JsonObject? = fetchProducts(listOf(id))[id]

    /**
     * Returns a cached anonymous access token, fetching a new one when it is missing or within a
     * minute of expiry. Tokens are valid for about a week, so a running app fetches one.
     */
    suspend fun accessToken(): String {
        val cached = cachedToken
        if (cached != null && Clock.System.now().toEpochMilliseconds() < tokenExpiresAt - 60_000L) {
            return cached
        }
        val token = json.decodeFromString<AnonymousTokenResponse>(fetchTokenJson())
        cachedToken = token.accessToken
        tokenExpiresAt = Clock.System.now().toEpochMilliseconds() + token.expiresIn * 1000L
        return token.accessToken
    }

    protected open suspend fun fetchTokenJson(): String = request {
        client.post(TOKEN_URL) {
            contentType(ContentType.Application.Json)
            setBody("""{"clientId":"$CLIENT_ID"}""")
        }
    }

    protected open suspend fun fetchSearchJson(query: String, size: Int, token: String): String = request {
        client.get(SEARCH_URL) {
            parameter("query", query)
            parameter("sortOn", "RELEVANCE")
            parameter("size", size)
            parameter("page", 0)
            bearerAuth(token)
        }
    }

    protected open suspend fun fetchGraphQlJson(body: String, token: String): String = request {
        client.post(GRAPHQL_URL) {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(body)
        }
    }

    /**
     * Runs [call] with a valid token. A 401 means the token expired early or was invalidated, so
     * the token is dropped and the call retried exactly once — never more, to avoid hammering.
     */
    private suspend fun <T> withToken(call: suspend (String) -> T): T {
        return try {
            call(accessToken())
        } catch (e: AlbertHeijnApiException) {
            if (e.status != HttpStatusCode.Unauthorized) throw e
            cachedToken = null
            tokenExpiresAt = 0L
            call(accessToken())
        }
    }

    /** Applies request spacing, then turns a non-2xx response into [AlbertHeijnApiException]. */
    private suspend fun request(block: suspend () -> HttpResponse): String {
        if (minRequestSpacingMs > 0L) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - lastRequestAt
            if (lastRequestAt > 0L && elapsed < minRequestSpacingMs) delay(minRequestSpacingMs - elapsed)
        }
        val response = block()
        lastRequestAt = Clock.System.now().toEpochMilliseconds()

        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw AlbertHeijnApiException(response.status, text)
        return text
    }
}
