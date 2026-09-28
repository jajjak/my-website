package com.linnan.instadownloader.data.model

data class HistoryItem(
    val id: Long,
    val shortcode: String,
    val ownerUsername: String?,
    val mediaType: MediaType,
    val savedAtEpochMillis: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val contentUri: String,
    val mimeType: String
)
