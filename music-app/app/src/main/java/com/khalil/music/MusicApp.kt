package com.khalil.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.khalil.music.data.LibraryStore
import com.khalil.music.playback.PlayerConnection
import com.khalil.music.ui.components.ArtFetcher
import com.khalil.music.ui.components.ArtKeyer
import com.khalil.music.youtube.YouTubeEngine

class MusicApp : Application(), ImageLoaderFactory {

    lateinit var library: LibraryStore
        private set
    lateinit var player: PlayerConnection
        private set

    override fun onCreate() {
        super.onCreate()
        library = LibraryStore(this)
        player = PlayerConnection(this, library)
        player.connect()
        YouTubeEngine.init(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(ArtKeyer())
            add(ArtFetcher.Factory(this@MusicApp))
        }
        .crossfade(true)
        .build()
}
