package com.linnan.hayaophoto.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.linnan.hayaophoto.crypto.MediaCrypto
import com.linnan.hayaophoto.data.MediaItemEntity

/** Decrypts a stored item and writes a plain copy into the device's normal Photos/Videos
 * library via MediaStore, so the user can hand it off outside the encrypted vault. */
object MediaStoreExporter {

    fun export(context: Context, crypto: MediaCrypto, item: MediaItemEntity): Uri? = try {
        exportInternal(context, crypto, item)
    } catch (e: Exception) {
        null
    }

    private fun exportInternal(context: Context, crypto: MediaCrypto, item: MediaItemEntity): Uri? {
        val resolver = context.contentResolver
        val collection = if (item.isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val extension = if (item.isVideo) "mp4" else "jpg"
        val displayName = "hayaophoto_${System.currentTimeMillis()}.$extension"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = if (item.isVideo) "Movies/HayaoPhoto" else "Pictures/HayaoPhoto"
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { out ->
                crypto.openDecryptedStream(item.personId, item.fileName).use { input ->
                    input.copyTo(out)
                }
            } ?: return null
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }
        return uri
    }
}
