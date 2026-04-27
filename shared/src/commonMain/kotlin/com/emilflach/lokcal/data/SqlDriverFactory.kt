package com.emilflach.lokcal.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.emilflach.lokcal.Database

expect class SqlDriverFactory {
    suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver
}

private suspend fun tryExec(driver: SqlDriver, sql: String) {
    try {
        driver.execute(null, sql, 0).await()
    } catch (_: Throwable) { }
}

private suspend fun ensureMetaTable(driver: SqlDriver) {
    tryExec(driver, "CREATE TABLE IF NOT EXISTS Meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
}

private suspend fun getSchemaVersion(driver: SqlDriver): Int {
    return try {
        driver.executeQuery(
            identifier = null,
            sql = "SELECT value FROM Meta WHERE key = 'schema_version'",
            mapper = { cursor ->
                val hasRow = cursor.next()
                QueryResult.AsyncValue {
                    if (hasRow.await()) cursor.getString(0)?.toIntOrNull() ?: 0 else 0
                }
            },
            parameters = 0,
            binders = null
        ).await()
    } catch (_: Throwable) {
        0
    }
}

private suspend fun setSchemaVersion(driver: SqlDriver, version: Int) {
    tryExec(driver, "INSERT OR REPLACE INTO Meta(key, value) VALUES ('schema_version', '$version')")
}

suspend fun createDatabase(sqlDriverFactory: SqlDriverFactory, onProgress: ((Float) -> Unit)? = null): Database {
    val driver = sqlDriverFactory.createDriver(schema = Database.Schema)
    ensureMetaTable(driver)

    // Keep a sample for future migration mechanisms
    if (getSchemaVersion(driver) == 0) {
        setSchemaVersion(driver, 1)
    }

    val database = Database(driver)
    DatabaseSeeder.seedIfNeeded(database, onProgress)
    DatabaseSeeder.seedExerciseTypesIfNeeded(database)
    return database
}
