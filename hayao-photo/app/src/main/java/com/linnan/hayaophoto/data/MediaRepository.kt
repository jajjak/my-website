package com.linnan.hayaophoto.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.linnan.hayaophoto.crypto.MediaCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MediaRepository(
    private val context: Context,
    private val personDao: PersonDao,
    private val mediaDao: MediaDao,
    private val crypto: MediaCrypto
) {
    fun observePersons(): Flow<List<PersonEntity>> = personDao.observeAll()
    fun observePerson(id: Long): Flow<PersonEntity?> = personDao.observeById(id)
    fun observeMedia(personId: Long): Flow<List<MediaItemEntity>> = mediaDao.observeByPerson(personId)
    fun observeAllMedia(): Flow<List<MediaItemEntity>> = mediaDao.observeAll()

    suspend fun addPerson(name: String, memo: String, profileBytes: ByteArray?): Long =
        withContext(Dispatchers.IO) {
            val id = personDao.insert(PersonEntity(name = name, memo = memo))
            if (profileBytes != null) {
                val fileName = crypto.newFileName("jpg")
                crypto.writeEncryptedBytes(id, fileName, profileBytes)
                personDao.update(personDao.getById(id)!!.copy(profilePhotoFile = fileName))
            }
            id
        }

    suspend fun updatePerson(person: PersonEntity, name: String, memo: String, newProfileBytes: ByteArray?) =
        withContext(Dispatchers.IO) {
            var updated = person.copy(name = name, memo = memo)
            if (newProfileBytes != null) {
                crypto.deleteMedia(person.id, person.profilePhotoFile)
                val fileName = crypto.newFileName("jpg")
                crypto.writeEncryptedBytes(person.id, fileName, newProfileBytes)
                updated = updated.copy(profilePhotoFile = fileName)
            }
            personDao.update(updated)
        }

    suspend fun deletePerson(person: PersonEntity) = withContext(Dispatchers.IO) {
        personDao.delete(person)
        crypto.deletePersonDir(person.id)
    }

    suspend fun addMediaFromFile(personId: Long, sourceFile: File, isVideo: Boolean, mimeType: String): Long =
        withContext(Dispatchers.IO) {
            val ext = if (isVideo) "mp4" else "jpg"
            val fileName = crypto.newFileName(ext)
            sourceFile.inputStream().use { crypto.writeEncrypted(personId, fileName, it) }

            val thumbBytes = generateThumbnailFromPath(sourceFile.absolutePath, isVideo)
            val thumbFileName = thumbBytes?.let {
                crypto.newFileName("jpg").also { name -> crypto.writeEncryptedBytes(personId, name, it) }
            }
            val duration = if (isVideo) durationFromPath(sourceFile.absolutePath) else 0L

            mediaDao.insert(
                MediaItemEntity(
                    personId = personId,
                    fileName = fileName,
                    thumbFileName = thumbFileName,
                    isVideo = isVideo,
                    mimeType = mimeType,
                    durationMs = duration,
                    sizeBytes = sourceFile.length()
                )
            )
        }

    suspend fun addMediaFromUri(personId: Long, uri: Uri, isVideo: Boolean, mimeType: String): Long =
        withContext(Dispatchers.IO) {
            val ext = if (isVideo) "mp4" else "jpg"
            val fileName = crypto.newFileName(ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                crypto.writeEncrypted(personId, fileName, input)
            } ?: throw IOException("import元を開けません: $uri")
            val size = querySize(uri)

            val thumbBytes = generateThumbnailFromUri(uri, isVideo)
            val thumbFileName = thumbBytes?.let {
                crypto.newFileName("jpg").also { name -> crypto.writeEncryptedBytes(personId, name, it) }
            }
            val duration = if (isVideo) durationFromUri(uri) else 0L

            mediaDao.insert(
                MediaItemEntity(
                    personId = personId,
                    fileName = fileName,
                    thumbFileName = thumbFileName,
                    isVideo = isVideo,
                    mimeType = mimeType,
                    durationMs = duration,
                    sizeBytes = size
                )
            )
        }

    suspend fun deleteMediaItems(items: List<MediaItemEntity>) = withContext(Dispatchers.IO) {
        items.forEach {
            crypto.deleteMedia(it.personId, it.fileName)
            crypto.deleteMedia(it.personId, it.thumbFileName)
        }
        mediaDao.deleteByIds(items.map { it.id })
    }

    private fun generateThumbnailFromPath(path: String, isVideo: Boolean): ByteArray? = try {
        val bitmap = if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(path)
                retriever.getFrameAtTime(0)
            } finally {
                retriever.release()
            }
        } else {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(path, opts)
        }
        bitmap?.let { crypto.jpegBytesFrom(scaleDown(it, THUMB_MAX_DIMENSION)) }
    } catch (e: Exception) {
        null
    }

    private fun generateThumbnailFromUri(uri: Uri, isVideo: Boolean): ByteArray? = try {
        val bitmap = if (isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(0)
            } finally {
                retriever.release()
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeStream(input, null, opts)
            }
        }
        bitmap?.let { crypto.jpegBytesFrom(scaleDown(it, THUMB_MAX_DIMENSION)) }
    } catch (e: Exception) {
        null
    }

    private fun durationFromPath(path: String): Long = try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(path)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    } catch (e: Exception) {
        0L
    }

    private fun durationFromUri(uri: Uri): Long = try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    } catch (e: Exception) {
        0L
    }

    private fun querySize(uri: Uri): Long = try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx) else 0L
        } ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val ratio = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (ratio >= 1f) return bitmap
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    companion object {
        private const val THUMB_MAX_DIMENSION = 480
    }
}
