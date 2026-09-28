package com.linnan.instadownloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shortcode: String,
    val ownerUsername: String?,
    val mediaType: String,
    val savedAtEpochMillis: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val contentUri: String,
    val mimeType: String
)
