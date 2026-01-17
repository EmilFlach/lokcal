@file:OptIn(ExperimentalTime::class)

package com.emilflach.lokcal.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

/**
 * Returns the current local date in ISO-8601 format (YYYY-MM-DD).
 */
fun currentDateIso(): String {
    return System.todayIn(TimeZone.currentSystemDefault()).toString()
}

/**
 * Returns a Pair of timestamps representing the start and end of today in ISO-8601 format.
 * The first value is today at 00:00:00, and the second value is today at 23:59:59.
 */
fun getTodayTimeRange(): Pair<String, String> {
    val today = currentDateIso()
    return "${today}T00:00:00" to "${today}T23:59:59"
}

/**
 * Converts an ISO-8601 formatted timestamp to epoch milliseconds.
 */
fun isoTimestampToMillis(isoTimestamp: String): Long {
    val localDateTime = LocalDateTime.parse(isoTimestamp)
    return localDateTime.toInstant(TimeZone.UTC).toEpochMilliseconds()
}

/**
 * Returns a Pair of epoch milliseconds representing the start and end of today.
 */
fun getTodayMillisRange(): Pair<Long, Long> {
    val (startIso, endIso) = getTodayTimeRange()
    return isoTimestampToMillis(startIso) to isoTimestampToMillis(endIso)
}
