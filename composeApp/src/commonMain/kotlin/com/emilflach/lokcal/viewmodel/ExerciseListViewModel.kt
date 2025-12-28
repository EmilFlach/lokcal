package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Exercise
import com.emilflach.lokcal.data.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExerciseListViewModel(private val repo: ExerciseRepository, private val dateIso: String) {
    data class UiState(
        val items: List<Exercise> = emptyList(),
        val totalKcal: Double = 0.0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { loadForSelectedDate() }

    private fun rangeFor(date: String): Pair<String, String> {
        return "${date}T00:00:00" to "${date}T23:59:59"
    }

    fun updateDuration(type: String, minutes: Double) {
        val (start, end) = rangeFor(dateIso)
        val timestamp = dateIso + "T12:00:00"
        val list = repo.getByDateRange(start, end)
        val existing = list.firstOrNull { it.exercise_type == type }
        
        if (existing == null) {
            if (minutes > 0) {
                repo.logExercise(ExerciseRepository.Type.fromDb(type), minutes, timestamp)
            }
        } else {
            if (minutes > 0) {
                repo.updateExercise(existing.id, ExerciseRepository.Type.fromDb(type), minutes, existing.notes)
            } else {
                repo.deleteById(existing.id)
            }
        }
        loadForSelectedDate()
    }

    private fun loadForSelectedDate() {
        val (start, end) = rangeFor(dateIso)
        val list = repo.getByDateRange(start, end)
        
        // Always include all types, even if not in DB
        val allTypes = ExerciseRepository.Type.entries
        val uiItems = allTypes.map { type ->
            list.firstOrNull { it.exercise_type == type.dbName } ?: Exercise(
                id = -1, // Temporary ID for items not in DB
                timestamp = dateIso + "T12:00:00",
                exercise_type = type.dbName,
                duration_min = 0.0,
                energy_kcal_total = 0.0,
                notes = null
            )
        }
        
        val total = list.sumOf { it.energy_kcal_total }
        _state.value = UiState(items = uiItems, totalKcal = total)
    }
}
