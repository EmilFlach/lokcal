package com.emilflach.lokcal.data.scraper

import com.emilflach.lokcal.data.OnlineFoodItem

/**
 * Common interface for all food data sources (scrapers and APIs).
 */
interface FoodSource {
    val id: String
    val displayName: String
    val description: String
    val country: String  // ISO 3166-1 alpha-2 code (e.g., "NL", "BE") or "*" for global
    val type: SourceType
    val rateLimitSeconds: Int  // Minimum seconds between requests

    suspend fun search(query: String): List<OnlineFoodItem>
    suspend fun scrapeUrl(url: String): OnlineFoodItem?
}

enum class SourceType {
    API,      // REST API (e.g., OpenFoodFacts)
    SCRAPER   // HTML scraping (e.g., Albert Heijn)
}
