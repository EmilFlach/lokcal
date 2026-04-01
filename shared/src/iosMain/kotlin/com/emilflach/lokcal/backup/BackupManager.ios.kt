package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*

// NativeSqliteDriver (via SQLiter) stores the DB in NSApplicationSupportDirectory/databases/,
// NOT in NSDocumentDirectory which is what FileKit.databasesDir points to.
private fun databasesDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
    return "${paths.first() as String}/databases"
}

private fun liveDatabase(): PlatformFile =
    PlatformFile(NSURL.fileURLWithPath("${databasesDir()}/lokcal.db"))

// Delete stale WAL files so the new DB doesn't get corrupted on next open.
@OptIn(ExperimentalForeignApi::class)
private fun deleteWalFiles() {
    val dir = databasesDir()
    val fm = NSFileManager.defaultManager
    fm.removeItemAtPath("$dir/lokcal.db-wal", null)
    fm.removeItemAtPath("$dir/lokcal.db-shm", null)
}

private fun showRestartAlert() {
    val alert = UIAlertController.alertControllerWithTitle(
        title = "Import successful",
        message = "Reopen the app to see your imported data.",
        preferredStyle = UIAlertControllerStyleAlert
    )
    alert.addAction(UIAlertAction.actionWithTitle("Ok", UIAlertActionStyleDefault) {
        kotlin.system.exitProcess(0)
    })
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        alert, animated = true, completion = null
    )
}

actual suspend fun replaceDatabase(file: PlatformFile): Boolean {
    val destination = liveDatabase()
    return try {
        file.startAccessingSecurityScopedResource()
        destination.write(file)
        file.stopAccessingSecurityScopedResource()
        deleteWalFiles()
        showRestartAlert()
        true
    } catch (_: Exception) {
        file.stopAccessingSecurityScopedResource()
        false
    }
}

actual suspend fun copyDatabase(): Boolean {
    val database = liveDatabase()
    return try {
        if (!database.exists()) return false
        val destination = FileKit.openFileSaver(
            suggestedName = "lokcal-backup-${NSDate().timeIntervalSince1970.toLong()}",
            extension = "db"
        )
        destination?.write(database)
        true
    } catch (_: Exception) {
        false
    }
}

actual suspend fun exportDatabaseToBackupDirectory(): Boolean {
    val database = liveDatabase()
    val directory = BookmarkManager.load() ?: return false
    return try {
        directory.startAccessingSecurityScopedResource()
        val fileName = "lokcal-backup-${NSDate().timeIntervalSince1970.toLong()}.db"
        val backupFile = directory / fileName
        backupFile.write(database)
        directory.stopAccessingSecurityScopedResource()
        true
    } catch (_: Exception) {
        directory.stopAccessingSecurityScopedResource()
        false
    }
}

actual fun allowNightlyBackup(): Boolean = false

actual suspend fun chooseBackupDirectory(): PlatformFile? {
    val directory = FileKit.openDirectoryPicker()
    BookmarkManager.save(directory)
    return directory
}

actual suspend fun retrieveBackupDirectory(): String? {
    return BookmarkManager.load()?.toString()?.removePrefix("file://")?.trimEnd('/')
}

actual fun enableNightlyBackup(value: Boolean): Boolean = false

actual suspend fun isNightlyBackupEnabled(): Boolean = false
