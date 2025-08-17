package com.emilflach.lokcal.data

import com.emilflach.lokcal.Database
import com.emilflach.lokcal.Food

class FoodRepository(private val database: Database) {
    private val queries = database.foodQueries

    fun getAll(): List<Food> = queries.selectAll().executeAsList()

    fun insert(name: String, description: String?) {
        queries.insert(name = name, description = description)
    }
}
