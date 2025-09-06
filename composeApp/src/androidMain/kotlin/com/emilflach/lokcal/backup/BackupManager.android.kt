package com.emilflach.lokcal.backup

import androidx.documentfile.provider.DocumentFile
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.startAccessingSecurityScopedResource
import io.github.vinceglb.filekit.stopAccessingSecurityScopedResource
import io.github.vinceglb.filekit.write
import java.net.URLDecoder

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

actual suspend fun exportDatabaseToBackupDirectory(): Boolean {
    val database = FileKit.databasesDir / "lokcal.db"
    val directory = BookmarkManager.load()
    if (directory == null) return false
    try {
        directory.startAccessingSecurityScopedResource()
        val fileName = "lokcal-backup-${System.currentTimeMillis()}.db"
        val treeUri = (directory.androidFile as AndroidFile.UriWrapper).uri
        val docDir = DocumentFile.fromTreeUri(FileKit.context, treeUri)
        val backupFile = docDir!!.createFile("application/x-sqlite3", fileName)
        val destination = PlatformFile(backupFile!!.uri)
        destination.write(database)
        directory.stopAccessingSecurityScopedResource()
        return true
    } catch (e: Exception) {
        println(e)
        directory.stopAccessingSecurityScopedResource()
        return false
    }
}


actual fun allowNightlyBackup(): Boolean {
    return true
}

actual suspend fun chooseBackupDirectory(): PlatformFile? {
    val directory = FileKit.openDirectoryPicker()
    BookmarkManager.save(directory)
    return directory
}

actual suspend fun retrieveBackupDirectory(): String {
    val directory = BookmarkManager.load()
    return if (directory != null) {
        URLDecoder
            .decode(directory.toString(), "UTF-8")
            .substringAfterLast(":")
    } else {
        "No directory set"
    }
}

actual fun enableNightlyBackup(value: Boolean): Boolean {
    return true
}