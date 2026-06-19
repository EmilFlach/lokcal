package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Exercise
import com.emilflach.lokcal.util.currentDateIso

class ExerciseRepository(private val db: Database) {
    private val q get() = db.exerciseQueries

    companion object {
        const val AUTOMATIC_STEPS_KEY = "AUTOMATIC_STEPS"
        const val AUTOMATIC_STEPS_KCAL_PER_HOUR = 220.0
        const val AUTOMATIC_STEPS_DISPLAY_NAME = "Automatic step counter"

        fun displayName(typeName: String) =
            if (typeName == AUTOMATIC_STEPS_KEY) AUTOMATIC_STEPS_DISPLAY_NAME else typeName
    }

    suspend fun logExercise(typeName: String, kcalPerHour: Double, minutes: Double, timestamp: String, notes: String? = null) {
        require(minutes >= 0.0) { "minutes must be >= 0" }
        val total = (kcalPerHour / 60.0) * minutes
        q.logExercise(timestamp = timestamp, exercise_type = typeName, duration_min = minutes, energy_kcal_total = total, notes = notes)
    }

    suspend fun updateExercise(id: Long, typeName: String, kcalPerHour: Double, minutes: Double, notes: String?) {
        require(minutes >= 0.0) { "minutes must be >= 0" }
        val total = (kcalPerHour / 60.0) * minutes
        q.updateExercise(exercise_type = typeName, duration_min = minutes, energy_kcal_total = total, notes = notes, id = id)
    }

    suspend fun logAutomaticSteps(steps: Int) {
        val minutes = if (steps > 0) steps / 100.0 else 0.0
        val timestamp = currentDateIso() + "T12:00:00"
        val existing = getByDateRange(timestamp, timestamp).firstOrNull { it.exercise_type == AUTOMATIC_STEPS_KEY }
        if (existing == null) {
            logExercise(typeName = AUTOMATIC_STEPS_KEY, kcalPerHour = AUTOMATIC_STEPS_KCAL_PER_HOUR, minutes = minutes, timestamp = timestamp)
        } else {
            updateExercise(id = existing.id, typeName = AUTOMATIC_STEPS_KEY, kcalPerHour = AUTOMATIC_STEPS_KCAL_PER_HOUR, minutes = minutes, notes = existing.notes)
        }
    }

    suspend fun deleteById(id: Long) = q.deleteExerciseById(id)

    suspend fun getByDateRange(startIso: String, endIso: String): List<Exercise> =
        q.selectExerciseByDateRange(startIso, endIso).awaitAsList()

    suspend fun sumKcalByDate(startIso: String, endIso: String): Double =
        getByDateRange(startIso, endIso).sumOf { it.energy_kcal_total }

    suspend fun getDailyBurned(startIso: String, endIso: String) =
        q.statsDailyBurned(startIso, endIso).awaitAsList()
}
