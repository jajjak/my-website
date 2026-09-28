package com.linnan.hayaocamera.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Album name used for both Pictures and Movies — never a hidden/random folder. */
const val ALBUM_NAME = "林男カメラ"

private fun timestampName(prefix: String, extension: String): String {
    val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    return "${prefix}_${fmt.format(Date())}.$extension"
}

@Suppress("DEPRECATION")
private fun legacyAlbumDir(publicDirType: String): java.io.File {
    val dir = java.io.File(Environment.getExternalStoragePublicDirectory(publicDirType), ALBUM_NAME)
    if (!dir.exists()) dir.mkdirs()
    return dir
}

object MediaStoreSaver {

    fun createPhotoOutputOptions(context: Context, isRaw: Boolean): ImageCapture.OutputFileOptions {
        val resolver = context.contentResolver
        val extension = if (isRaw) "dng" else "jpg"
        val mime = if (isRaw) "image/x-adobe-dng" else "image/jpeg"
        val displayName = timestampName("IMG", extension)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val dir = legacyAlbumDir(Environment.DIRECTORY_PICTURES)
                put(MediaStore.MediaColumns.DATA, "${dir.absolutePath}/$displayName")
            }
        }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return ImageCapture.OutputFileOptions.Builder(resolver, collection, values).build()
    }

    fun createVideoOutputOptions(context: Context): MediaStoreOutputOptions {
        val resolver = context.contentResolver
        val displayName = timestampName("VID", "mp4")
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$ALBUM_NAME")
            } else {
                val dir = legacyAlbumDir(Environment.DIRECTORY_MOVIES)
                put(MediaStore.MediaColumns.DATA, "${dir.absolutePath}/$displayName")
            }
        }
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return MediaStoreOutputOptions.Builder(resolver, collection)
            .setContentValues(values)
            .build()
    }

    /** Marks a MediaStore Q+ entry as no longer pending so it becomes visible in galleries. */
    fun finalizePending(context: Context, uri: android.net.Uri?) {
        if (uri == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        runCatching { context.contentResolver.update(uri, values, null, null) }
    }
}
