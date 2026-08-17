@file:OptIn(ExperimentalTime::class)

package com.emilflach.lokcal.health

import com.emilflach.lokcal.data.AutomaticStepsDayState
import com.emilflach.lokcal.data.ExerciseRepository
import com.emilflach.lokcal.data.SettingsRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Keeps the Health Connect query window stable while the device changes timezones. */
class AutomaticStepsSyncManager(
    private val exerciseRepository: ExerciseRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun sync(): Boolean = sync(
        now = Clock.System.now(),
        timeZone = TimeZone.currentSystemDefault(),
        readSteps = HealthManager::readSteps,
        readHourlySteps = HealthManager::readStepsByHour,
    )

    internal suspend fun sync(
        now: Instant,
        timeZone: TimeZone,
        readSteps: suspend (Long, Long) -> Int?,
        readHourlySteps: (suspend (Long, Long) -> List<TimedSteps>?)? = null,
    ): Boolean {
        val nowMillis = now.toEpochMilliseconds()
        val localDate = now.toLocalDateTime(timeZone).date
        var state = settingsRepository.getAutomaticStepsDayState()
            ?.takeIf { runCatching { LocalDate.parse(it.dateIso) }.isSuccess }

        if (state == null) {
            state = newState(localDate, timeZone)
            if (!settingsRepository.setAutomaticStepsDayState(state)) return false
        } else {
            val storedDate = LocalDate.parse(state.dateIso)
            if (localDate > storedDate) {
                val dayDifference = localDate.toEpochDays() - storedDate.toEpochDays()
                val boundary = localDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
                if (dayDifference == 1L && boundary > state.startEpochMillis) {
                    if (!readAndStoreSteps(
                            dateIso = state.dateIso,
                            startMillis = state.startEpochMillis,
                            endMillis = boundary,
                            timeZone = timeZone,
                            readSteps = readSteps,
                            readHourlySteps = readHourlySteps,
                        )) return false
                }

                state = newState(localDate, timeZone)
                if (!settingsRepository.setAutomaticStepsDayState(state)) return false
            }
        }

        if (nowMillis <= state.startEpochMillis) return false
        return readAndStoreSteps(
            dateIso = state.dateIso,
            startMillis = state.startEpochMillis,
            endMillis = nowMillis,
            timeZone = timeZone,
            readSteps = readSteps,
            readHourlySteps = readHourlySteps,
        )
    }

    private suspend fun readAndStoreSteps(
        dateIso: String,
        startMillis: Long,
        endMillis: Long,
        timeZone: TimeZone,
        readSteps: suspend (Long, Long) -> Int?,
        readHourlySteps: (suspend (Long, Long) -> List<TimedSteps>?)?,
    ): Boolean {
        if (readHourlySteps == null) {
            val count = readSteps(startMillis, endMillis) ?: return false
            exerciseRepository.logAutomaticSteps(dateIso, count)
            return true
        }

        val timedSteps = readHourlySteps(startMillis, endMillis) ?: return false
        val stepsByHour = timedSteps
            .groupBy { Instant.fromEpochMilliseconds(it.startEpochMillis).toLocalDateTime(timeZone).hour }
            .mapValues { (_, buckets) -> buckets.sumOf { it.count } }
        exerciseRepository.logAutomaticStepsByHour(dateIso, stepsByHour)
        return true
    }

    private fun newState(date: LocalDate, timeZone: TimeZone) =
        AutomaticStepsDayState(
            dateIso = date.toString(),
            startEpochMillis = date.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        )
}
