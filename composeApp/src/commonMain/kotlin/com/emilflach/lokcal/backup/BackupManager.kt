package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker

class BackupManager {

    // Runs a backup immediately. Returns true if a backup file was written.
    suspend fun exportDatabase(): Boolean {
        return copyDatabase()
    }

    // Restores from the latest available backup file in the fixed folder. Returns true if restored.
    suspend fun importDatabase(): Boolean {
        val file = FileKit.openFilePicker(
            type = FileKitType.File(extensions = listOf("db"))
        )
        return if (file != null) {
            replaceDatabase(file)
        } else {
            false
        }
    }

    // Ensure background scheduling is aligned with current settings (call on app start on supported platforms).
    fun getNightlyBackup() {

    }

    // Enables/disables nightly backups and (re)schedules as needed on supported platforms.
    fun setNightlyBackup(value: Boolean) {

    }

}

expect suspend fun replaceDatabase(file: PlatformFile): Boolean

expect suspend fun copyDatabase(): Boolean