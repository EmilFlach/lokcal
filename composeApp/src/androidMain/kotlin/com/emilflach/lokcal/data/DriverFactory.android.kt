package com.emilflach.lokcal.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.emilflach.lokcal.Database

actual class SqlDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        // Provide Android asset-based JSON loader for initial seeding
        IngredientSeeder.provideJsonText = {
            try {
                context.assets.open("ingredients.json").bufferedReader().use { it.readText() }
            } catch (_: Throwable) {
                null
            }
        }
        return AndroidSqliteDriver(Database.Schema, context, "lokcal.db")
    }
}