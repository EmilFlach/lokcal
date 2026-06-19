package com.emilflach.lokcal.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.emilflach.lokcal.Database

data class CachedImage(val bytes: ByteArray, val mimeType: String, val sourceUrl: String?)

class ImageCacheRepository(database: Database) {
    private val queries = database.imageCacheQueries

    suspend fun getImage(entityType: String, entityId: Long): CachedImage? {
        val row = queries.getImage(entityType, entityId).awaitAsOneOrNull() ?: return null
        return CachedImage(row.image_data, row.mime_type, row.source_url)
    }

    suspend fun saveImage(
        entityType: String,
        entityId: Long,
        bytes: ByteArray,
        mimeType: String,
        sourceUrl: String? = null,
    ) {
        queries.upsertImage(
            entity_type = entityType,
            entity_id = entityId,
            image_data = bytes,
            mime_type = mimeType,
            byte_size = bytes.size.toLong(),
            source_url = sourceUrl,
        )
    }

    suspend fun deleteImage(entityType: String, entityId: Long) {
        queries.deleteImage(entityType, entityId)
    }

    suspend fun getCachedIdsByType(entityType: String): Set<Long> {
        return queries.getCachedIdsByType(entityType).awaitAsList().toSet()
    }

}
