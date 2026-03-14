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

    suspend fun readSteps(): Int = getStepsData(healthProvider)

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


expect fun allowAutomaticExerciseLogging(): Boolean

internal expect suspend fun getStepsData(healthClient: Any?): Int