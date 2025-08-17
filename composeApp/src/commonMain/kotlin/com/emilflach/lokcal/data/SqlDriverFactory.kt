package com.emilflach.lokcal.data

import app.cash.sqldelight.db.SqlDriver
import com.emilflach.lokcal.Database

expect class SqlDriverFactory {
    fun createDriver(): SqlDriver

}

fun createDatabase(sqlDriverFactory: SqlDriverFactory): Database {
    val driver = sqlDriverFactory.createDriver()
    val database = Database(driver)
    return database
}

