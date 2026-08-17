package com.emilflach.lokcal.health

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object HealthManager {

    private var healthProvider: Any? = null
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()

    private var requestPermissionsCallback: (() -> Unit)? = null

    fun showAutomaticExerciseLogging() = allowAutomaticExerciseLogging()

    fun setHealthProvider(provider: Any?) {
        healthProvider = provider
    }

    suspend fun readSteps(startInclusiveMillis: Long, endExclusiveMillis: Long): Int? =
        getStepsData(healthProvider, startInclusiveMillis, endExclusiveMillis)

    /** Reads elapsed-hour buckets; callers map each instant back to the local clock hour. */
    internal suspend fun readStepsByHour(
        startInclusiveMillis: Long,
        endExclusiveMillis: Long,
    ): List<TimedSteps>? {
        val result = mutableListOf<TimedSteps>()
        var start = startInclusiveMillis
        while (start < endExclusiveMillis) {
            val end = minOf(start + MILLIS_PER_HOUR, endExclusiveMillis)
            val steps = getStepsData(healthProvider, start, end) ?: return null
            result += TimedSteps(startEpochMillis = start, count = steps)
            start = end
        }
        return result
    }

    fun setPermissionsGranted(bool: Boolean) {
        _permissionsGranted.value = bool
    }

    fun arePermissionsGranted(): Boolean = _permissionsGranted.value

    fun setRequestPermissionsCallback(callback: () -> Unit) {
        requestPermissionsCallback = callback
    }

    fun requestPermissions() {
        requestPermissionsCallback?.invoke()
    }
}

internal data class TimedSteps(val startEpochMillis: Long, val count: Int)

private const val MILLIS_PER_HOUR = 60L * 60L * 1_000L

expect fun allowAutomaticExerciseLogging(): Boolean

internal expect suspend fun getStepsData(
    healthClient: Any?,
    startInclusiveMillis: Long,
    endExclusiveMillis: Long,
): Int?
