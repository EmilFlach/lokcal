package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Exercise
import com.emilflach.lokcal.util.currentDateIso

class ExerciseRepository(private val db: Database) {
    private val q get() = db.exerciseQueries

    enum class Type(val dbName: String, val kcalPerHour: Double) {
        AUTOMATIC_STEPS("AUTOMATIC_STEPS", 220.0),
        WALKING("WALKING", 200.0),
        RUNNING("RUNNING", 740.0);

        companion object {
            fun fromDb(name: String): Type = entries.first { it.dbName == name }
        }
    }

    fun logExercise(type: Type, minutes: Double, timestamp: String, notes: String? = null) {
        require(minutes >= 0.0) { "minutes must be >= 0" }
        val kcalPerMinute = type.kcalPerHour / 60.0
        val total = kcalPerMinute * minutes
        q.logExercise(timestamp = timestamp, exercise_type = type.dbName, duration_min = minutes, energy_kcal_total = total, notes = notes)
    }

    fun updateExercise(id: Long, type: Type, minutes: Double, notes: String?) {
        require(minutes >= 0.0) { "minutes must be >= 0" }
        val kcalPerMinute = type.kcalPerHour / 60.0
        val total = kcalPerMinute * minutes
        q.updateExercise(exercise_type = type.dbName, duration_min = minutes, energy_kcal_total = total, notes = notes, id = id)
    }

    fun logAutomaticSteps(steps: Int) {
        val minutes = if(steps > 0) steps / 100.0 else 0.0
        val timestamp = currentDateIso() + "T12:00:00"
        val type = Type.AUTOMATIC_STEPS
        val existing = getByDateRange(timestamp, timestamp).firstOrNull { it.exercise_type == type.dbName }
        if (existing == null) {
            logExercise(type = type, minutes = minutes, timestamp = timestamp, notes = null)
        } else {
            updateExercise(id = existing.id, type = type, minutes = minutes, notes = existing.notes)
        }
    }

    fun deleteById(id: Long) = q.deleteExerciseById(id)

    fun getById(id: Long): Exercise? = try { q.selectExerciseById(id).executeAsOne() } catch (_: Throwable) { null }

    fun getByDateRange(startIso: String, endIso: String): List<Exercise> =
        q.selectExerciseByDateRange(startIso, endIso).executeAsList()

    fun sumKcalByDate(startIso: String, endIso: String): Double =
        getByDateRange(startIso, endIso).sumOf { it.energy_kcal_total }
}
