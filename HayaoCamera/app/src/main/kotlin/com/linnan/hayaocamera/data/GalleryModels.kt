package com.linnan.hayaocamera.data

import android.net.Uri

data class GalleryItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateAddedSeconds: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val displayName: String
)
