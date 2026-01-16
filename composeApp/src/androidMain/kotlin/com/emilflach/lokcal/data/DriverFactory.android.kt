package com.emilflach.lokcal.data

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class SqlDriverFactory(private val context: Context) {
    actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver {
        // Provide Android asset-based JSON loader for initial seeding
        IngredientSeeder.provideJsonText = {
            try {
                context.assets.open("ingredients.json").bufferedReader().use { it.readText() }
            } catch (_: Throwable) {
                null
            }
        }
        return AndroidSqliteDriver(schema.synchronous(), context, "lokcal.db")
    }
}