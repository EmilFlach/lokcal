package com.emilflach.lokcal.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BurnBucketsTest {
    private fun emptyDay() = MutableList(BurnBuckets.BUCKETS_PER_DAY) { 0.0 }

    private fun dayWith(vararg buckets: Int) = emptyDay().also { day ->
        buckets.forEach { day[it] = 100.0 }
    }

    @Test
    fun of_splitsEachHourInHalf() {
        assertEquals(0, BurnBuckets.of("2026-08-17T00:00:00"))
        assertEquals(1, BurnBuckets.of("2026-08-17T00:30:00"))
        assertEquals(1, BurnBuckets.of("2026-08-17T00:59:59"))
        assertEquals(28, BurnBuckets.of("2026-08-17T14:00:00"))
        assertEquals(29, BurnBuckets.of("2026-08-17T14:45:00"))
        assertEquals(47, BurnBuckets.of("2026-08-17T23:59:59"))
    }

    @Test
    fun of_rejectsUnparseableTimes() {
        assertNull(BurnBuckets.of("2026-08-17"))
        assertNull(BurnBuckets.of("2026-08-17T"))
        assertNull(BurnBuckets.of("2026-08-17Tab:cd:ef"))
        assertNull(BurnBuckets.of("2026-08-17T24:00:00"))
        assertNull(BurnBuckets.of("2026-08-17T12:60:00"))
    }

    @Test
    fun label_readsAsClockTime_andEndsDayAt2400() {
        assertEquals("00:00", BurnBuckets.label(0))
        assertEquals("00:30", BurnBuckets.label(1))
        assertEquals("14:30", BurnBuckets.label(29))
        assertEquals("23:30", BurnBuckets.label(47))
        // The trailing edge, so the last slot reads as 23:30–24:00 rather than 23:30–00:00.
        assertEquals("24:00", BurnBuckets.label(BurnBuckets.BUCKETS_PER_DAY))
    }

    @Test
    fun focusedRange_coversWholeDayWhenNothingLogged() {
        val range = BurnBuckets.focusedRange(emptyDay())
        assertEquals(BurnBuckets.BUCKETS_PER_DAY, range.size)
        assertEquals(0, range.first())
        assertEquals(BurnBuckets.BUCKETS_PER_DAY - 1, range.last())
    }

    @Test
    fun focusedRange_zoomsToAnEvenNumberOfWholeHours() {
        // Single slot at 14:00–14:30.
        val range = BurnBuckets.focusedRange(dayWith(28))
        assertTrue(28 in range, "active slot must stay visible, got $range")
        assertEquals(0, range.first() % BurnBuckets.BUCKETS_PER_HOUR, "window starts on the hour")
        assertEquals(0, range.size % (2 * BurnBuckets.BUCKETS_PER_HOUR), "even hour count")
        assertTrue(range.size >= 6 * BurnBuckets.BUCKETS_PER_HOUR, "at least 6h wide, got $range")
    }

    @Test
    fun focusedRange_staysInsideTheDayAtItsEdges() {
        val fromMidnight = BurnBuckets.focusedRange(dayWith(0))
        assertEquals(0, fromMidnight.first())
        assertTrue(0 in fromMidnight)

        val untilMidnight = BurnBuckets.focusedRange(dayWith(47))
        assertEquals(BurnBuckets.BUCKETS_PER_DAY - 1, untilMidnight.last())
        assertTrue(47 in untilMidnight)
    }

    @Test
    fun slotsPerBar_keepsHalfHourBarsWhenTheyFit() {
        // A 6h window (12 slots) on a phone-width card.
        assertEquals(1, BurnBuckets.slotsPerBar(12, availableWidth = 328f, spacing = 2f, minBarWidth = 10f))
        // A full day (48 slots) can't show 30-minute bars at 10dp, so it groups into hours.
        assertEquals(2, BurnBuckets.slotsPerBar(48, availableWidth = 328f, spacing = 2f, minBarWidth = 10f))
        // On a wide desktop window the whole day fits at 30-minute resolution.
        assertEquals(1, BurnBuckets.slotsPerBar(48, availableWidth = 900f, spacing = 2f, minBarWidth = 10f))
        // Very narrow: falls back to the widest grouping rather than giving up.
        assertEquals(4, BurnBuckets.slotsPerBar(48, availableWidth = 80f, spacing = 2f, minBarWidth = 10f))
    }

    @Test
    fun slotsPerBar_alwaysDividesTheWindowEvenly() {
        val widths = listOf(80f, 200f, 328f, 420f, 900f)
        (0..<BurnBuckets.BUCKETS_PER_DAY).forEach { active ->
            val window = BurnBuckets.focusedRange(dayWith(active))
            widths.forEach { width ->
                val slots = BurnBuckets.slotsPerBar(window.size, width, spacing = 2f, minBarWidth = 10f)
                assertEquals(
                    0, window.size % slots,
                    "window ${window.size} slots doesn't divide into bars of $slots at ${width}dp",
                )
                val groups = BurnBuckets.barGroups(window, slots)
                assertEquals(window.size / slots, groups.size)
                assertEquals(window, groups.flatten(), "grouping must cover the window exactly")
                assertTrue(groups.all { it.size == slots }, "ragged final bar in $groups")
            }
        }
    }

    @Test
    fun barGroups_spanWholeSlicesOfTime() {
        val hourly = BurnBuckets.barGroups((0..<8).toList(), BurnBuckets.BUCKETS_PER_HOUR)
        assertEquals(listOf(listOf(0, 1), listOf(2, 3), listOf(4, 5), listOf(6, 7)), hourly)
        // A bar's popup range reads from its first slot to just past its last.
        val first = hourly.first()
        assertEquals("00:00", BurnBuckets.label(first.first()))
        assertEquals("01:00", BurnBuckets.label(first.last() + 1))
    }

    @Test
    fun ticks_areEvenlySpacedWholeHours() {
        val fullDay = BurnBuckets.ticks(BurnBuckets.focusedRange(emptyDay()))
        assertEquals(listOf("00:00", "06:00", "12:00", "18:00", "24:00"), fullDay.map(BurnBuckets::label))
    }

    @Test
    fun ticks_stayOnTheHourForEveryPossibleWindow() {
        val windows = (0..<BurnBuckets.BUCKETS_PER_DAY).map { BurnBuckets.focusedRange(dayWith(it)) } +
            listOf(BurnBuckets.focusedRange(dayWith(2, 45)))

        windows.forEach { window ->
            val ticks = BurnBuckets.ticks(window)
            assertTrue(ticks.size >= 2, "expected multiple ticks for $window")
            assertTrue(
                ticks.all { it % BurnBuckets.BUCKETS_PER_HOUR == 0 },
                "ticks off the hour: ${ticks.map(BurnBuckets::label)}",
            )
            // Labels are laid out with even spacing, so their values must be evenly spaced too.
            val steps = ticks.zipWithNext { a, b -> b - a }.distinct()
            assertEquals(1, steps.size, "uneven tick steps $steps for ${ticks.map(BurnBuckets::label)}")
            assertEquals(window.first(), ticks.first(), "first tick must sit at the window start")
            assertEquals(window.last() + 1, ticks.last(), "last tick must sit at the window's end edge")
        }
    }
}
