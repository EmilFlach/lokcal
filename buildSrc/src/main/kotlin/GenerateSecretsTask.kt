import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateSecretsTask : DefaultTask() {
    @get:Input abstract val packageName: Property<String>
    @get:Input abstract val objectName: Property<String>
    @get:Input abstract val secrets: MapProperty<String, String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val constants = secrets.get().entries
            .joinToString("\n") { (key, value) -> "    const val $key = \"$value\"" }
        outputDir.get().asFile.also { it.mkdirs() }
            .resolve("${objectName.get()}.kt")
            .writeText(
                """
                |package ${packageName.get()}
                |
                |internal object ${objectName.get()} {
                |$constants
                |}
                """.trimMargin()
            )
    }
}
