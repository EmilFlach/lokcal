package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.write

actual suspend fun replaceDatabase(file: PlatformFile): Boolean {
    val destination = FileKit.databasesDir / "lokcal.db"
    try {
        destination.write(file)
        return true
    } catch (_: Exception) {
        return false
    }
}

actual suspend fun copyDatabase(): Boolean {
    val database = FileKit.databasesDir / "lokcal.db"
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