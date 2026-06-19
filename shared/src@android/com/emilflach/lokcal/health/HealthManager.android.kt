package com.emilflach.lokcal.health

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.emilflach.lokcal.util.getTodayMillisRange
import java.time.Instant

actual fun allowAutomaticExerciseLogging(): Boolean {
    return true
}

internal actual suspend fun getStepsData(healthClient: Any?): Int {

    val (startTime, endTime) = getTodayMillisRange()
    Log.d("HealthConnect", "Attempting to read steps from $startTime to $endTime")
    return if (healthClient is HealthConnectClient) {
        try {
            var total = 0L
            val response = healthClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(startTime),
                        Instant.ofEpochMilli(endTime)
                    )
                )
            )
            Log.d("HealthConnect", "Response received: ${response.records.size} records found")
            if (response.records.isEmpty()) {
                -1
            } else {
                response.records.forEach {
                    total += it.count
                }
                Log.d("HealthConnect", "Total steps: $total")
                total.toInt()
            }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading steps data", e)
            -1
        }
    } else {
        Log.e("HealthConnect", "Health Connect client is null or invalid type: ${healthClient?.javaClass?.name}")
        -1
    }
}