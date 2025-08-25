package com.emilflach.lokcal.data

import app.cash.sqldelight.db.SqlDriver
import com.emilflach.lokcal.Database

expect class SqlDriverFactory {
    fun createDriver(): SqlDriver

}

private fun ensureMetaTable(driver: SqlDriver) {
    try {
        driver.execute(null, "CREATE TABLE IF NOT EXISTS Meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)", 0)
    } catch (_: Throwable) {
        // ignore
    }
}

private fun tryExec(driver: SqlDriver, sql: String) {
    try {
        driver.execute(null, sql, 0)
    } catch (_: Throwable) {
        // Ignore errors (e.g., column already exists or table missing on fresh DB)
    }
}

private fun ensureFoodSchemaUpgrades(driver: SqlDriver) {
    // Add newly introduced columns to existing Food table (best-effort, safe if already present)
    tryExec(driver, "ALTER TABLE Food ADD COLUMN brand TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN category TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN energy_kcal_per_100g REAL NOT NULL DEFAULT 0")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN unit TEXT NOT NULL DEFAULT 'g'")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN external_id TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN plural_name TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN english_name TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN dutch_name TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN brand_name TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN serving_size TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN gtin13 TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN image_url TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN product_url TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN source TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN label_id TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN created_at_source TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN updated_at_source TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN on_hand INTEGER NOT NULL DEFAULT 0")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN raw_json TEXT")
    tryExec(driver, "ALTER TABLE Food ADD COLUMN created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP")

    // Ensure FoodAlias table exists (used for aliases in seed)
    tryExec(
        driver,
        """
        CREATE TABLE IF NOT EXISTS FoodAlias (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            food_id INTEGER NOT NULL REFERENCES Food(id) ON DELETE CASCADE,
            alias TEXT NOT NULL
        )
        """.trimIndent()
    )
}

private fun ensureMealSchemaUpgrades(driver: SqlDriver) {
    // Ensure Meal table exists (with new column) and add missing column on older DBs
    tryExec(
        driver,
        """
        CREATE TABLE IF NOT EXISTS Meal (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            description TEXT,
            image_url TEXT,
            total_portions REAL NOT NULL DEFAULT 1.0,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
        """.trimIndent()
    )

    // Add the new columns if the table already existed without them
    tryExec(driver, "ALTER TABLE Meal ADD COLUMN total_portions REAL NOT NULL DEFAULT 1.0")
    tryExec(driver, "ALTER TABLE Meal ADD COLUMN image_url TEXT")

    // Ensure MealItem table exists
    tryExec(
        driver,
        """
        CREATE TABLE IF NOT EXISTS MealItem (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            meal_id INTEGER NOT NULL REFERENCES Meal(id) ON DELETE CASCADE,
            food_id INTEGER NOT NULL REFERENCES Food(id) ON DELETE RESTRICT,
            quantity_g REAL NOT NULL CHECK (quantity_g >= 0)
        )
        """.trimIndent()
    )
}

private fun ensureExerciseSchemaUpgrades(driver: SqlDriver) {
    // Ensure Exercise table exists
    tryExec(
        driver,
        """
        CREATE TABLE IF NOT EXISTS Exercise (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            exercise_type TEXT NOT NULL CHECK (exercise_type IN ('WALKING','RUNNING')),
            duration_min REAL NOT NULL CHECK (duration_min >= 0),
            energy_kcal_total REAL NOT NULL,
            notes TEXT
        )
        """.trimIndent()
    )
}

fun createDatabase(sqlDriverFactory: SqlDriverFactory): Database {
    val driver = sqlDriverFactory.createDriver()

    // Ensure Meta table exists for older databases without migrations
    ensureMetaTable(driver)

    // Best-effort runtime schema upgrades for existing installs without SQLDelight migrations
    ensureFoodSchemaUpgrades(driver)
    ensureMealSchemaUpgrades(driver)
    ensureExerciseSchemaUpgrades(driver)

    val database = Database(driver)
    // Seed initial data on first launch
    IngredientSeeder.seedIfNeeded(database)
    return database
}

