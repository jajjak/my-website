package com.linnan.hayaophoto.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.linnan.hayaophoto.AppGraph
import com.linnan.hayaophoto.media.DecryptedImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decrypts a stored image (or its thumbnail) off the main thread and renders it, with an
 * in-memory cache so re-composition doesn't repeatedly hit the Keystore-backed cipher. */
@Composable
fun DecryptedImage(
    personId: Long,
    fileName: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxDimension: Int = 0
) {
    val cacheKey = "$personId/$fileName/$maxDimension"
    val bitmapState = produceState(initialValue = DecryptedImageCache.get(cacheKey), key1 = cacheKey) {
        if (value == null && fileName != null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val bytes = AppGraph.mediaCrypto.decryptToBytes(personId, fileName)
                    val bmp = if (maxDimension > 0) {
                        AppGraph.mediaCrypto.decodeBitmap(bytes, maxDimension, maxDimension)
                    } else {
                        AppGraph.mediaCrypto.decodeBitmap(bytes)
                    }
                    bmp?.also { DecryptedImageCache.put(cacheKey, it) }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmapState.value?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}
