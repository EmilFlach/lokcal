package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.PlatformFile

actual suspend fun chooseBackupDirectory(): PlatformFile? {
    TODO("Not yet implemented")
}

actual suspend fun retrieveBackupDirectory(): String? {
    TODO("Not yet implemented")
}

actual suspend fun replaceDatabase(file: PlatformFile): Boolean {
    TODO("Not yet implemented")
}

actual suspend fun copyDatabase(): Boolean {
    TODO("Not yet implemented")
}

actual suspend fun exportDatabaseToBackupDirectory(): Boolean {
    TODO("Not yet implemented")
}

actual fun allowNightlyBackup(): Boolean {
    return false
}

actual fun enableNightlyBackup(value: Boolean): Boolean {
    TODO("Not yet implemented")
}

actual suspend fun isNightlyBackupEnabled(): Boolean {
    TODO("Not yet implemented")
}