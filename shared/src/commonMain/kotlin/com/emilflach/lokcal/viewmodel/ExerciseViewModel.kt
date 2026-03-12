package com.emilflach.lokcal.viewmodel

import com.emilflach.lokcal.data.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExerciseViewModel(private val repo: ExerciseRepository, private val dateIso: String) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

    private fun timestampForDate(): String = dateIso + "T12:00:00"

    fun save() {
        val minutes = parseMinutes(_state.value.minutesText)
        scope.launch {
            repo.logExercise(_state.value.type, minutes, timestamp = timestampForDate(), notes = null)
        }
    }
}
