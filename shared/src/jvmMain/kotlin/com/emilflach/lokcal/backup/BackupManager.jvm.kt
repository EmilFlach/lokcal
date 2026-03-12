package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun replaceDatabase(file: PlatformFile): Boolean {
    val destination = PlatformFile("lokcal.db")
    try {
        destination.write(file)
        return true
    } catch (e: Exception) {
        println(e)
        return false
    }
}

actual suspend fun copyDatabase(): Boolean {
    val database = PlatformFile("lokcal.db")
    try {
        val destination = FileKit.openFileSaver(
            suggestedName = "lokcal-backup-${System.currentTimeMillis()}",
            extension = "db"
        )
        destination?.write(database)
        return true
    } catch (e: Exception) {
        println(e)
        return false
    }
}

actual fun allowNightlyBackup(): Boolean {
    return false
}

actual suspend fun chooseBackupDirectory(): PlatformFile? {
    TODO("Not yet implemented")
}

actual suspend fun retrieveBackupDirectory(): String? {
    TODO("Not yet implemented")
}

actual suspend fun exportDatabaseToBackupDirectory(): Boolean {
    TODO("Not yet implemented")
}

actual fun enableNightlyBackup(value: Boolean): Boolean {
    TODO("Not yet implemented")
}

actual suspend fun isNightlyBackupEnabled(): Boolean {
    TODO("Not yet implemented")
}