package com.emilflach.lokcal.health

actual fun allowAutomaticExerciseLogging(): Boolean {
    return true
}

internal actual suspend fun getStepsData(
    healthClient: Any?
): Int = -1