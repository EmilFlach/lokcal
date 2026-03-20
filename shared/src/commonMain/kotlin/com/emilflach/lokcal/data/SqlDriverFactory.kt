package com.emilflach.lokcal.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.emilflach.lokcal.Database

expect class SqlDriverFactory {
    suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver

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
    // Check if the first new column exists. If it does, we assume all are already added.
    try {
        driver.execute(null, "SELECT brand FROM Food LIMIT 0", 0)
        return // Column exists, skip upgrades
    } catch (_: Throwable) {
        // Column doesn't exist, proceed with upgrades
    }

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

    // Check if the new columns exist.
    try {
        driver.execute(null, "SELECT total_portions FROM Meal LIMIT 0", 0)
    } catch (_: Throwable) {
        // Add the new columns if the table already existed without them
        tryExec(driver, "ALTER TABLE Meal ADD COLUMN total_portions REAL NOT NULL DEFAULT 1.0")
        tryExec(driver, "ALTER TABLE Meal ADD COLUMN image_url TEXT")
    }

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
    // First check if the table exists and if it needs updating
    try {
        // Try to insert a dummy record with AUTOMATIC_STEPS to test if the constraint allows it
        driver.execute(
            null,
            "INSERT INTO Exercise(id, timestamp, exercise_type, duration_min, energy_kcal_total, notes) VALUES (NULL, '2000-01-01', 'AUTOMATIC_STEPS', 0, 0, NULL)",
            0
        )
        // If we get here, the constraint is already correct, delete the test record

        driver.execute(
            null,
            "DELETE FROM Exercise WHERE timestamp = '2000-01-01' AND exercise_type = 'AUTOMATIC_STEPS' AND duration_min = 0",
            0
        )
    } catch (_: Exception) {
        // Constraint error or table doesn't exist, need to create or update the table

        // Create a new table with the correct constraint
        tryExec(
            driver,
            """
            CREATE TABLE IF NOT EXISTS Exercise_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                exercise_type TEXT NOT NULL CHECK (exercise_type IN ('WALKING','RUNNING','AUTOMATIC_STEPS')),
                duration_min REAL NOT NULL CHECK (duration_min >= 0),
                energy_kcal_total REAL NOT NULL,
                notes TEXT
            )
            """.trimIndent()
        )

        // Try to copy data from the old table if it exists
        tryExec(
            driver,
            """
            INSERT INTO Exercise_new
            SELECT id, timestamp, exercise_type, duration_min, energy_kcal_total, notes
            FROM Exercise
            WHERE exercise_type IN ('WALKING', 'RUNNING')
            """
        )

        // Drop the old table
        tryExec(driver, "DROP TABLE IF EXISTS Exercise")

        // Rename the new table to the original name
        tryExec(driver, "ALTER TABLE Exercise_new RENAME TO Exercise")
    }
}


private fun ensureWeightSchemaUpgrades(driver: SqlDriver) {
    tryExec(
        driver,
        """
        CREATE TABLE IF NOT EXISTS WeightLog (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            date TEXT NOT NULL UNIQUE,
            weight_kg REAL NOT NULL CHECK (weight_kg > 0)
        )
        """.trimIndent()
    )
}

private fun ensureIntakeSchemaUpgrades(driver: SqlDriver) {
    try {
        driver.execute(null, "SELECT leftover FROM Intake LIMIT 0", 0)
    } catch (_: Throwable) {
        tryExec(driver, "ALTER TABLE Intake ADD COLUMN leftover INTEGER NOT NULL DEFAULT 0")
    }
}

private fun getSchemaVersion(driver: SqlDriver): Int {
    return try {
        var version = 0
        driver.executeQuery(
            identifier = null,
            sql = "SELECT value FROM Meta WHERE key = 'schema_version'",
            mapper = { cursor ->
                if (cursor.next().value) {
                    version = cursor.getString(0)?.toIntOrNull() ?: 0
                }
                QueryResult.Value(Unit)
            },
            parameters = 0,
            binders = null
        )
        version
    } catch (_: Throwable) {
        0
    }
}

private fun setSchemaVersion(driver: SqlDriver, version: Int) {
    tryExec(driver, "INSERT OR REPLACE INTO Meta(key, value) VALUES ('schema_version', '$version')")
}

