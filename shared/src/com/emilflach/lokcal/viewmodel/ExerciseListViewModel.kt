package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.Exercise
import com.emilflach.lokcal.ExerciseType
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.ExerciseTypeRepository
import com.emilflach.lokcal.util.BurnBuckets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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
        /** Calories burned per 30-minute slot of the selected day. */
        val bucketKcal: List<Double> = List(BurnBuckets.BUCKETS_PER_DAY) { 0.0 },
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { loadForSelectedDate() }

    private fun rangeFor(date: String): Pair<String, String> =
        "${date}T00:00:00" to "${date}T23:59:59"

    fun updateDuration(typeName: String, minutes: Double) {
        val (start, end) = rangeFor(dateIso)
        val timestamp = timestampForNewExercise()
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
                    if (dateIso == Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()) {
                        repo.updateTimestamp(existing.id, timestamp)
                    }
                } else {
                    repo.deleteById(existing.id)
                }
            }
            loadForSelectedDate()
        }
    }

    private fun timestampForNewExercise(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        if (now.date.toString() != dateIso) return dateIso + "T12:00:00"

        val hour = now.hour.toString().padStart(2, '0')
        val minute = now.minute.toString().padStart(2, '0')
        val second = now.second.toString().padStart(2, '0')
        return "${dateIso}T$hour:$minute:$second"
    }

    private fun loadForSelectedDate() {
        scope.launch {
            val dbTypes = typeRepo.getAll()
            val kcalMap = mutableMapOf<String, Double>()
            dbTypes.forEach { kcalMap[it.name] = it.kcal_per_hour }

            val (start, end) = rangeFor(dateIso)
            val list = repo.getByDateRange(start, end)

            val allItems = dbTypes.map { type ->
                val matching = list.filter { it.exercise_type == type.name }
                val logged = matching.firstOrNull()?.let { first ->
                    if (type.name == ExerciseRepository.AUTOMATIC_STEPS_KEY) {
                        first.copy(
                            duration_min = matching.sumOf { it.duration_min },
                            energy_kcal_total = matching.sumOf { it.energy_kcal_total },
                        )
                    } else first
                }
                logged ?: Exercise(
                    id = -1,
                    timestamp = dateIso + "T12:00:00",
                    exercise_type = type.name,
                    duration_min = 0.0,
                    energy_kcal_total = 0.0,
                    notes = null
                )
            }

            val total = list.sumOf { it.energy_kcal_total }
            val bucketKcal = MutableList(BurnBuckets.BUCKETS_PER_DAY) { 0.0 }
            list.forEach { exercise ->
                val bucket = BurnBuckets.of(exercise.timestamp)
                if (bucket != null) bucketKcal[bucket] += exercise.energy_kcal_total
            }
            val typeMap = dbTypes.associateBy { it.name }
            _state.value = UiState(
                items = allItems,
                totalKcal = total,
                typeKcalMap = kcalMap,
                typeMap = typeMap,
                bucketKcal = bucketKcal,
            )
        }
    }
}
