package com.linnan.hayaocamera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a downsampled bitmap for preview/thumbnail purposes only. The original file on disk
 * is never touched or recompressed — this only bounds how much memory a *displayed* copy uses,
 * which is what keeps the gallery grid and viewer from running out of memory on very
 * high-resolution captures.
 */
suspend fun loadDownsampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(maxDimension, maxDimension), null)
            } else {
                decodeSampledLegacy(context, uri, maxDimension)
            }
        }.getOrNull()
    }

private fun decodeSampledLegacy(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, boundsOptions)
    }
    var sampleSize = 1
    var width = boundsOptions.outWidth
    var height = boundsOptions.outHeight
    while (width / (sampleSize * 2) >= maxDimension && height / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOptions)
    }
}
