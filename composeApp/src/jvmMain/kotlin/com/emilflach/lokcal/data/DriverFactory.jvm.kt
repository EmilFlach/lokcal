package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.*

actual class SqlDriverFactory {
    actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:lokcal.db", Properties(), schema.synchronous())
        return driver
    }
}