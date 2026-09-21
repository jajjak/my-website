package com.linnan.girlvideos

data class VideoItem(
    val uri: String,
    var name: String,
    val durationMs: Long,
    val addedAt: Long,
    var favorite: Boolean = false
)
