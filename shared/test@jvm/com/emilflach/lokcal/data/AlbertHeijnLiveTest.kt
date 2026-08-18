package com.emilflach.lokcal.data

import com.emilflach.lokcal.data.sources.AlbertHeijnFoodSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Manual liveness check for the Albert Heijn integration.
 *
 * Albert Heijn has no public API, so this app rides on the endpoints the Appie mobile app uses, and
 * those change without notice. Run this by hand when Albert Heijn search looks wrong:
 *
 *     LOKCAL_AH_LIVE=1 ./kotlin test -m shared -p jvm \
 *       --include-classes 'com.emilflach.lokcal.data.AlbertHeijnLiveTest'
 *
 * It is the only test that touches the network, and it is off unless `LOKCAL_AH_LIVE=1` is set, so
 * `./kotlin test` stays hermetic and offline.
 *
 * It drives the real [AlbertHeijnFoodSource] rather than probing endpoints directly, so it catches
 * "Albert Heijn changed something" and "we broke our own client" alike. One run makes three requests
 * (token, search, batched detail), spaced out, and retries at most once — only when Albert Heijn
 * itself errored. A block or a throttle is never retried.
 */
class AlbertHeijnLiveTest {

    private companion object {
        const val ENV_ENABLED = "LOKCAL_AH_LIVE"

        const val QUERY = "melk"
        const val MIN_RESULTS = 3
        const val REQUEST_SPACING_MS = 1_500L
        const val RETRY_BACKOFF_MS = 15_000L

        const val PRODUCT_URL_PREFIX = "https://www.ah.nl/producten/product/"
        val GTIN13 = Regex("""\d{13}""")
    }

    /**
     * - [OK]: Albert Heijn answered and the results are usable.
     * - [BLOCKED]: we were refused at the edge (WAF, or throttled). Inconclusive about the contract.
     * - [UPSTREAM_ERROR]: Albert Heijn errored or did not answer. Transient often enough to retry once.
     * - [CONTRACT_CHANGED]: we got a response, but it no longer contains what the app needs.
     */
    private enum class Status { OK, BLOCKED, UPSTREAM_ERROR, CONTRACT_CHANGED }

    private data class Verdict(val status: Status, val detail: String)

    @Test
    fun albertHeijnSearchStillReturnsUsableResults() {
        if (System.getenv(ENV_ENABLED) != "1") {
            println("[ah-live-check] skipped — set $ENV_ENABLED=1 to run the live Albert Heijn check")
            return
        }

        // Real time, not virtual: request spacing and the retry backoff must actually elapse, which
        // runTest's test scheduler would skip.
        val verdict = runBlocking { probe(retryAllowed = true) }

        println("[ah-live-check] ${verdict.status}: ${verdict.detail}")
        assertEquals(Status.OK, verdict.status, verdict.detail)
    }

    private suspend fun probe(retryAllowed: Boolean): Verdict {
        val source = AlbertHeijnFoodSource(AlbertHeijnApi(minRequestSpacingMs = REQUEST_SPACING_MS))

        val items = try {
            source.search(QUERY)
        } catch (e: Throwable) {
            val status = classify(e)
            if (status == Status.UPSTREAM_ERROR && retryAllowed) {
                println("[ah-live-check] ${describe(e)} — backing off ${RETRY_BACKOFF_MS / 1000}s for one retry")
                delay(RETRY_BACKOFF_MS)
                return probe(retryAllowed = false)
            }
            return Verdict(status, describe(e))
        }

        return evaluate(items)
    }

    private fun evaluate(items: List<OnlineFoodItem>): Verdict {
        fun changed(detail: String) = Verdict(Status.CONTRACT_CHANGED, detail)

        if (items.size < MIN_RESULTS) {
            return changed("searched \"$QUERY\" and got ${items.size} results, expected at least $MIN_RESULTS")
        }
        if (items.any { it.name.isBlank() }) {
            return changed("a result came back with a blank name")
        }
        items.firstOrNull { it.productUrl?.startsWith(PRODUCT_URL_PREFIX) != true }?.let {
            return changed("result \"${it.name}\" has an unexpected productUrl: ${it.productUrl}")
        }
        if (items.none { (it.energyKcalPer100g ?: 0.0) > 0.0 }) {
            return changed("no result carried an energy value — nutrition is missing from the response")
        }
        if (items.none { it.gtin13?.let(GTIN13::matches) == true }) {
            return changed("no result carried a 13-digit GTIN")
        }
        if (items.none { it.imageUrl != null }) {
            return changed("no result carried an image URL")
        }

        val first = items.first()
        return Verdict(
            Status.OK,
            "searched \"$QUERY\" and got ${items.size} usable results — " +
                "first: ${first.name} (${first.energyKcalPer100g} kcal/100g, gtin ${first.gtin13})",
        )
    }

    private fun classify(e: Throwable): Status = when {
        e is AlbertHeijnApiException -> when {
            looksLikeAWall(e.bodySnippet) -> Status.BLOCKED
            e.status.value == 401 || e.status.value == 403 || e.status.value == 429 -> Status.BLOCKED
            e.status.value >= 500 -> Status.UPSTREAM_ERROR
            else -> Status.CONTRACT_CHANGED
        }
        // A 200 carrying an edge block page rather than JSON surfaces as a parse failure.
        looksLikeAWall(e.message.orEmpty()) -> Status.BLOCKED
        e is IOException -> Status.UPSTREAM_ERROR
        e::class.simpleName?.contains("Timeout") == true -> Status.UPSTREAM_ERROR
        else -> Status.CONTRACT_CHANGED
    }

    private fun looksLikeAWall(text: String): Boolean {
        val haystack = text.lowercase()
        return "access denied" in haystack || "<html" in haystack || "just a moment" in haystack
    }

    private fun describe(e: Throwable): String = "${e::class.simpleName}: ${e.message?.take(300)}"
}
