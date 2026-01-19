package com.emilflach.lokcal.backup

import com.emilflach.lokcal.data.WorkerInstance
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CompletableDeferred
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

actual suspend fun chooseBackupDirectory(): PlatformFile? {
    return null
}

actual suspend fun retrieveBackupDirectory(): String? {
    return null
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun generateRandomId(): JsNumber = js("Math.random() * 1000000")

@OptIn(ExperimentalWasmJsInterop::class)
private fun createImportMessage(buffer: Uint8Array, id: JsNumber): JsAny =
    js("({ action: 'import_db', buffer: buffer, id: id })")

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun replaceDatabase(file: PlatformFile): Boolean {
    val worker = WorkerInstance.worker ?: return false
    try {
        val bytes = file.readBytes()
        val buffer = Uint8Array(bytes.size)
        for (i in bytes.indices) {
            buffer[i] = bytes[i]
        }

        val deferred = CompletableDeferred<Boolean>()
        val id = generateRandomId()

        setupWorkerListener(worker, id, callback = { success ->
            deferred.complete(success.toBoolean())
        })

        val message = createImportMessage(buffer, id)
        worker.postMessage(message)

        return deferred.await()
    } catch (e: Exception) {
        println("Error replacing database: $e")
        return false
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun setupWorkerListener(worker: org.w3c.dom.Worker, id: JsNumber, callback: (JsBoolean) -> Unit) {
    js("""
        const listener = (event) => {
            if (event.data && event.data.id === id) {
                worker.removeEventListener('message', listener);
                callback(event.data.error === undefined);
            }
        };
        worker.addEventListener('message', listener);
        return undefined;
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun createExportMessage(id: JsNumber): JsAny =
    js("({ action: 'export_db', id: id })")

@OptIn(ExperimentalWasmJsInterop::class)
private fun setupExportWorkerListener(worker: org.w3c.dom.Worker, id: JsNumber, callback: (Uint8Array?) -> Unit) {
    js("""
        const listener = (event) => {
            if (event.data && event.data.id === id) {
                worker.removeEventListener('message', listener);
                if (event.data.error) {
                    console.error('Worker error during export:', event.data.error);
                    callback(null);
                } else {
                    callback(event.data.buffer);
                }
            }
        };
        worker.addEventListener('message', listener);
        return undefined;
    """)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun currentTimeMillis(): Double = js("Date.now()")

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun copyDatabase(): Boolean {
    val worker = WorkerInstance.worker ?: return false
    try {
        val deferred = CompletableDeferred<Uint8Array?>()
        val id = generateRandomId()

        setupExportWorkerListener(worker, id, callback = { buffer ->
            deferred.complete(buffer)
        })

        val message = createExportMessage(id)
        worker.postMessage(message)

        val buffer = deferred.await() ?: return false

        val bytes = ByteArray(buffer.length)
        for (i in 0 until buffer.length) {
            bytes[i] = buffer[i]
        }

        saveFile(bytes, "lokcal-backup-${currentTimeMillis().toLong()}.db")
        return true
    } catch (e: Exception) {
        println("Error copying database: $e")
        return false
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun triggerDownload(buffer: Uint8Array, fileName: String): Unit = js("""
    (function() {
        const blob = new Blob([buffer], { type: 'application/x-sqlite3' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    })()
""")

@OptIn(ExperimentalWasmJsInterop::class)
private fun saveFile(bytes: ByteArray, fileName: String) {
    val buffer = Uint8Array(bytes.size)
    for (i in bytes.indices) {
        buffer[i] = bytes[i]
    }
    triggerDownload(buffer, fileName)
}

actual suspend fun exportDatabaseToBackupDirectory(): Boolean {
    return false
}

actual fun allowNightlyBackup(): Boolean {
    return false
}

actual fun enableNightlyBackup(value: Boolean): Boolean {
    return false
}

actual suspend fun isNightlyBackupEnabled(): Boolean {
    return false
}