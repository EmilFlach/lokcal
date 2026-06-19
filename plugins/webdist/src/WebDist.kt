package com.emilflach.lokcal.plugins.webdist

import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Runs a script from the given working dir, streaming its output, failing the task
 * on a non-zero exit. The web bundle is a side effect (and `runWeb` blocks), so these
 * tasks have no `@Output` and disable execution avoidance — they always run on demand.
 */
private fun runScript(workingDir: Path, vararg command: String) {
    val exit = ProcessBuilder(*command)
        .directory(workingDir.toFile())
        .inheritIO()
        .start()
        .waitFor()
    if (exit != 0) error("webdist: `${command.joinToString(" ")}` failed with exit code $exit")
}

// moduleRootDir is <repo>/webApp; the scripts live alongside it at webApp/scripts/
// (each script resolves the repo root itself, so CWD only needs to make the path valid).
@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun assembleWeb(@Input(inferTaskDependency = false) moduleRootDir: Path) {
    runScript(moduleRootDir, "bash", "scripts/assemble-web.sh")
}

@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun runWeb(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: WebDistSettings,
) {
    runScript(moduleRootDir, "bash", "scripts/run-web.sh", settings.port.toString())
}
