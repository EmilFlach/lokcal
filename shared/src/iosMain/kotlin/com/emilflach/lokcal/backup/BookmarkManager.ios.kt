@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.*

actual object BookmarkManager {
    private val bookmarkFile = FileKit.filesDir / "bookmark.bin"

    actual suspend fun save(file: PlatformFile?) {
        try {
            val bookmark = file!!.bookmarkData()
            bookmarkFile.write(bookmark.bytes)
        } catch (_: Exception) {
            // Error saving bookmark
        }
    }

    actual suspend fun load(): PlatformFile? {
        if (!bookmarkFile.exists()) return null

        return try {
            val bytes = bookmarkFile.readBytes()
            val file = PlatformFile.fromBookmarkData(bytes)

            if (file.exists()) {
                file
            } else {
                clear()
                null
            }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    actual suspend fun clear() {
        try {
            if (bookmarkFile.exists()) {
                bookmarkFile.delete()
            }
        } catch (_: Exception) {
            // Error clearing bookmark
        }
    }
}
