package com.linnan.hayaophoto.crypto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * All photo/video bytes at rest are encrypted with a Keystore-bound master key via
 * Jetpack Security's EncryptedFile (AES256-GCM, HKDF-chunked so large videos stream fine).
 * The key never leaves the device's hardware-backed Keystore, so a copied .db/file pair
 * is unreadable off-device.
 */
class MediaCrypto(private val context: Context) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val mediaRoot: File by lazy { File(context.filesDir, "media").apply { mkdirs() } }

    fun dirForPerson(personId: Long): File =
        File(mediaRoot, personId.toString()).apply { mkdirs() }

    fun newFileName(extension: String): String = "${UUID.randomUUID()}.$extension"

    private fun encryptedFile(target: File): EncryptedFile =
        EncryptedFile.Builder(
            context,
            target,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

    fun writeEncrypted(personId: Long, fileName: String, source: InputStream) {
        val target = File(dirForPerson(personId), fileName)
        if (target.exists()) target.delete()
        encryptedFile(target).openFileOutput().use { out ->
            source.use { input -> input.copyTo(out) }
        }
    }

    fun writeEncryptedBytes(personId: Long, fileName: String, bytes: ByteArray) {
        writeEncrypted(personId, fileName, ByteArrayInputStream(bytes))
    }

    fun openDecryptedStream(personId: Long, fileName: String): InputStream =
        encryptedFile(File(dirForPerson(personId), fileName)).openFileInput()

    fun decryptToBytes(personId: Long, fileName: String): ByteArray =
        openDecryptedStream(personId, fileName).use { it.readBytes() }

    /** Decrypts a video into the app-private cache so Media3 can play it via a plain file
     * Uri. The plaintext lives only in sandboxed cache storage and is removed by
     * [clearPlaybackCache] as soon as playback ends. */
    fun decryptToCacheFile(personId: Long, fileName: String): File {
        val outDir = File(context.cacheDir, "playback").apply { mkdirs() }
        val outFile = File(outDir, "$personId-$fileName")
        openDecryptedStream(personId, fileName).use { input ->
            FileOutputStream(outFile).use { out -> input.copyTo(out) }
        }
        return outFile
    }

    fun clearPlaybackCache() {
        File(context.cacheDir, "playback").deleteRecursively()
    }

    fun deleteMedia(personId: Long, fileName: String?) {
        if (fileName == null) return
        File(dirForPerson(personId), fileName).delete()
    }

    fun deletePersonDir(personId: Long) {
        dirForPerson(personId).deleteRecursively()
    }

    fun decodeBitmap(bytes: ByteArray, reqWidth: Int = 0, reqHeight: Int = 0): Bitmap? {
        if (reqWidth <= 0 || reqHeight <= 0) {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, reqWidth, reqHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    fun jpegBytesFrom(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
