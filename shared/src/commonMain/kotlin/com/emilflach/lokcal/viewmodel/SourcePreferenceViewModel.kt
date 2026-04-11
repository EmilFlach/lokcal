package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.data.SettingsRepository
import com.emilflach.lokcal.data.sources.*
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
    data class UiState(
        val optionalSources: List<FoodSource> = emptyList(),
        val selectedSourceId: String? = null,
        val isLoading: Boolean = true
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val sourceRegistry = SourceRegistry().apply {
        register(AlbertHeijnFoodSource())
        register(EsselungaFoodSource())
        register(KrogerFoodSource())
    }

    init {
        loadSources()
    }

    private fun loadSources() {
        scope.launch {
            val preferences = settingsRepo.getSourcePreferences()
            val selectedId = preferences.firstOrNull()
            _state.value = UiState(
                optionalSources = sourceRegistry.getAll(),
                selectedSourceId = selectedId,
                isLoading = false
            )
        }
    }

    fun selectSource(sourceId: String) {
        scope.launch {
            settingsRepo.clearSourcePreferences()
            settingsRepo.setSourcePreference(1L, sourceId)
            _state.value = _state.value.copy(selectedSourceId = sourceId)
        }
    }

    fun selectNone() {
        scope.launch {
            settingsRepo.clearSourcePreferences()
            settingsRepo.setSourcePreference(1L, "none")
            _state.value = _state.value.copy(selectedSourceId = "none")
        }
    }
}
