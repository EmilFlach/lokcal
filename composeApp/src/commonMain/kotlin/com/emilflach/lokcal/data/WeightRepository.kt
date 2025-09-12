package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.WeightLog
import com.emilflach.lokcal.util.currentDateIso

class WeightRepository(database: Database) {
    private val q = database.weightQueries

    fun getAll(): List<WeightLog> = q.selectAllDesc().executeAsList()

    fun setForToday(kg: Double) {
        val date = currentDateIso()
        q.insertOrReplace(date, kg)
    }

    fun deleteById(id: Long) {
        q.deleteById(id)
    }

    fun getForDate(dateIso: String): WeightLog? = q.selectByDate(dateIso).executeAsOneOrNull()
}
