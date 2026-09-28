package com.linnan.instadownloader.data.model

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class PostType {
    PHOTO,
    VIDEO,
    REEL,
    CAROUSEL
}

data class MediaItem(
    val id: String,
    val index: Int,
    val type: MediaType,
    val downloadUrl: String,
    val thumbnailUrl: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long? = null
)

data class InstagramPost(
    val shortcode: String,
    val postType: PostType,
    val ownerUsername: String?,
    val caption: String?,
    val items: List<MediaItem>
)
