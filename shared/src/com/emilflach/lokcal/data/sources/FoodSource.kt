package com.emilflach.lokcal.data.sources

import com.emilflach.lokcal.data.OnlineFoodItem

/**
 * Common interface for all food data sources (APIs or web fetching).
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
    WEB       // Web-based source (e.g., Albert Heijn)
}
