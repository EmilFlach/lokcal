package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.util.currentDateIso

class WeightRepository(database: Database) {
    private val q = database.weightQueries

    suspend fun getAll(): List<WeightLog> = q.selectAllDesc().awaitAsList()

    suspend fun setForToday(kg: Double) {
        val date = currentDateIso()
        q.insertOrReplace(date, kg)
    }

    suspend fun deleteById(id: Long) {
        q.deleteById(id)
    }

    suspend fun getForDate(dateIso: String): WeightLog? = q.selectByDate(dateIso).awaitAsOneOrNull()
}
