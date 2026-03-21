package com.emilflach.lokcal.data.scraper

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Rate limiter to prevent overwhelming external servers.
 * Enforces a minimum delay between consecutive requests per scraper.
 */
class RateLimiter {
    private val lastRequestTime = mutableMapOf<String, Instant>()

    /**
     * Check if a request can be made for the given scraper.
     * Returns true if enough time has passed since the last request.
     */
    fun canRequest(scraperId: String, rateLimitSeconds: Int): Boolean {
        val now = Clock.System.now()
        val lastRequest = lastRequestTime[scraperId] ?: return true
        val elapsed = now - lastRequest
        return elapsed >= rateLimitSeconds.seconds
    }

    /**
     * Get remaining cooldown time in seconds.
     * Returns 0 if no cooldown is active.
     */
    fun getRemainingCooldown(scraperId: String, rateLimitSeconds: Int): Int {
        val now = Clock.System.now()
        val lastRequest = lastRequestTime[scraperId] ?: return 0
        val elapsed = now - lastRequest
        val remaining = rateLimitSeconds.seconds - elapsed
        return if (remaining.isPositive()) remaining.inWholeSeconds.toInt() else 0
    }

    /**
     * Record that a request was made for the given scraper.
     */
    fun recordRequest(scraperId: String) {
        lastRequestTime[scraperId] = Clock.System.now()
    }

    /**
     * Reset the rate limiter for a specific scraper.
     */
    fun reset(scraperId: String) {
        lastRequestTime.remove(scraperId)
    }
}
