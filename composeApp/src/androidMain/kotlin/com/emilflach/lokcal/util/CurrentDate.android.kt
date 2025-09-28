package com.emilflach.lokcal.util

import java.util.*

actual fun currentDateIso(): String {
    val c = Calendar.getInstance()
    val y = c.get(Calendar.YEAR)
    val m = c.get(Calendar.MONTH) + 1
    val d = c.get(Calendar.DAY_OF_MONTH)
    val mm = if (m < 10) "0$m" else "$m"
    val dd = if (d < 10) "0$d" else "$d"
    return "$y-$mm-$dd"
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
    val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
    val localDateTime = java.time.LocalDateTime.parse(isoTimestamp, formatter)
    return localDateTime.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
}

/**
 * Returns a Pair of epoch milliseconds representing the start and end of today.
 */
fun getTodayMillisRange(): Pair<Long, Long> {
    val (startIso, endIso) = getTodayTimeRange()
    return isoTimestampToMillis(startIso) to isoTimestampToMillis(endIso)
}
