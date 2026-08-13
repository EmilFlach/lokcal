package sqldelight.amper

import app.cash.sqldelight.core.SqlDelightEnvironment
import app.cash.sqldelight.core.SqlDelightEnvironment.CompilationStatus
import app.cash.sqldelight.dialect.api.SqlDelightDialect
import app.cash.sqldelight.gradle.SqlDelightCompilationUnitImpl
import app.cash.sqldelight.gradle.SqlDelightDatabasePropertiesImpl
import app.cash.sqldelight.gradle.SqlDelightSourceFolderImpl
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.*

private const val INPUT_FINGERPRINT_FILE = ".sqldelight-inputs.sha256"

@OptIn(ExperimentalPathApi::class)
@TaskAction
fun generateSqlDelight(
    @Input sqlSourceDir: Path,
    @Output generatedSourceDir: Path,
    packageName: String,
    databaseName: String,
    generateAsync: Boolean,
) {
    if (!sqlSourceDir.isDirectory()) {
        println("SQLDelight: source directory $sqlSourceDir does not exist, skipping")
        return
    }

    val fingerprint = inputFingerprint(sqlSourceDir)
    val fingerprintFile = generatedSourceDir.resolve(INPUT_FINGERPRINT_FILE)
    val generatedSourcesExist = generatedSourceDir.isDirectory() &&
        generatedSourceDir.walk().any { it.isRegularFile() && it.name.endsWith(".kt") }
    if (generatedSourcesExist && fingerprintFile.isRegularFile() && fingerprintFile.readText() == fingerprint) {
        return
    }

    generatedSourceDir.deleteRecursively()

    val dialect = ServiceLoader
        .load(SqlDelightDialect::class.java, SqlDelightDialect::class.java.classLoader)
        .first()

    val sourceFolder = SqlDelightSourceFolderImpl(sqlSourceDir.toFile(), false)
    val compilationUnit = SqlDelightCompilationUnitImpl(
        name = "commonMain",
        sourceFolders = setOf(sourceFolder),
        outputDirectoryFile = generatedSourceDir.toFile(),
    )
    val properties = SqlDelightDatabasePropertiesImpl(
        packageName = packageName,
        compilationUnits = listOf(compilationUnit),
        className = databaseName,
        dependencies = emptyList(),
        generateAsync = generateAsync,
        rootDirectory = sqlSourceDir.toFile(),
    )

    val environment = SqlDelightEnvironment(
        properties = properties,
        compilationUnit = compilationUnit,
        verifyMigrations = false,
        dialect = dialect,
        moduleName = databaseName,
    )

    when (val status = environment.generateSqlDelightFiles { message ->
        println("[SQLDelight] $message")
    }) {
        is CompilationStatus.Success -> {
            println("SQLDelight: code generation successful")
            fixImplPackageConflict(generatedSourceDir, packageName, databaseName)
            fingerprintFile.writeText(fingerprint)
        }
        is CompilationStatus.Failure -> error(
            "SQLDelight: code generation failed:\n${status.errors.joinToString("\n")}"
        )
    }
}

@OptIn(ExperimentalPathApi::class)
private fun inputFingerprint(
    sqlSourceDir: Path,
): String {
    val digest = MessageDigest.getInstance("SHA-256")

    fun update(value: ByteArray) {
        digest.update(value.size.toString().toByteArray())
        digest.update(0.toByte())
        digest.update(value)
    }

    sqlSourceDir.walk()
        .filter { it.isRegularFile() && it.name.endsWith(".sq") }
        .sortedBy { sqlSourceDir.relativize(it).toString() }
        .forEach { file ->
            update(sqlSourceDir.relativize(file).toString().toByteArray())
            update(file.readBytes())
        }

    return digest.digest().joinToString("") { "%02x".format(it) }
}

// SQLDelight puts the impl class in package `$packageName.$databaseName`, which conflicts with
// the `$databaseName` interface in `$packageName` (Kotlin forbids a package and class sharing
// the same FQN). Rewrite the conflicting package/import references to `$packageName.internal`.
@OptIn(ExperimentalPathApi::class)
private fun fixImplPackageConflict(generatedSourceDir: Path, packageName: String, databaseName: String) {
    val conflictingPkg = "$packageName.$databaseName"
    val replacementPkg = "$packageName.internal"

    generatedSourceDir.walk().filter { it.toString().endsWith(".kt") }.forEach { file ->
        var content = file.readText()
        var changed = false

        if (content.contains("package $conflictingPkg")) {
            content = content.replace("package $conflictingPkg", "package $replacementPkg")
            changed = true
        }
        // Match sub-package imports like `import $conflictingPkg.newInstance` but NOT
        // the class import `import $conflictingPkg` (no trailing dot).
        if (content.contains("import $conflictingPkg.")) {
            content = content.replace("import $conflictingPkg.", "import $replacementPkg.")
            changed = true
        }

        if (changed) file.writeText(content)
    }
}
