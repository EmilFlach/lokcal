package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.util.currentDateIso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ExerciseViewModel(private val repo: ExerciseRepository) {
    data class UiState(
        val type: ExerciseRepository.Type = ExerciseRepository.Type.WALKING,
        val minutesText: String = "30",
        val kcalPreview: Double = (ExerciseRepository.Type.WALKING.kcalPerHour / 60.0) * 30.0,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setType(type: ExerciseRepository.Type) {
        val minutes = parseMinutes(_state.value.minutesText)
        val kcal = calc(type, minutes)
        _state.value = _state.value.copy(type = type, kcalPreview = kcal)
    }

    fun setMinutesText(text: String) {
        val minutes = parseMinutes(text)
        val kcal = calc(_state.value.type, minutes)
        _state.value = _state.value.copy(minutesText = text, kcalPreview = kcal)
    }

    private fun parseMinutes(text: String): Double = text.trim().replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0

    private fun calc(type: ExerciseRepository.Type, minutes: Double): Double = (type.kcalPerHour / 60.0) * minutes

    private fun nowIso(): String = currentDateIso() + "T12:00:00"

    fun save() {
        val minutes = parseMinutes(_state.value.minutesText)
        repo.logExercise(_state.value.type, minutes, timestamp = nowIso(), notes = null)
    }
}
