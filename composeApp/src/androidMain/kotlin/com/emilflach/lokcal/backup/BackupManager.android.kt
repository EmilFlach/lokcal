package com.emilflach.lokcal.backup

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.asFlow
import androidx.work.*
import io.github.vinceglb.filekit.*
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit

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

actual suspend fun retrieveBackupDirectory(): String? {
    val directory = BookmarkManager.load()
    return if (directory != null) {
        URLDecoder
            .decode(directory.toString(), "UTF-8")
            .substringAfterLast(":")
    } else {
        null
    }
}

actual fun enableNightlyBackup(value: Boolean): Boolean {
    return try {
        val workManager = WorkManager.getInstance(FileKit.context)
        if (value) {
            workManager.cancelUniqueWork("NightlyBackup")

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val initialDelayMillis = target.timeInMillis - now.timeInMillis

            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(true)
                        .build()
            ).build()

            workManager.enqueueUniquePeriodicWork(
                "NightlyBackup",
                ExistingPeriodicWorkPolicy.REPLACE,
                backupRequest
            )
        } else {
            workManager.cancelUniqueWork("NightlyBackup")
        }
        true
    } catch (e: Exception) {
        println("Failed to ${if (value) "enable" else "disable"} nightly backup: $e")
        false
    }
}

actual suspend fun isNightlyBackupEnabled(): Boolean {
    val workManager = WorkManager.getInstance(FileKit.context)

    try {
        val workInfos = workManager.getWorkInfosForUniqueWorkLiveData("NightlyBackup")
            .asFlow()
            .first()
        return workInfos.any { workInfo ->
            workInfo.state == WorkInfo.State.ENQUEUED ||
                    workInfo.state == WorkInfo.State.RUNNING
        }
    } catch (e: Exception) {
        return false
    }

}


class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val success = exportDatabaseToBackupDirectory()
                if (success) {
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (_: Exception) {
                Result.failure()
            }
        }
    }
}
