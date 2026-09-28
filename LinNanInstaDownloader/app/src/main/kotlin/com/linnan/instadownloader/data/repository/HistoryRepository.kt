package com.linnan.instadownloader.data.repository

import android.content.Context
import com.linnan.instadownloader.data.local.HistoryEntity
import com.linnan.instadownloader.data.local.LinNanDatabase
import com.linnan.instadownloader.data.model.HistoryItem
import com.linnan.instadownloader.data.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryRepository(context: Context) {

    private val dao = LinNanDatabase.getInstance(context).historyDao()

    val history: Flow<List<HistoryItem>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun record(
        shortcode: String,
        ownerUsername: String?,
        mediaType: MediaType,
        width: Int,
        height: Int,
        fileSizeBytes: Long,
        contentUri: String,
        mimeType: String
    ) {
        dao.insert(
            HistoryEntity(
                shortcode = shortcode,
                ownerUsername = ownerUsername,
                mediaType = mediaType.name,
                savedAtEpochMillis = System.currentTimeMillis(),
                width = width,
                height = height,
                fileSizeBytes = fileSizeBytes,
                contentUri = contentUri,
                mimeType = mimeType
            )
        )
    }

    private fun HistoryEntity.toDomain() = HistoryItem(
        id = id,
        shortcode = shortcode,
        ownerUsername = ownerUsername,
        mediaType = MediaType.valueOf(mediaType),
        savedAtEpochMillis = savedAtEpochMillis,
        width = width,
        height = height,
        fileSizeBytes = fileSizeBytes,
        contentUri = contentUri,
        mimeType = mimeType
    )
}
