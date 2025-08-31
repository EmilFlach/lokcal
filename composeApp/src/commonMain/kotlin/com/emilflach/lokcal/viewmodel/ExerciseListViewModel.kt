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
        val summaryText: String = "",
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { loadForSelectedDate() }

    private fun rangeFor(date: String): Pair<String, String> {
        return "${date}T00:00:00" to "${date}T23:59:59"
    }

    private fun loadForSelectedDate() {
        val (start, end) = rangeFor(dateIso)
        val list = repo.getByDateRange(start, end)
        val total = list.sumOf { it.energy_kcal_total }
        _state.value = UiState(items = list, totalKcal = total, summaryText = buildSummary(list))
    }

    fun delete(id: Long) {
        repo.deleteById(id)
        loadForSelectedDate()
    }

    private fun buildSummary(list: List<Exercise>): String {
        if (list.isEmpty()) return ""
        // Summarize by type, e.g., "Walking 30m, Running 20m"
        val minutesByType = list.groupBy { it.exercise_type }
            .mapValues { (_, v) -> v.sumOf { it.duration_min } }
        val parts = minutesByType.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { (type, min) ->
                val label = when (type) {
                    ExerciseRepository.Type.WALKING.dbName -> "Walking"
                    ExerciseRepository.Type.RUNNING.dbName -> "Running"
                    else -> type
                }
                "$label ${min.toInt()} m"
            }
        return parts.joinToString(", ")
    }
}
