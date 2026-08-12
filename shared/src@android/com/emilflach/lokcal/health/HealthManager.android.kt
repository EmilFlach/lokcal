package com.emilflach.lokcal.health

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

actual fun allowAutomaticExerciseLogging(): Boolean {
    return true
}

internal actual suspend fun getStepsData(
    healthClient: Any?,
    startInclusiveMillis: Long,
    endExclusiveMillis: Long,
): Int? {
    val start = Instant.ofEpochMilli(startInclusiveMillis)
    val end = Instant.ofEpochMilli(endExclusiveMillis)
    Log.d("HealthConnect", "Aggregating steps from $start (inclusive) to $end (exclusive)")
    return if (healthClient is HealthConnectClient) {
        try {
            val response = healthClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        start,
                        end,
                    )
                )
            )
            val total = response[StepsRecord.COUNT_TOTAL] ?: 0L
            Log.d("HealthConnect", "Aggregated steps: $total")
            total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading steps data", e)
            null
        }
    } else {
        Log.e("HealthConnect", "Health Connect client is null or invalid type: ${healthClient?.javaClass?.name}")
        null
    }
}
