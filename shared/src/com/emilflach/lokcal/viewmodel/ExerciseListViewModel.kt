package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Exercise
import com.emilflach.lokcal.ExerciseType
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.ExerciseTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseListViewModel(
    private val repo: ExerciseRepository,
    private val typeRepo: ExerciseTypeRepository,
    private val dateIso: String,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    data class UiState(
        val items: List<Exercise> = emptyList(),
        val totalKcal: Double = 0.0,
        val typeKcalMap: Map<String, Double> = emptyMap(),
        val typeMap: Map<String, ExerciseType> = emptyMap(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { loadForSelectedDate() }

    private fun rangeFor(date: String): Pair<String, String> =
        "${date}T00:00:00" to "${date}T23:59:59"

    fun updateDuration(typeName: String, minutes: Double) {
        val (start, end) = rangeFor(dateIso)
        val timestamp = dateIso + "T12:00:00"
        val kcalPerHour = _state.value.typeKcalMap[typeName] ?: return

        scope.launch {
            val list = repo.getByDateRange(start, end)
            val existing = list.firstOrNull { it.exercise_type == typeName }
            if (existing == null) {
                if (minutes > 0) {
                    repo.logExercise(typeName, kcalPerHour, minutes, timestamp)
                }
            } else {
                if (minutes > 0) {
                    repo.updateExercise(existing.id, typeName, kcalPerHour, minutes, existing.notes)
                } else {
                    repo.deleteById(existing.id)
                }
            }
            loadForSelectedDate()
        }
    }

    private fun loadForSelectedDate() {
        scope.launch {
            val dbTypes = typeRepo.getAll()
            val kcalMap = mutableMapOf<String, Double>()
            dbTypes.forEach { kcalMap[it.name] = it.kcal_per_hour }

            val (start, end) = rangeFor(dateIso)
            val list = repo.getByDateRange(start, end)

            val allItems = dbTypes.map { type ->
                list.firstOrNull { it.exercise_type == type.name } ?: Exercise(
                    id = -1,
                    timestamp = dateIso + "T12:00:00",
                    exercise_type = type.name,
                    duration_min = 0.0,
                    energy_kcal_total = 0.0,
                    notes = null
                )
            }

            val total = list.sumOf { it.energy_kcal_total }
            val typeMap = dbTypes.associateBy { it.name }
            _state.value = UiState(items = allItems, totalKcal = total, typeKcalMap = kcalMap, typeMap = typeMap)
        }
    }
}
