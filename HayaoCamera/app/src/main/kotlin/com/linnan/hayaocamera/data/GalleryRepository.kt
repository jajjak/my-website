package com.linnan.hayaocamera.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Only ever surfaces media this app itself wrote to shared storage — never the user's
 * whole camera roll — so no broad media-library permission is requested.
 */
class GalleryRepository(private val context: Context) {

    suspend fun loadOwnMedia(): List<GalleryItem> = withContext(Dispatchers.IO) {
        val images = queryCollection(
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            isVideo = false
        )
        val videos = queryCollection(
            collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            isVideo = true
        )
        (images + videos).sortedByDescending { it.dateAddedSeconds }
    }

    suspend fun delete(item: GalleryItem): Boolean = withContext(Dispatchers.IO) {
        runCatching { context.contentResolver.delete(item.uri, null, null) > 0 }.getOrDefault(false)
    }

    private fun queryCollection(collection: android.net.Uri, isVideo: Boolean): List<GalleryItem> {
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DATE_ADDED)
            add(MediaStore.MediaColumns.WIDTH)
            add(MediaStore.MediaColumns.HEIGHT)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            if (isVideo) add(MediaStore.Video.Media.DURATION)
        }.toTypedArray()

        val (selection, args) = ownerSelection()
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val items = mutableListOf<GalleryItem>()
        val cursor = runCatching {
            context.contentResolver.query(collection, projection, selection, args, sortOrder)
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val widthCol = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightCol = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val nameCol = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val durationCol = if (isVideo) c.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += GalleryItem(
                    id = id,
                    uri = uri,
                    isVideo = isVideo,
                    dateAddedSeconds = c.getLong(dateCol),
                    durationMs = if (durationCol >= 0) c.getLong(durationCol) else 0L,
                    width = if (widthCol >= 0) c.getInt(widthCol) else 0,
                    height = if (heightCol >= 0) c.getInt(heightCol) else 0,
                    displayName = if (nameCol >= 0) c.getString(nameCol) ?: "" else ""
                )
            }
        }
        return items
    }

    private fun ownerSelection(): Pair<String, Array<String>> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME} = ?" to arrayOf(context.packageName)
        } else {
            "${MediaStore.MediaColumns.DATA} LIKE ?" to arrayOf("%/$ALBUM_NAME/%")
        }
    }
}
