package com.emilflach.lokcal.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.emilflach.lokcal.Database

expect class SqlDriverFactory {
    suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver
}

suspend fun createDatabase(sqlDriverFactory: SqlDriverFactory, onProgress: ((Float) -> Unit)? = null): Database {
    val driver = sqlDriverFactory.createDriver(schema = Database.Schema)
    val database = Database(driver)
    DatabaseSeeder.seedIfNeeded(database, onProgress)
    DatabaseSeeder.seedExerciseTypesIfNeeded(database)
    return database
}
