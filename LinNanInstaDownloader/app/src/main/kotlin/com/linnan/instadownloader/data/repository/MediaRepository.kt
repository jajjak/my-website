package com.linnan.instadownloader.data.repository

import com.linnan.instadownloader.data.model.FailureReason
import com.linnan.instadownloader.data.model.InstaOutcome
import com.linnan.instadownloader.data.model.InstagramPost
import com.linnan.instadownloader.data.model.PostType
import com.linnan.instadownloader.data.remote.InstagramMediaParser
import com.linnan.instadownloader.data.remote.InstagramUrlParser
import com.linnan.instadownloader.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

/**
 * Orchestrates URL parsing, remote lookup, and a best-effort file-size probe.
 * Never invents data: a size that cannot be confirmed with a real HEAD response
 * is left null so the UI can show "不明" instead of a made-up number.
 */
class MediaRepository(
    private val mediaParser: InstagramMediaParser = InstagramMediaParser()
) {

    suspend fun analyze(rawUrl: String): InstaOutcome<InstagramPost> {
        val parsedUrl = InstagramUrlParser.parse(rawUrl)
            ?: return InstaOutcome.Failure(FailureReason.INVALID_URL)

        val outcome = mediaParser.fetchPost(parsedUrl.shortcode)
        if (outcome !is InstaOutcome.Success) return outcome

        var post = outcome.data
        if (parsedUrl.hintedPostType == PostType.REEL &&
            post.postType == PostType.VIDEO
        ) {
            post = post.copy(postType = PostType.REEL)
        }

        val itemsWithSize = post.items.map { item ->
            val size = probeContentLength(item.downloadUrl)
            if (size != null) item.copy(fileSizeBytes = size) else item
        }

        return InstaOutcome.Success(post.copy(items = itemsWithSize))
    }

    private suspend fun probeContentLength(url: String): Long? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", NetworkModule.MOBILE_USER_AGENT)
                .head()
                .build()
            NetworkModule.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val length = response.header("Content-Length")?.toLongOrNull()
                if (length != null && length > 0) length else null
            }
        } catch (e: IOException) {
            null
        }
    }
}