private fun migrateToV5(driver: SqlDriver) {
    println("[Migration] Starting V5 migration: Simplify Food table, enhance FoodAlias, remove enum constraints")

    // Step 1: Create new FoodAlias table with alias_type
    tryExec(driver, """
        CREATE TABLE IF NOT EXISTS FoodAlias_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            food_id INTEGER NOT NULL REFERENCES Food(id) ON DELETE CASCADE,
            alias TEXT NOT NULL,
            alias_type TEXT NOT NULL,
            UNIQUE(food_id, alias, alias_type)
        )
    """)

    // Step 2: Migrate existing aliases (if any)
    tryExec(driver, """
        INSERT OR IGNORE INTO FoodAlias_new (food_id, alias, alias_type)
        SELECT food_id, alias, 'name' FROM FoodAlias
    """)

    // Step 3: Migrate brand_name, english_name, dutch_name to aliases
    tryExec(driver, """
        INSERT OR IGNORE INTO FoodAlias_new (food_id, alias, alias_type)
        SELECT id, brand_name, 'brand'
        FROM Food
        WHERE brand_name IS NOT NULL AND brand_name != ''
    """)

    tryExec(driver, """
        INSERT OR IGNORE INTO FoodAlias_new (food_id, alias, alias_type)
        SELECT id, english_name, 'locale:en'
        FROM Food
        WHERE english_name IS NOT NULL AND english_name != ''
    """)

    tryExec(driver, """
        INSERT OR IGNORE INTO FoodAlias_new (food_id, alias, alias_type)
        SELECT id, dutch_name, 'locale:nl'
        FROM Food
        WHERE dutch_name IS NOT NULL AND dutch_name != ''
    """)

    // Step 4: Create new simplified Food table
    tryExec(driver, """
        CREATE TABLE Food_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            energy_kcal_per_100g REAL NOT NULL DEFAULT 0,
            unit TEXT NOT NULL DEFAULT 'g',
            serving_size TEXT,
            gtin13 TEXT,
            image_url TEXT,
            product_url TEXT,
            source TEXT,
            created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
        )
    """)

    // Step 5: Copy data (only 10 fields)
    tryExec(driver, """
        INSERT INTO Food_new (id, name, energy_kcal_per_100g, unit, serving_size,
                              gtin13, image_url, product_url, source, created_at)
        SELECT id, name, energy_kcal_per_100g, unit, serving_size,
               gtin13, image_url, product_url, source, created_at
        FROM Food
    """)

    // Step 6: Replace tables
    tryExec(driver, "DROP TABLE IF EXISTS Food")
    tryExec(driver, "ALTER TABLE Food_new RENAME TO Food")

    tryExec(driver, "DROP TABLE IF EXISTS FoodAlias")
    tryExec(driver, "ALTER TABLE FoodAlias_new RENAME TO FoodAlias")

    // Step 7: Create indexes
    tryExec(driver, "CREATE INDEX IF NOT EXISTS food_gtin13_idx ON Food(gtin13)")
    tryExec(driver, "CREATE INDEX IF NOT EXISTS food_source_idx ON Food(source)")
    tryExec(driver, "CREATE INDEX IF NOT EXISTS food_alias_text_idx ON FoodAlias(LOWER(alias))")
    tryExec(driver, "CREATE INDEX IF NOT EXISTS food_alias_type_idx ON FoodAlias(alias_type)")
    tryExec(driver, "CREATE INDEX IF NOT EXISTS food_alias_food_id_idx ON FoodAlias(food_id)")

    // Step 8: Recreate Intake table without meal_type CHECK constraint
    tryExec(driver, """
        CREATE TABLE Intake_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            meal_type TEXT NOT NULL,
            source_type TEXT NOT NULL CHECK (source_type IN ('FOOD','MEAL')),
            source_food_id INTEGER REFERENCES Food(id) ON DELETE SET NULL,
            source_meal_id INTEGER REFERENCES Meal(id) ON DELETE SET NULL,
            quantity_g REAL NOT NULL CHECK (quantity_g >= 0),
            item_name TEXT NOT NULL,
            energy_kcal_total REAL NOT NULL,
            notes TEXT,
            leftover INTEGER NOT NULL DEFAULT 0,
            CHECK (
                (source_type = 'FOOD' AND source_food_id IS NOT NULL AND source_meal_id IS NULL) OR
                (source_type = 'MEAL' AND source_meal_id IS NOT NULL AND source_food_id IS NULL)
            )
        )
    """)

    tryExec(driver, "INSERT INTO Intake_new SELECT * FROM Intake")
    tryExec(driver, "DROP TABLE IF EXISTS Intake")
    tryExec(driver, "ALTER TABLE Intake_new RENAME TO Intake")

    // Step 9: Recreate Exercise table without exercise_type CHECK constraint
    tryExec(driver, """
        CREATE TABLE Exercise_new (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp TEXT NOT NULL,
            exercise_type TEXT NOT NULL,
            duration_min REAL NOT NULL CHECK (duration_min >= 0),
            energy_kcal_total REAL NOT NULL,
            notes TEXT
        )
    """)

    tryExec(driver, "INSERT INTO Exercise_new SELECT * FROM Exercise")
    tryExec(driver, "DROP TABLE IF EXISTS Exercise")
    tryExec(driver, "ALTER TABLE Exercise_new RENAME TO Exercise")

    println("[Migration] V5 migration completed successfully")
}

suspend fun createDatabase(sqlDriverFactory: SqlDriverFactory, onProgress: ((Float) -> Unit)? = null): Database {
    val driver = sqlDriverFactory.createDriver(schema = Database.Schema)

    // Ensure Meta table exists for older databases without migrations
    ensureMetaTable(driver)

    // Check schema version and run migration if needed
    val currentVersion = getSchemaVersion(driver)
    if (currentVersion < 5) {
        // Run old migrations first if database exists
        if (currentVersion == 0) {
            // Best-effort runtime schema upgrades for existing installs without SQLDelight migrations
            ensureFoodSchemaUpgrades(driver)
            ensureMealSchemaUpgrades(driver)
            ensureExerciseSchemaUpgrades(driver)
            ensureWeightSchemaUpgrades(driver)
            ensureIntakeSchemaUpgrades(driver)
        }

        // Run V5 migration
        migrateToV5(driver)
        setSchemaVersion(driver, 5)
    }

    val database = Database(driver)
    // Seed initial data on first launch
    IngredientSeeder.seedIfNeeded(database, onProgress)
    return database
}

