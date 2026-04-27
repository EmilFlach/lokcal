package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.data.OnlineFoodItem
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.sources.*
import kotlinx.coroutines.*

class OnlineSearchManager(
    private val settingsRepo: SettingsRepository,
    private val scope: CoroutineScope
) {
    companion object {
        private const val RATE_LIMIT_MESSAGE = "Wait 10 seconds to search online again"
        private const val CANCELLED_MESSAGE = "Search cancelled"
    }

    data class SearchSection(
        val sourceId: String = "",
        val sourceName: String = "",
        val foods: List<Food> = emptyList(),
        val isSearching: Boolean = false,
        val error: String? = null,
        val noResults: Boolean = false,
        val remainingCooldown: Int = 0,
        val progress: Float? = null, // Progress from 0 to 1
    )

    private val sourceRegistry = SourceRegistry().apply {
        register(AlbertHeijnFoodSource())
        register(OpenFoodFactsFoodSource())
        register(EsselungaFoodSource())
        register(KrogerFoodSource())
    }
    private val rateLimiter = RateLimiter()
    private val sourceResults = mutableMapOf<String, MutableMap<Long, OnlineFoodItem>>()
    private val sourceTempIds = mutableMapOf<String, Long>()
    private var onlineSearchJob: Job? = null

    private var _isSearching = false
    val isSearching: Boolean get() = _isSearching

    private var _sections = emptyList<SearchSection>()
    val sections: List<SearchSection> get() = _sections

    val showGlobalNoResults: Boolean
        get() = _sections.isNotEmpty() && _sections.all {
            !it.isSearching && it.error == null && (it.noResults || it.foods.isEmpty())
        }

    fun search(query: String, onStateChanged: () -> Unit) {
        val q = query.trim()
        if (q.isEmpty()) return

        scope.launch {
            val sources = getPreferredSources()
            if (sources.isEmpty()) return@launch

            // Initialize sections
            _sections = sources.map { source ->
                val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
                SearchSection(
                    sourceId = source.id,
                    sourceName = source.displayName,
                    isSearching = cooldown <= 0,
                    remainingCooldown = cooldown,
                    error = if (cooldown > 0) RATE_LIMIT_MESSAGE else null
                )
            }
            _isSearching = _sections.any { it.isSearching }
            onStateChanged()

            // Clear transient maps
            sourceResults.clear()
            sourceTempIds.clear()

            // Cancel any previous job
            onlineSearchJob?.cancel()
            onlineSearchJob = scope.launch {
                try {
                    val jobs = sources.map { source ->
                        launch {
                            val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
                            if (cooldown > 0) {
                                updateSection(source.id, onStateChanged) { it.copy(isSearching = false) }
                                return@launch
                            }
                            searchWithSource(source, q, onStateChanged)
                        }
                    }
                    jobs.joinAll()
                } catch (_: CancellationException) {
                    handleCancellation(onStateChanged)
                } catch (_: Throwable) {
                    _isSearching = false
                    _sections = _sections.map { it.copy(isSearching = false) }
                    onStateChanged()
                } finally {
                    checkSearchFinished(onStateChanged)
                }
            }
        }
    }

    private suspend fun searchWithSource(
        source: FoodSource,
        query: String,
        onStateChanged: () -> Unit
    ) {
        // Check rate limit
        if (!rateLimiter.canRequest(source.id, source.rateLimitSeconds)) {
            val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
            updateSection(source.id, onStateChanged) {
                it.copy(
                    isSearching = false,
                    error = RATE_LIMIT_MESSAGE,
                    remainingCooldown = cooldown
                )
            }
            return
        }

        try {
            rateLimiter.recordRequest(source.id)

            // Initial progress for WEB sources
            if (source.type == SourceType.WEB) {
                updateSection(source.id, onStateChanged) { it.copy(progress = 0.1f) }
            }

            val items = withContext(Dispatchers.Default) { source.search(query) }

            // Process results
            val results = sourceResults.getOrPut(source.id) { mutableMapOf() }
            if (!sourceTempIds.containsKey(source.id)) {
                sourceTempIds[source.id] = (sourceTempIds.size + 1) * -1000L
            }

            val foods = items.map { item ->
                val tempId = sourceTempIds[source.id]!! - 1
                sourceTempIds[source.id] = tempId
                results[tempId] = item
                item.toFood(tempId, source.id)
            }

            updateSection(source.id, onStateChanged) {
                it.copy(
                    foods = foods,
                    isSearching = false,
                    noResults = items.isEmpty(),
                    progress = null
                )
            }
        } catch (e: CancellationException) {
            updateSection(source.id, onStateChanged) {
                it.copy(isSearching = false, error = CANCELLED_MESSAGE, progress = null, noResults = false)
            }
            throw e
        } catch (_: Throwable) {
            updateSection(source.id, onStateChanged) {
                it.copy(
                    isSearching = false,
                    error = "Could not connect to ${source.displayName}",
                    progress = null,
                    noResults = false
                )
            }
        }
    }

    private fun updateSection(
        sourceId: String,
        onStateChanged: () -> Unit,
        transform: (SearchSection) -> SearchSection
    ) {
        val index = _sections.indexOfFirst { it.sourceId == sourceId }
        if (index != -1) {
            val newSections = _sections.toMutableList()
            newSections[index] = transform(newSections[index])
            _sections = newSections
            onStateChanged()
        }
    }

    private fun handleCancellation(onStateChanged: () -> Unit) {
        _isSearching = false
        _sections = _sections.map {
            if (it.isSearching) it.copy(isSearching = false, error = CANCELLED_MESSAGE, progress = null, noResults = false)
            else it.copy(isSearching = false)
        }
        onStateChanged()
    }

    private fun checkSearchFinished(onStateChanged: () -> Unit) {
        if (_sections.all { !it.isSearching }) {
            _isSearching = false
            onStateChanged()
        }
    }

    private suspend fun getPreferredSources(): List<FoodSource> {
        val off = sourceRegistry.getById("off") ?: return emptyList()
        val optional = settingsRepo.getSourcePreferences().filter { it != "none" }
        return if (optional.isNotEmpty()) {
            // Optional source first, Open Food Facts always last
            sourceRegistry.getByIds(optional.filter { it != "off" }) + off
        } else {
            listOf(off)
        }
    }

    fun getSourceItem(foodId: Long): OnlineFoodItem? {
        return sourceResults.values.firstNotNullOfOrNull { it[foodId] }
    }

    fun getSourceId(foodId: Long): String? {
        return sourceResults.entries.firstOrNull { it.value.containsKey(foodId) }?.key
    }

    fun clear() {
        cancel()
        _sections = emptyList()
        sourceResults.clear()
        sourceTempIds.clear()
    }

    fun cancel(onStateChanged: () -> Unit = {}) {
        onlineSearchJob?.cancel()
        onlineSearchJob = null
        handleCancellation(onStateChanged)
    }

    private fun OnlineFoodItem.toFood(tempId: Long, source: String) = Food(
        id = tempId,
        name = name,
        energy_kcal_per_100g = energyKcalPer100g ?: 0.0,
        unit = "g",
        serving_size = servingSize,
        gtin13 = gtin13,
        image_url = imageUrl,
        product_url = productUrl,
        source = source,
        created_at = ""
    )
}
