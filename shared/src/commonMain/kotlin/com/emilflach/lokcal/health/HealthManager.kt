package com.emilflach.lokcal.health

object HealthManager {

    private var healthProvider: Any? = null
    private var permissionsGranted: Boolean = false

    fun showAutomaticExerciseLogging() = allowAutomaticExerciseLogging()

    fun setHealthProvider(provider: Any?) {
        healthProvider = provider
    }

    suspend fun readSteps(): Int = getStepsData(healthProvider)

    fun setPermissionsGranted(bool: Boolean) {
        permissionsGranted = bool
    }

    fun arePermissionsGranted(): Boolean = permissionsGranted
}


expect fun allowAutomaticExerciseLogging(): Boolean

internal expect suspend fun getStepsData(healthClient: Any?): Int