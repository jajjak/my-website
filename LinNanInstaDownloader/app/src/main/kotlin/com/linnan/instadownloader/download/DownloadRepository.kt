package com.linnan.instadownloader.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.linnan.instadownloader.data.model.FailureReason
import com.linnan.instadownloader.data.model.MediaItem
import com.linnan.instadownloader.data.model.MediaType
import com.linnan.instadownloader.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Streams the real bytes of a [MediaItem] straight into MediaStore (scoped storage,
 * API 29+). An HTML error page mislabeled as a photo is caught by the content-type
 * check below and reported as a failure -- it is never written to disk as a ".jpg".
 */
class DownloadRepository(private val context: Context) {

    private class DownloadValidationException(val reason: FailureReason) : IOException()

    fun download(item: MediaItem, shortcode: String): Flow<DownloadState> = flow {
        emit(DownloadState.Progress(0L, item.fileSizeBytes))

        val request = Request.Builder()
            .url(item.downloadUrl)
            .header("User-Agent", NetworkModule.MOBILE_USER_AGENT)
            .build()

        var createdUri: Uri? = null
        try {
            NetworkModule.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw DownloadValidationException(
                        if (response.code == 404) FailureReason.NOT_FOUND else FailureReason.MEDIA_UNAVAILABLE
                    )
                }

                val body = response.body ?: throw DownloadValidationException(FailureReason.MEDIA_UNAVAILABLE)
                val contentType = response.header("Content-Type").orEmpty()
                val expectedPrefix = if (item.type == MediaType.VIDEO) "video/" else "image/"
                if (!contentType.startsWith(expectedPrefix)) {
                    throw DownloadValidationException(FailureReason.MEDIA_UNAVAILABLE)
                }

                val totalBytes = response.header("Content-Length")?.toLongOrNull()
                    ?: item.fileSizeBytes

                val extension = extensionFor(contentType, item.type)
                val displayName = buildFileName(shortcode, item.index, extension)

                val uri = createMediaStoreEntry(displayName, contentType, item.type)
                createdUri = uri
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw DownloadValidationException(FailureReason.UNKNOWN)

                var totalDownloaded = 0L
                outputStream.use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            totalDownloaded += read
                            emit(DownloadState.Progress(totalDownloaded, totalBytes))
                        }
                    }
                }

                markComplete(uri)
                emit(DownloadState.Completed(uri, displayName, item.type, contentType, totalDownloaded))
            }
        } catch (e: DownloadValidationException) {
            createdUri?.let { context.contentResolver.delete(it, null, null) }
            emit(DownloadState.Failed(e.reason))
        } catch (e: IOException) {
            createdUri?.let { context.contentResolver.delete(it, null, null) }
            emit(DownloadState.Failed(FailureReason.NETWORK_ERROR))
        }
    }.flowOn(Dispatchers.IO)

    private fun extensionFor(contentType: String, type: MediaType): String = when {
        contentType.contains("png") -> "png"
        contentType.contains("webp") -> "webp"
        contentType.contains("mp4") -> "mp4"
        contentType.contains("jpeg") || contentType.contains("jpg") -> "jpg"
        type == MediaType.VIDEO -> "mp4"
        else -> "jpg"
    }

    private fun buildFileName(shortcode: String, index: Int, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "linnan_insta_${shortcode}_${index + 1}_$timestamp.$extension"
    }

    private fun createMediaStoreEntry(displayName: String, mimeType: String, type: MediaType): Uri {
        val collection: Uri
        val relativePath: String
        if (type == MediaType.VIDEO) {
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            relativePath = "${Environment.DIRECTORY_MOVIES}/LinNan Insta"
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            relativePath = "${Environment.DIRECTORY_PICTURES}/LinNan Insta"
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        return context.contentResolver.insert(collection, values)
            ?: throw DownloadValidationException(FailureReason.UNKNOWN)
    }

    private fun markComplete(uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        context.contentResolver.update(uri, values, null, null)
    }
}
