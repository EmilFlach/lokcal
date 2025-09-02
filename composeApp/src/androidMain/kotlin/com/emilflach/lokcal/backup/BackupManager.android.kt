package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.write
import java.io.File

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
    val externalFilesDir = FileKit.context.getExternalFilesDir(null)
    val destination = File(externalFilesDir, "lokcal-backup-${System.currentTimeMillis()}.db")
    val destinationFile = PlatformFile(destination)
    try {
        destinationFile.write(database)
        return true
    } catch (_: Exception) {
        return false
    }
}