package com.linnan.instadownloader.data.remote

import com.linnan.instadownloader.data.model.PostType

data class ParsedInstagramUrl(
    val shortcode: String,
    val hintedPostType: PostType
)

/**
 * Recognizes instagram.com/p/, /reel/ and /reels/ links, with or without a leading
 * username segment, query string, or share suffix (e.g. ?igsh=...).
 */
object InstagramUrlParser {

    private val URL_REGEX = Regex(
        pattern = "instagram\\.com/(?:[A-Za-z0-9_.]+/)?(p|reel|reels)/([A-Za-z0-9_-]+)",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(rawUrl: String): ParsedInstagramUrl? {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return null

        val match = URL_REGEX.find(trimmed) ?: return null
        val segment = match.groupValues[1].lowercase()
        val shortcode = match.groupValues[2]

        val hintedType = when (segment) {
            "reel", "reels" -> PostType.REEL
            else -> PostType.PHOTO
        }

        return ParsedInstagramUrl(shortcode = shortcode, hintedPostType = hintedType)
    }

    fun isInstagramUrl(rawUrl: String): Boolean = URL_REGEX.containsMatchIn(rawUrl.trim())
}
