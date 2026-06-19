package com.emilflach.lokcal.plugins.webdist

import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.Path

/**
 * Runs a repo script, streaming its output, failing the task on a non-zero exit.
 * The web bundle is a side effect (and `serveWeb` blocks), so these tasks have no
 * `@Output` and disable execution avoidance — they always run on demand.
 */
private fun runScript(repoRoot: Path, vararg command: String) {
    val exit = ProcessBuilder(*command)
        .directory(repoRoot.toFile())
        .inheritIO()
        .start()
        .waitFor()
    if (exit != 0) error("webdist: `${command.joinToString(" ")}` failed with exit code $exit")
}

@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun assembleWeb(@Input(inferTaskDependency = false) moduleRootDir: Path) {
    // moduleRootDir is <repo>/webApp; the scripts live at <repo>/scripts.
    runScript(moduleRootDir.parent, "bash", "scripts/assemble-web.sh")
}

@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun serveWeb(
    @Input(inferTaskDependency = false) moduleRootDir: Path,
    settings: WebDistSettings,
) {
    runScript(moduleRootDir.parent, "bash", "scripts/serve-web.sh", settings.port.toString())
}
