package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.data.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditExerciseViewModel(private val repo: ExerciseRepository, private val id: Long) {
    data class UiState(
        val type: ExerciseRepository.Type = ExerciseRepository.Type.WALKING,
        val minutesText: String = "30",
        val notes: String = "",
        val kcalPreview: Double = (ExerciseRepository.Type.WALKING.kcalPerHour / 60.0) * 30.0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { load() }

    private fun parseMinutes(text: String): Double = text.trim().replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    private fun calc(type: ExerciseRepository.Type, minutes: Double): Double = (type.kcalPerHour / 60.0) * minutes

    fun setType(type: ExerciseRepository.Type) {
        val m = parseMinutes(_state.value.minutesText)
        _state.value = _state.value.copy(type = type, kcalPreview = calc(type, m))
    }

    fun setMinutesText(text: String) {
        val m = parseMinutes(text)
        _state.value = _state.value.copy(minutesText = text, kcalPreview = calc(_state.value.type, m))
    }

    fun setNotes(text: String) {
        _state.value = _state.value.copy(notes = text)
    }

    fun load() {
        val e = repo.getById(id) ?: return
        val type = ExerciseRepository.Type.fromDb(e.exercise_type)
        val minutesText = e.duration_min.toInt().toString()
        val notes = e.notes ?: ""
        _state.value = UiState(
            type = type,
            minutesText = minutesText,
            notes = notes,
            kcalPreview = calc(type, e.duration_min)
        )
    }

    fun save() {
        val minutes = parseMinutes(_state.value.minutesText)
        repo.updateExercise(id, _state.value.type, minutes, _state.value.notes.ifBlank { null })
    }

    fun delete() { repo.deleteById(id) }
}
