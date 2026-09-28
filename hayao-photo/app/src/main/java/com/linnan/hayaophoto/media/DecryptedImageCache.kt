package com.linnan.hayaophoto.media

import android.graphics.Bitmap
import androidx.collection.LruCache

/** Small in-memory cache so scrolling a gallery grid doesn't re-decrypt+decode the same
 * thumbnail on every recomposition. Keyed by "personId/fileName". Never persisted. */
object DecryptedImageCache {
    private val cache = object : LruCache<String, Bitmap>(96) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    fun clear() = cache.evictAll()
}
