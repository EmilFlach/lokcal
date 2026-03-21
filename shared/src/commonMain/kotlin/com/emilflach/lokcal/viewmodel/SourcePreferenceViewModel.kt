package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.scraper.AlbertHeijnFoodSource
import com.emilflach.lokcal.data.scraper.FoodSource
import com.emilflach.lokcal.data.scraper.OpenFoodFactsFoodSource
import com.emilflach.lokcal.data.scraper.SourceRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SourcePreferenceViewModel(
    private val settingsRepo: SettingsRepository
) {
    data class SourceItem(
        val source: FoodSource,
        val isSelected: Boolean,
        val priority: Int? = null  // 1 or 2, null if not selected
    )

    data class UiState(
        val sources: List<SourceItem> = emptyList(),
        val isLoading: Boolean = true
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sourceRegistry = SourceRegistry().apply {
        register(AlbertHeijnFoodSource())
        register(OpenFoodFactsFoodSource())
    }

    init {
        loadSources()
    }

    private fun loadSources() {
        scope.launch {
            val preferences = settingsRepo.getSourcePreferences()
            val allSources = sourceRegistry.getAll()

            println("[SourcePreferenceVM] Loading sources - preferences: $preferences, allSources: ${allSources.map { it.id }}")

            val items = allSources.map { source ->
                val index = preferences.indexOf(source.id)
                SourceItem(
                    source = source,
                    isSelected = index >= 0,
                    priority = if (index >= 0) index + 1 else null
                )
            }

            println("[SourcePreferenceVM] Loaded ${items.size} items: ${items.map { "${it.source.id}:${it.isSelected}" }}")

            _state.value = UiState(
                sources = items,
                isLoading = false
            )
        }
    }

    fun toggleSource(sourceId: String) {
        println("[SourcePreferenceVM] toggleSource called for: $sourceId")
        scope.launch {
            val currentPrefs = settingsRepo.getSourcePreferences()
            val isCurrentlySelected = currentPrefs.contains(sourceId)

            println("[SourcePreferenceVM] Current prefs: $currentPrefs, isCurrentlySelected: $isCurrentlySelected")

            if (isCurrentlySelected) {
                // Deselect - find and remove this source, then reindex remaining
                println("[SourcePreferenceVM] Deselecting source: $sourceId")
                val remainingSources = currentPrefs.filter { it != sourceId }

                // Clear all preferences
                settingsRepo.clearSourcePreferences()

                // Re-add remaining sources with correct priorities
                remainingSources.forEachIndexed { index, id ->
                    settingsRepo.setSourcePreference((index + 1).toLong(), id)
                }
            } else {
                // Select (if less than 2 selected)
                if (currentPrefs.size >= 2) {
                    println("[SourcePreferenceVM] Cannot select - already have 2 sources selected")
                    return@launch
                }
                val newPriority = currentPrefs.size + 1
                println("[SourcePreferenceVM] Selecting source with priority: $newPriority")
                settingsRepo.setSourcePreference(newPriority.toLong(), sourceId)
            }

            println("[SourcePreferenceVM] Reloading sources after toggle")
            loadSources()
        }
    }

    fun swapPriority(sourceId1: String, sourceId2: String) {
        scope.launch {
            val items = _state.value.sources
            val item1 = items.find { it.source.id == sourceId1 && it.isSelected } ?: return@launch
            val item2 = items.find { it.source.id == sourceId2 && it.isSelected } ?: return@launch

            val priority1 = item1.priority ?: return@launch
            val priority2 = item2.priority ?: return@launch

            // Swap priorities
            settingsRepo.setSourcePreference(priority1.toLong(), sourceId2)
            settingsRepo.setSourcePreference(priority2.toLong(), sourceId1)

            loadSources()
        }
    }
}
