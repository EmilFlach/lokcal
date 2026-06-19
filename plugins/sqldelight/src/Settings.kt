package sqldelight.amper

import org.jetbrains.amper.plugins.Configurable

@Configurable
interface SqlDelightSettings {
    val packageName: String
    val databaseName: String get() = "AppDatabase"

    /**
     * Generate suspending (async) query/transacter APIs. Must be `true` for the
     * web-worker JS driver. Lokcal sets this (Gradle: `generateAsync.set(true)`).
     */
    val generateAsync: Boolean get() = false
}
