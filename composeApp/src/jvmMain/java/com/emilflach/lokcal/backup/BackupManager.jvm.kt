package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.PlatformFile
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
    val destination = PlatformFile("lokcal-backup-${System.currentTimeMillis()}.db")
    try {
        destination.write(database)
        return true
    } catch (e: Exception) {
        println(e)
        return false
    }
}