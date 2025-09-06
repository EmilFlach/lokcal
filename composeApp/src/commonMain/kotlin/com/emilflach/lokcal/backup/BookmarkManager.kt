package com.emilflach.lokcal.backup

import io.github.vinceglb.filekit.PlatformFile

expect object BookmarkManager {

    suspend fun save(file: PlatformFile?)

    suspend fun load(): PlatformFile?

    suspend fun clear()
}