package com.khalil.music.ui.components

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Embedded artwork for a single track, read through MediaStore's thumbnail API. */
data class ArtRequest(val uri: Uri, val size: Int = 256)

class ArtFetcher(private val data: ArtRequest, private val context: Context) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val bitmap = withContext(Dispatchers.IO) {
            context.contentResolver.loadThumbnail(data.uri, Size(data.size, data.size), null)
        }
        return DrawableResult(BitmapDrawable(context.resources, bitmap), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(private val context: Context) : Fetcher.Factory<ArtRequest> {
        override fun create(data: ArtRequest, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtFetcher(data, context)
    }
}

class ArtKeyer : Keyer<ArtRequest> {
    override fun key(data: ArtRequest, options: Options): String = "${data.uri}#${data.size}"
}
