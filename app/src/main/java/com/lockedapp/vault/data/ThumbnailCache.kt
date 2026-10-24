package com.lockedapp.vault.data

import android.graphics.Bitmap
import android.util.LruCache

/** In-memory only — thumbnails are never written to disk unencrypted. */
object ThumbnailCache {
    private val cache = LruCache<Long, Bitmap>(80)

    fun get(id: Long): Bitmap? = cache.get(id)

    fun put(id: Long, bitmap: Bitmap) {
        cache.put(id, bitmap)
    }

    fun remove(id: Long) {
        cache.remove(id)
    }
}
