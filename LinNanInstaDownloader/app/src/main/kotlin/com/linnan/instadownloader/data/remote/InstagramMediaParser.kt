package com.linnan.instadownloader.data.remote

import com.linnan.instadownloader.data.model.FailureReason
import com.linnan.instadownloader.data.model.InstaOutcome
import com.linnan.instadownloader.data.model.InstagramPost
import com.linnan.instadownloader.data.model.MediaItem
import com.linnan.instadownloader.data.model.MediaType
import com.linnan.instadownloader.data.model.PostType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Talks to Instagram's own public web endpoints for a post's shortcode and turns
 * whatever real response comes back into [MediaItem]s. Nothing here fabricates data:
 * every field is read from the actual HTTP response, and a missing/blocked response
 * is surfaced as a [FailureReason], never as a fake fallback.
 *
 * Instagram's app-id constant below (936619743392459) is the same public id every
 * logged-out browser sends to instagram.com; it is not a stolen credential. When
 * Instagram's own anti-abuse system replies with a login wall or a rate limit, that
 * reply is treated as final -- this app does not attempt to defeat CAPTCHAs, forge
 * session cookies, or rotate IPs to get around it.
 */
class InstagramMediaParser(
    private val client: OkHttpClient = NetworkModule.client
) {

    suspend fun fetchPost(shortcode: String): InstaOutcome<InstagramPost> = withContext(Dispatchers.IO) {
        var lastFailure = FailureReason.MEDIA_UNAVAILABLE

        fetchViaEmbedPage(shortcode).let { attempt ->
            if (attempt is AttemptResult.Parsed) return@withContext InstaOutcome.Success(attempt.post)
            if (attempt is AttemptResult.Failed) lastFailure = attempt.reason
        }

        fetchViaAjaxJson(shortcode).let { attempt ->
            if (attempt is AttemptResult.Parsed) return@withContext InstaOutcome.Success(attempt.post)
            if (attempt is AttemptResult.Failed) lastFailure = attempt.reason
        }

        InstaOutcome.Failure(lastFailure)
    }

    private sealed class AttemptResult {
        data class Parsed(val post: InstagramPost) : AttemptResult()
        data class Failed(val reason: FailureReason) : AttemptResult()
        object NoData : AttemptResult()
    }

    private fun fetchViaEmbedPage(shortcode: String): AttemptResult {
        val url = "https://www.instagram.com/p/$shortcode/embed/captioned/"
        val response = safeGet(url, extraHeaders = emptyMap()) ?: return AttemptResult.Failed(FailureReason.NETWORK_ERROR)
        response.exception?.let { return AttemptResult.Failed(classifyException(it)) }
        val code = response.code ?: return AttemptResult.Failed(FailureReason.UNKNOWN)
        if (code == 404) return AttemptResult.Failed(FailureReason.NOT_FOUND)
        if (code == 429) return AttemptResult.Failed(FailureReason.RATE_LIMITED)
        if (code !in 200..299) return AttemptResult.NoData

        val html = response.body ?: return AttemptResult.NoData
        val json = extractEmbeddedJson(html) ?: return AttemptResult.NoData
        val post = parsePostJson(json, shortcode) ?: return AttemptResult.NoData
        return AttemptResult.Parsed(post)
    }

    private fun fetchViaAjaxJson(shortcode: String): AttemptResult {
        val url = "https://www.instagram.com/p/$shortcode/?__a=1&__d=dis"
        val headers = mapOf(
            "X-IG-App-ID" to INSTAGRAM_WEB_APP_ID,
            "Accept" to "application/json"
        )
        val response = safeGet(url, headers) ?: return AttemptResult.Failed(FailureReason.NETWORK_ERROR)
        response.exception?.let { return AttemptResult.Failed(classifyException(it)) }
        val code = response.code ?: return AttemptResult.Failed(FailureReason.UNKNOWN)
        when (code) {
            404 -> return AttemptResult.Failed(FailureReason.NOT_FOUND)
            401, 403 -> return AttemptResult.Failed(FailureReason.PRIVATE_OR_LOGIN_REQUIRED)
            429 -> return AttemptResult.Failed(FailureReason.RATE_LIMITED)
        }
        if (code !in 200..299) return AttemptResult.NoData

        val contentType = response.contentType.orEmpty()
        if (!contentType.contains("json")) return AttemptResult.NoData

        val body = response.body ?: return AttemptResult.NoData
        val json = runCatching { JSONObject(body) }.getOrNull() ?: return AttemptResult.NoData

        if (json.optBoolean("require_login", false)) {
            return AttemptResult.Failed(FailureReason.PRIVATE_OR_LOGIN_REQUIRED)
        }

        val post = parsePostJson(json, shortcode) ?: return AttemptResult.NoData
        return AttemptResult.Parsed(post)
    }

    private fun extractEmbeddedJson(html: String): JSONObject? {
        val markers = listOf("\"shortcode_media\":", "\"graphql\":")
        for (marker in markers) {
            val idx = html.indexOf(marker)
            if (idx < 0) continue
            val objStart = html.indexOf('{', idx)
            if (objStart < 0) continue
            val objText = extractBalancedJsonObject(html, objStart) ?: continue
            val parsed = runCatching { JSONObject(objText) }.getOrNull() ?: continue
            return JSONObject().put("graphql", JSONObject().put("shortcode_media", parsed))
        }
        return null
    }

    /** Walks forward from an opening '{' and returns the matching balanced JSON object text. */
    private fun extractBalancedJsonObject(text: String, start: Int): String? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val c = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    internal fun parsePostJson(root: JSONObject, shortcode: String): InstagramPost? {
        val mediaRoot = root.optJSONObject("graphql")?.optJSONObject("shortcode_media")
            ?: root.optJSONArray("items")?.optJSONObject(0)
            ?: if (root.has("image_versions2") || root.has("display_url") || root.has("video_versions")) root else null
            ?: return null

        val ownerUsername = mediaRoot.optJSONObject("owner")?.optString("username")
            ?: mediaRoot.optJSONObject("user")?.optString("username")

        val caption = extractCaption(mediaRoot)

        val childNodes = extractChildren(mediaRoot)

        val items = if (childNodes.isNotEmpty()) {
            childNodes.mapIndexedNotNull { index, child -> parseMediaNode(child, index) }
        } else {
            listOfNotNull(parseMediaNode(mediaRoot, 0))
        }

        if (items.isEmpty()) return null

        val postType = when {
            items.size > 1 -> PostType.CAROUSEL
            items.first().type == MediaType.VIDEO -> PostType.VIDEO
            else -> PostType.PHOTO
        }

        return InstagramPost(
            shortcode = shortcode,
            postType = postType,
            ownerUsername = ownerUsername,
            caption = caption,
            items = items
        )
    }

    private fun extractCaption(mediaRoot: JSONObject): String? {
        mediaRoot.optJSONObject("edge_media_to_caption")
            ?.optJSONArray("edges")
            ?.let { edges ->
                if (edges.length() > 0) {
                    val text = edges.optJSONObject(0)?.optJSONObject("node")?.optString("text")
                    if (!text.isNullOrBlank()) return text
                }
            }
        mediaRoot.optJSONObject("caption")?.optString("text")?.let { if (it.isNotBlank()) return it }
        return null
    }

    private fun extractChildren(mediaRoot: JSONObject): List<JSONObject> {
        mediaRoot.optJSONObject("edge_sidecar_to_children")?.optJSONArray("edges")?.let { edges ->
            val list = mutableListOf<JSONObject>()
            for (i in 0 until edges.length()) {
                edges.optJSONObject(i)?.optJSONObject("node")?.let { list.add(it) }
            }
            if (list.isNotEmpty()) return list
        }
        mediaRoot.optJSONArray("carousel_media")?.let { carousel ->
            val list = mutableListOf<JSONObject>()
            for (i in 0 until carousel.length()) {
                carousel.optJSONObject(i)?.let { list.add(it) }
            }
            if (list.isNotEmpty()) return list
        }
        return emptyList()
    }

    private fun parseMediaNode(node: JSONObject, index: Int): MediaItem? {
        val isVideoGraphql = node.optString("__typename") == "GraphVideo"
        val mediaTypePrivate = node.optInt("media_type", -1)
        val isVideo = isVideoGraphql || mediaTypePrivate == 2 || node.has("video_versions") || node.optBoolean("is_video", false)

        val (thumbUrl, thumbW, thumbH) = pickBestImage(node) ?: Triple(null, 0, 0)

        return if (isVideo) {
            val (videoUrl, vw, vh) = pickBestVideo(node) ?: return null
            MediaItem(
                id = "${node.optString("id", index.toString())}_$index",
                index = index,
                type = MediaType.VIDEO,
                downloadUrl = videoUrl,
                thumbnailUrl = thumbUrl ?: videoUrl,
                width = if (vw > 0) vw else thumbW,
                height = if (vh > 0) vh else thumbH
            )
        } else {
            val url = thumbUrl ?: return null
            MediaItem(
                id = "${node.optString("id", index.toString())}_$index",
                index = index,
                type = MediaType.IMAGE,
                downloadUrl = url,
                thumbnailUrl = url,
                width = thumbW,
                height = thumbH
            )
        }
    }

    private fun pickBestImage(node: JSONObject): Triple<String, Int, Int>? {
        node.optJSONObject("image_versions2")?.optJSONArray("candidates")?.let { candidates ->
            var best: Triple<String, Int, Int>? = null
            for (i in 0 until candidates.length()) {
                val c = candidates.optJSONObject(i) ?: continue
                val w = c.optInt("width", 0)
                val url = c.optString("url").takeIf { it.isNotBlank() } ?: continue
                if (best == null || w > best!!.second) best = Triple(url, w, c.optInt("height", 0))
            }
            if (best != null) return best
        }
        node.optJSONArray("display_resources")?.let { resources ->
            var best: Triple<String, Int, Int>? = null
            for (i in 0 until resources.length()) {
                val r = resources.optJSONObject(i) ?: continue
                val w = r.optInt("config_width", 0)
                val url = r.optString("src").takeIf { it.isNotBlank() } ?: continue
                if (best == null || w > best!!.second) best = Triple(url, w, r.optInt("config_height", 0))
            }
            if (best != null) return best
        }
        node.optString("display_url").takeIf { it.isNotBlank() }?.let { url ->
            val dims = node.optJSONObject("dimensions")
            return Triple(url, dims?.optInt("width") ?: 0, dims?.optInt("height") ?: 0)
        }
        return null
    }

    private fun pickBestVideo(node: JSONObject): Triple<String, Int, Int>? {
        node.optJSONArray("video_versions")?.let { versions ->
            var best: Triple<String, Int, Int>? = null
            for (i in 0 until versions.length()) {
                val v = versions.optJSONObject(i) ?: continue
                val w = v.optInt("width", 0)
                val url = v.optString("url").takeIf { it.isNotBlank() } ?: continue
                if (best == null || w > best!!.second) best = Triple(url, w, v.optInt("height", 0))
            }
            if (best != null) return best
        }
        node.optString("video_url").takeIf { it.isNotBlank() }?.let { url ->
            val dims = node.optJSONObject("dimensions")
            return Triple(url, dims?.optInt("width") ?: 0, dims?.optInt("height") ?: 0)
        }
        return null
    }

    private data class RawResponse(
        val code: Int?,
        val body: String?,
        val contentType: String?,
        val exception: IOException?
    )

    private fun safeGet(url: String, extraHeaders: Map<String, String>): RawResponse? {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", NetworkModule.MOBILE_USER_AGENT)
            .header("Accept-Language", "ja-JP,ja;q=0.9,en-US;q=0.8")
        extraHeaders.forEach { (k, v) -> builder.header(k, v) }

        return try {
            client.newCall(builder.build()).execute().use { resp ->
                val bytes = resp.body?.bytes()
                RawResponse(
                    code = resp.code,
                    body = bytes?.toString(Charsets.UTF_8),
                    contentType = resp.header("Content-Type"),
                    exception = null
                )
            }
        } catch (e: IOException) {
            RawResponse(code = null, body = null, contentType = null, exception = e)
        }
    }

    private fun classifyException(e: IOException): FailureReason = FailureReason.NETWORK_ERROR

    companion object {
        private const val INSTAGRAM_WEB_APP_ID = "936619743392459"
    }
}
