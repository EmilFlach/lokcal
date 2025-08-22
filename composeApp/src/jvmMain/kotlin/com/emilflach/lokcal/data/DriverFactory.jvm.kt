package com.emilflach.lokcal.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties
import com.emilflach.lokcal.Database

actual class SqlDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:lokcal.db", Properties(), Database.Schema)
        return driver
    }
}