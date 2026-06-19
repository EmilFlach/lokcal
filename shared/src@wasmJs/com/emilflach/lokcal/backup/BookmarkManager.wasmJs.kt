package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.PlatformFile

actual object BookmarkManager {
    actual suspend fun save(file: PlatformFile?) {
    }

    actual suspend fun load(): PlatformFile? {
        TODO("Not yet implemented")
    }

    actual suspend fun clear() {
    }
}