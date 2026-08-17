package com.emilflach.lokcal.plugins.secrets

import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

/**
 * The secrets to generate, as `constant name` -> (`local.properties key`, `env var`).
 * Mirrors the map the Gradle build passed to `GenerateSecretsTask` for `KrogerConfig`.
 */
private val SECRETS = listOf(
    Secret(constant = "CLIENT_ID", propertyKey = "kroger.clientId", envVar = "KROGER_CLIENT_ID"),
    Secret(constant = "CLIENT_SECRET", propertyKey = "kroger.clientSecret", envVar = "KROGER_CLIENT_SECRET"),
)

private data class Secret(val constant: String, val propertyKey: String, val envVar: String)

/**
 * Generates `<packageName>.<objectName>` containing one `const val` per [SECRETS] entry.
 *
 * Hidden inputs (env vars, the untracked `local.properties`) cannot be fingerprinted, so
 * execution avoidance is disabled. The task still preserves the output timestamp when the
 * resolved values are unchanged, allowing downstream compilation and linking to remain cached.
 *
 * `local.properties` lives at the *project* root, one level above the consumer module, so it
 * is read from [moduleRootDir]`.parent`. Env vars take precedence (the CI secret source).
 */
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun generateSecrets(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    @Output outputDir: Path,
    settings: Settings,
) {
    val localProps = Properties().apply {
        val f = moduleRootDir.parent?.resolve("local.properties")
        if (f != null && f.exists()) f.inputStream().use { load(it) }
    }

    fun resolve(secret: Secret): String =
        System.getenv(secret.envVar)?.takeIf { it.isNotBlank() }
            ?: localProps.getProperty(secret.propertyKey, "")

    val constants = SECRETS.joinToString("\n") { """    const val ${it.constant} = "${resolve(it)}"""" }

    val generatedSource =
        """
        |package ${settings.packageName}
        |
        |internal object ${settings.objectName} {
        |$constants
        |}
        |
        """.trimMargin()

    val file = outputDir.resolve("${settings.objectName}.kt")
    if (!file.exists() || file.readText() != generatedSource) {
        file.createParentDirectories()
        file.writeText(generatedSource)
    }
}
