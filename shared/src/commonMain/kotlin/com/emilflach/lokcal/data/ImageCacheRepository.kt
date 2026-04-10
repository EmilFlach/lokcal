package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database

class ImageCacheRepository(database: Database) {
    private val queries = database.imageCacheQueries

    suspend fun getImage(entityType: String, entityId: Long): Pair<ByteArray, String>? {
        val row = queries.getImage(entityType, entityId).awaitAsOneOrNull() ?: return null
        return row.image_data to row.mime_type
    }

    suspend fun saveImage(entityType: String, entityId: Long, bytes: ByteArray, mimeType: String) {
        queries.upsertImage(
            entity_type = entityType,
            entity_id = entityId,
            image_data = bytes,
            mime_type = mimeType,
            byte_size = bytes.size.toLong()
        )
    }

    suspend fun deleteImage(entityType: String, entityId: Long) {
        queries.deleteImage(entityType, entityId)
    }

    suspend fun getTotalCacheSizeBytes(): Long {
        return queries.getTotalCacheSize().awaitAsOneOrNull() ?: 0L
    }

    suspend fun getCachedIdsByType(entityType: String): Set<Long> {
        return queries.getCachedIdsByType(entityType).awaitAsList().toSet()
    }

    suspend fun evictIfNeeded(maxBytes: Long = 50L * 1024 * 1024) {
        var total = getTotalCacheSizeBytes()
        if (total <= maxBytes) return
        val candidates = queries.getOldestEntries(50).awaitAsList()
        for (entry in candidates) {
            if (total <= maxBytes) break
            val row = queries.getImage(entry.entity_type, entry.entity_id).awaitAsOneOrNull() ?: continue
            total -= row.image_data.size
            queries.deleteImage(entry.entity_type, entry.entity_id)
        }
    }
}
