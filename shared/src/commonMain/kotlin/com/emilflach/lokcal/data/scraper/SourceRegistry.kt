package com.emilflach.lokcal.data.scraper

/**
 * Central registry for all available food sources.
 * Manages source discovery, filtering, and URL routing.
 */
class SourceRegistry {
    private val sources = mutableListOf<FoodSource>()

    /**
     * Register a new food source.
     */
    fun register(source: FoodSource) {
        sources.add(source)
    }

    /**
     * Get all registered sources.
     */
    fun getAll(): List<FoodSource> = sources.toList()

    /**
     * Get a source by its ID.
     */
    fun getById(id: String): FoodSource? {
        return sources.find { it.id == id }
    }

    /**
     * Get sources by multiple IDs, preserving order.
     */
    fun getByIds(ids: List<String>): List<FoodSource> {
        return ids.mapNotNull { getById(it) }
    }

    /**
     * Find the appropriate source for a given URL.
     */
    suspend fun findForUrl(url: String): FoodSource? {
        return sources.find { it.canHandle(url) }
    }
}
