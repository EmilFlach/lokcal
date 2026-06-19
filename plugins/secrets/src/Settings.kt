package com.emilflach.lokcal.plugins.secrets

import org.jetbrains.amper.plugins.Configurable

/**
 * Configuration for the secrets-generation plugin.
 *
 * The generated file is `<packageName>.<objectName>` with one `const val` per secret.
 * Values are resolved at build time from environment variables first, then from
 * `local.properties` at the project root; missing values fall back to the empty string
 * (mirroring the original Gradle `GenerateSecretsTask` behaviour).
 */
@Configurable
interface Settings {
    val packageName: String get() = "com.emilflach.lokcal.data"
    val objectName: String get() = "KrogerConfig"
}
