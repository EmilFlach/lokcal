package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.ExerciseType

class ExerciseTypeRepository(private val db: Database) {
    private val q get() = db.exerciseTypeQueries

    suspend fun getAll(): List<ExerciseType> =
        q.selectAllExerciseTypes().awaitAsList()

    suspend fun getById(id: Long): ExerciseType? =
        q.selectExerciseTypeById(id).awaitAsOneOrNull()

    suspend fun insert(name: String, kcalPerHour: Double, sortOrder: Int = 0, imageUrl: String? = null): Long {
        q.insertExerciseType(name = name, kcal_per_hour = kcalPerHour, sort_order = sortOrder.toLong(), image_url = imageUrl)
        return q.lastInsertedId().awaitAsOne()
    }

    suspend fun insertManual(name: String, kcalPerHour: Double, imageUrl: String? = null): Long {
        return q.transactionWithResult {
            q.insertManual(name = name, kcal_per_hour = kcalPerHour, image_url = imageUrl)
            q.lastInsertedId().awaitAsOne()
        }
    }

    suspend fun update(id: Long, name: String, kcalPerHour: Double, sortOrder: Int = 0, imageUrl: String? = null) {
        q.updateExerciseType(name = name, kcal_per_hour = kcalPerHour, sort_order = sortOrder.toLong(), image_url = imageUrl, id = id)
    }

    suspend fun delete(id: Long) {
        q.deleteExerciseType(id)
    }
}
