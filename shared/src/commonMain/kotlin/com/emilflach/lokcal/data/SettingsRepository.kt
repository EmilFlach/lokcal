package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database

/**
 * Simple settings repository backed by the Meta table.
 */
class SettingsRepository(database: Database) {
    private val meta = database.metaQueries
    private val sourcePref = database.sourcePreferenceQueries

    companion object {
        private const val KEY_STARTING_KCAL = "starting_kcal"
        private const val DEFAULT_STARTING_KCAL = 1690.0
    }

    suspend fun getStartingKcal(): Double {
        val v = try { meta.getMeta(KEY_STARTING_KCAL).awaitAsOneOrNull() } catch (_: Throwable) { null }
        return v?.toDoubleOrNull() ?: DEFAULT_STARTING_KCAL
    }

    suspend fun setStartingKcal(value: Double) {
        require(value > 0.0) { "Starting kcal must be > 0" }
        try {
            meta.setMeta(KEY_STARTING_KCAL, value.toString())
        } catch (_: Throwable) {
            // ignore
        }
    }

    // Source preference methods
    suspend fun getSourcePreferences(): List<String> {
        return try {
            val prefs = sourcePref.getPreferences().awaitAsList().map { it.source_id }
            prefs
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun setSourcePreference(order: Long, sourceId: String) {
        require(order in 1..2) { "Preference order must be 1 or 2" }
        try {
            sourcePref.setPreference(order, sourceId)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    suspend fun clearSourcePreferences() {
        try {
            sourcePref.clearAllPreferences()
        } catch (_: Throwable) {
            // ignore
        }
    }
}
