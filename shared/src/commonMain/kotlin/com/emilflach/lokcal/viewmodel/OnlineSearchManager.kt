package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Food
import com.emilflach.lokcal.data.OnlineFoodItem
import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.sources.AlbertHeijnFoodSource
import com.emilflach.lokcal.data.sources.FoodSource
import com.emilflach.lokcal.data.sources.OpenFoodFactsFoodSource
import com.emilflach.lokcal.data.sources.RateLimiter
import com.emilflach.lokcal.data.sources.SourceRegistry
import com.emilflach.lokcal.data.sources.SourceType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnlineSearchManager(
    private val settingsRepo: SettingsRepository,
    private val scope: CoroutineScope
) {
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
    }
    private val rateLimiter = RateLimiter()
    private val sourceResults = mutableMapOf<String, MutableMap<Long, OnlineFoodItem>>()
    private val sourceTempIds = mutableMapOf<String, Long>()
    private var onlineSearchJob: Job? = null

    private var _isSearching = false
    val isSearching: Boolean get() = _isSearching

    private var _sections = emptyList<SearchSection>()
    val sections: List<SearchSection> get() = _sections

    fun search(query: String, onStateChanged: () -> Unit) {
        val q = query.trim()
        if (q.isEmpty()) return

        scope.launch {
            val sources = getPreferredSources()
            if (sources.isEmpty()) return@launch

            // Initialize sections with rate limit info
            _sections = sources.map { source ->
                val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
                SearchSection(
                    sourceId = source.id,
                    sourceName = source.displayName,
                    isSearching = false, // Start as false, will be set to true if we can request
                    remainingCooldown = cooldown,
                    error = if (cooldown > 0) "Wait 10 seconds to search again" else null
                )
            }
            _isSearching = true
            onStateChanged()

            // Clear transient maps
            sourceResults.clear()
            sourceTempIds.clear()

            // Cancel any previous job
            onlineSearchJob?.cancel()
            onlineSearchJob = scope.launch {
                try {
                    val jobs = sources.mapIndexed { index, source ->
                        launch {
                            val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
                            if (cooldown > 0) {
                                updateSourceSection(index, onStateChanged) {
                                    it.copy(
                                        isSearching = false,
                                        error = "Wait 10 seconds to search again",
                                        remainingCooldown = cooldown
                                    )
                                }
                                checkSearchFinished(onStateChanged)
                                return@launch
                            }

                            updateSourceSection(index, onStateChanged) { it.copy(isSearching = true) }
                            searchWithSource(source, q, index, onStateChanged)
                        }
                    }
                    jobs.joinAll()
                } catch (_: CancellationException) {
                    _isSearching = false
                    _sections = _sections.map { 
                        if (it.isSearching) it.copy(isSearching = false, error = "Search cancelled", progress = null) 
                        else it.copy(isSearching = false) 
                    }
                    onStateChanged()
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
        sectionIndex: Int,
        onStateChanged: () -> Unit
    ) {
        // Check rate limit
        if (!rateLimiter.canRequest(source.id, source.rateLimitSeconds)) {
            val cooldown = rateLimiter.getRemainingCooldown(source.id, source.rateLimitSeconds)
            updateSourceSection(sectionIndex, onStateChanged) {
                it.copy(
                    isSearching = false,
                    error = "Wait 10 seconds to search again",
                    remainingCooldown = cooldown
                )
            }
            checkSearchFinished(onStateChanged)
            return
        }

        try {
            // Record request and perform search
            rateLimiter.recordRequest(source.id)

            // Update with initial progress if source is WEB (AH)
            if (source.type == SourceType.WEB) {
                updateSourceSection(sectionIndex, onStateChanged) {
                    it.copy(progress = 0.1f)
                }
            }

            val items = withContext(Dispatchers.Default) { source.search(query) }

            // Get or initialize temp ID counter for this source
            val results = sourceResults.getOrPut(source.id) { mutableMapOf() }
            if (!sourceTempIds.containsKey(source.id)) {
                sourceTempIds[source.id] = (sourceTempIds.size + 1) * -1000L
            }

            val transient = items.map { item ->
                val tempId = sourceTempIds[source.id]!! - 1
                sourceTempIds[source.id] = tempId
                results[tempId] = item
                item.toFood(tempId, source.id)
            }

            updateSourceSection(sectionIndex, onStateChanged) {
                it.copy(
                    foods = transient,
                    isSearching = false,
                    noResults = transient.isEmpty(),
                    progress = null
                )
            }
        } catch (e: CancellationException) {
            updateSourceSection(sectionIndex, onStateChanged) {
                it.copy(
                    isSearching = false,
                    error = "Search cancelled",
                    progress = null
                )
            }
            throw e
        } catch (_: Throwable) {
            updateSourceSection(sectionIndex, onStateChanged) {
                it.copy(
                    isSearching = false,
                    error = "Could not connect to ${source.displayName}",
                    progress = null
                )
            }
        } finally {
            checkSearchFinished(onStateChanged)
        }
    }

    private fun updateSourceSection(
        index: Int,
        onStateChanged: () -> Unit,
        update: (SearchSection) -> SearchSection
    ) {
        val newSections = _sections.toMutableList()
        if (index in newSections.indices) {
            newSections[index] = update(newSections[index])
            _sections = newSections
            onStateChanged()
        }
    }

    private fun checkSearchFinished(onStateChanged: () -> Unit) {
        if (_sections.all { !it.isSearching }) {
            _isSearching = false
            onStateChanged()
        }
    }

    private suspend fun getPreferredSources(): List<FoodSource> {
        val prefs = settingsRepo.getSourcePreferences()
        return if (prefs.isNotEmpty()) {
            sourceRegistry.getByIds(prefs)
        } else {
            // Default: use only OpenFoodFacts
            sourceRegistry.getById("off")?.let { listOf(it) } ?: emptyList()
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

    fun cancel() {
        onlineSearchJob?.cancel()
        onlineSearchJob = null
        _isSearching = false
        _sections = _sections.map { 
            if (it.isSearching) it.copy(isSearching = false, error = "Search cancelled", progress = null) 
            else it.copy(isSearching = false) 
        }
    }

    private fun OnlineFoodItem.toFood(tempId: Long, source: String) = Food(
        id = tempId,
        name = name,
        energy_kcal_per_100g = energyKcalPer100g ?: 0.0,
        unit = "g",
        serving_size = servingSize?.toString(),
        gtin13 = gtin13,
        image_url = imageUrl,
        product_url = productUrl,
        source = source,
        created_at = ""
    )
}
