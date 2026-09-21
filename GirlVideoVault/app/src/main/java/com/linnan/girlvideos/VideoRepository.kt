package com.linnan.girlvideos

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

enum class SortMode { NAME, NEWEST, OLDEST, DURATION, FAVORITE_FIRST }

enum class GridSize(val columns: Int) {
    AUTO(0), TWO(2), THREE(3), FOUR(4), FIVE(5)
}

class VideoRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("girl_video_vault", Context.MODE_PRIVATE)
    private val key = "videos"
    private val pinHashKey = "pin_hash"
    private val gridKey = "grid_size"
    private val sortKey = "sort_mode"

    fun load(): MutableList<VideoItem> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { i ->
                val o = array.getJSONObject(i)
                VideoItem(
                    uri = o.getString("uri"),
                    name = o.optString("name", "Video"),
                    durationMs = o.optLong("duration", 0L),
                    addedAt = o.optLong("addedAt", System.currentTimeMillis()),
                    favorite = o.optBoolean("favorite", false)
                )
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun save(items: List<VideoItem>) {
        prefs.edit().putString(key, toJson(items)).apply()
    }

    fun toJson(items: List<VideoItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("uri", item.uri)
                put("name", item.name)
                put("duration", item.durationMs)
                put("addedAt", item.addedAt)
                put("favorite", item.favorite)
            })
        }
        return array.toString()
    }

    /** Parses a previously exported backup. Returns null if the content isn't valid. */
    fun fromJson(raw: String): List<VideoItem>? {
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                VideoItem(
                    uri = o.getString("uri"),
                    name = o.optString("name", "Video"),
                    durationMs = o.optLong("duration", 0L),
                    addedAt = o.optLong("addedAt", System.currentTimeMillis()),
                    favorite = o.optBoolean("favorite", false)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    fun createItem(uri: Uri): VideoItem {
        var name = "Video"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = c.getString(index) ?: name
                }
            }
        } catch (_: Exception) { }

        var duration = 0L
        try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, uri)
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.release()
        } catch (_: Exception) { }

        return VideoItem(
            uri = uri.toString(),
            name = name,
            durationMs = duration,
            addedAt = System.currentTimeMillis(),
            favorite = false
        )
    }

    // --- App lock (PIN) ---

    fun hasPin(): Boolean = prefs.contains(pinHashKey)

    fun setPin(pin: String) {
        prefs.edit().putString(pinHashKey, hash(pin)).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(pinHashKey, null) ?: return false
        return stored == hash(pin)
    }

    fun clearPin() {
        prefs.edit().remove(pinHashKey).apply()
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(("girl-video-vault:" + value).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- Preferences ---

    fun getGridSize(): GridSize {
        val name = prefs.getString(gridKey, GridSize.AUTO.name)
        return try { GridSize.valueOf(name ?: GridSize.AUTO.name) } catch (_: Exception) { GridSize.AUTO }
    }

    fun setGridSize(size: GridSize) {
        prefs.edit().putString(gridKey, size.name).apply()
    }

    fun getSortMode(): SortMode {
        val name = prefs.getString(sortKey, SortMode.NEWEST.name)
        return try { SortMode.valueOf(name ?: SortMode.NEWEST.name) } catch (_: Exception) { SortMode.NEWEST }
    }

    fun setSortMode(mode: SortMode) {
        prefs.edit().putString(sortKey, mode.name).apply()
    }
}

fun List<VideoItem>.sortedByMode(mode: SortMode): List<VideoItem> = when (mode) {
    SortMode.NAME -> sortedBy { it.name.lowercase() }
    SortMode.NEWEST -> sortedByDescending { it.addedAt }
    SortMode.OLDEST -> sortedBy { it.addedAt }
    SortMode.DURATION -> sortedByDescending { it.durationMs }
    SortMode.FAVORITE_FIRST -> sortedWith(compareByDescending<VideoItem> { it.favorite }.thenByDescending { it.addedAt })
}
