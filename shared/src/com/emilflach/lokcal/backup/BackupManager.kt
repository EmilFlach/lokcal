package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker

object BackupManager {
    suspend fun exportDatabase() = copyDatabase()

    suspend fun importDatabase(): Boolean {
        val file = FileKit.openFilePicker(
            type = FileKitType.File(extensions = listOf("db"))
        ) ?: return false

        return replaceDatabase(file)
    }

    suspend fun setBackupDirectory() = chooseBackupDirectory()
    suspend fun getBackupDirectory() = retrieveBackupDirectory()

    fun showNightlyBackupSettings() = allowNightlyBackup()
    suspend fun getNightlyBackup() = isNightlyBackupEnabled()
    fun setNightlyBackup(value: Boolean) = enableNightlyBackup(value)
}

expect suspend fun chooseBackupDirectory(): PlatformFile?

expect suspend fun retrieveBackupDirectory(): String?

expect suspend fun replaceDatabase(file: PlatformFile): Boolean

expect suspend fun copyDatabase(): Boolean
expect suspend fun exportDatabaseToBackupDirectory(): Boolean

expect fun allowNightlyBackup(): Boolean

expect fun enableNightlyBackup(value: Boolean): Boolean

expect suspend fun isNightlyBackupEnabled(): Boolean