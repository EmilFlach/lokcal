package com.emilflach.lokcal.health

actual fun allowAutomaticExerciseLogging(): Boolean {
    return false
}

internal actual suspend fun getStepsData(
    healthClient: Any?,
    startInclusiveMillis: Long,
    endExclusiveMillis: Long,
): Int? = null
