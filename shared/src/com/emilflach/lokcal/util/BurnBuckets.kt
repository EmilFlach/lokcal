package com.emilflach.lokcal.util

import com.emilflach.lokcal.util.BurnBuckets.MIN_VISIBLE_HOURS


/** Splits a day into fixed 30-minute slots, used to chart calories burned over time. */
object BurnBuckets {
    const val MINUTES_PER_BUCKET = 30
    const val BUCKETS_PER_HOUR = 60 / MINUTES_PER_BUCKET
    const val HOURS_PER_DAY = 24
    const val BUCKETS_PER_DAY = HOURS_PER_DAY * BUCKETS_PER_HOUR

    /** Shortest window [focusedRange] will zoom into. */
    private const val MIN_VISIBLE_HOURS = 6

    /** Most labels [ticks] will place along the time axis. */
    private const val MAX_TICKS = 5

    /** How many slots one bar may cover: 30 minutes, an hour, or two. */
    private val BAR_SPANS = listOf(1, BUCKETS_PER_HOUR, 2 * BUCKETS_PER_HOUR)

    /** Maps an `…THH:MM:SS` timestamp onto its slot, or null if the time can't be parsed. */
    fun of(timestamp: String): Int? {
        val time = timestamp.substringAfter('T', "")
        val hour = time.take(2).toIntOrNull() ?: return null
        val minute = time.drop(3).take(2).toIntOrNull() ?: return null
        if (hour !in 0..<HOURS_PER_DAY || minute !in 0..59) return null
        return hour * BUCKETS_PER_HOUR + minute / MINUTES_PER_BUCKET
    }

    /**
     * The clock time a slot starts at, e.g. `14:30`. [BUCKETS_PER_DAY] is the day's trailing
     * edge and renders as `24:00`, so a slot's end reads as `label(bucket + 1)`.
     */
    fun label(bucket: Int): String {
        val minuteOfDay = bucket.coerceIn(0, BUCKETS_PER_DAY) * MINUTES_PER_BUCKET
        val hour = (minuteOfDay / 60).toString().padStart(2, '0')
        val minute = (minuteOfDay % 60).toString().padStart(2, '0')
        return "$hour:$minute"
    }

    /**
     * The slots worth charting: the active span padded by an hour on each side, widened to at
     * least [MIN_VISIBLE_HOURS]. Always whole hours, and an even number of them, so [ticks] can
     * space labels evenly and still land them on the hour. Falls back to the whole day when
     * nothing is logged.
     */
    fun focusedRange(values: List<Double>): List<Int> {
        val active = values.indices.filter { values[it] > 0.0 }
        if (active.isEmpty()) return (0..<BUCKETS_PER_DAY).toList()

        val firstHour = active.first() / BUCKETS_PER_HOUR
        val lastHourExclusive = active.last() / BUCKETS_PER_HOUR + 1
        var startHour = (firstHour - 1).coerceAtLeast(0)
        var endHour = (lastHourExclusive + 1).coerceAtMost(HOURS_PER_DAY)
        while (endHour - startHour < MIN_VISIBLE_HOURS || (endHour - startHour) % 2 != 0) {
            when {
                startHour > 0 && endHour < HOURS_PER_DAY -> {
                    if (firstHour - startHour <= endHour - lastHourExclusive) startHour-- else endHour++
                }
                startHour > 0 -> startHour--
                endHour < HOURS_PER_DAY -> endHour++
                else -> break
            }
        }
        return (startHour * BUCKETS_PER_HOUR..<endHour * BUCKETS_PER_HOUR).toList()
    }

    /**
     * How many slots each bar should cover to stay at least [minBarWidth] wide — 30 minutes where
     * there is room, otherwise an hour or two. All measurements are in the same unit (dp).
     */
    fun slotsPerBar(bucketCount: Int, availableWidth: Float, spacing: Float, minBarWidth: Float): Int {
        if (bucketCount <= 0) return 1
        return BAR_SPANS.firstOrNull { span ->
            val bars = bucketCount / span
            bars > 0 && (availableWidth - spacing * (bars - 1)) / bars >= minBarWidth
        } ?: BAR_SPANS.last()
    }

    /** Splits a [focusedRange] window into the slot groups one bar each should sum up. */
    fun barGroups(visibleBuckets: List<Int>, slotsPerBar: Int): List<List<Int>> =
        visibleBuckets.chunked(slotsPerBar.coerceAtLeast(1))

    /**
     * Evenly spaced axis marks for a [focusedRange] window, including its trailing edge. The step
     * is a whole number of hours, so every mark reads as `HH:00`.
     */
    fun ticks(visibleBuckets: List<Int>): List<Int> {
        if (visibleBuckets.isEmpty()) return emptyList()
        val startHour = visibleBuckets.first() / BUCKETS_PER_HOUR
        val spanHours = (visibleBuckets.last() + 1) / BUCKETS_PER_HOUR - startHour
        if (spanHours <= 0) return listOf(visibleBuckets.first())

        val divisions = (MAX_TICKS - 1 downTo 1).first { spanHours % it == 0 }
        val stepHours = spanHours / divisions
        return (0..divisions).map { (startHour + it * stepHours) * BUCKETS_PER_HOUR }
    }
}
