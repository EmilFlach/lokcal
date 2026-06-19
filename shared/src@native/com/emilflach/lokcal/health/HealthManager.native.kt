package com.emilflach.lokcal.health

actual fun allowAutomaticExerciseLogging(): Boolean {
    // TODO: Implement HealthKit integration for iOS
    return false
}

internal actual suspend fun getStepsData(
    healthClient: Any?
): Int = -1