package com.khalil.music

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khalil.music.data.MusicRepository
import com.khalil.music.data.SpecialPlaylist
import com.khalil.music.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val musicApp = app as MusicApp
    val library = musicApp.library
    val player = musicApp.player

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val sortedTracks = combine(_tracks, library.sort) { t, s -> MusicRepository.sort(t, s) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val albums = _tracks.map(MusicRepository::albums)
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val artists = _tracks.map(MusicRepository::artists)
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val folders = _tracks.map(MusicRepository::folders)
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val trackById = _tracks.map { list -> list.associateBy { it.id } }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private var observing = false
    private var refreshJob: Job? = null
    private var restored = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleRefresh()
    }

    init {
        player.onConnected = { restoreQueue() }
        viewModelScope.launch { library.hideShort.drop(1).collect { refresh() } }
    }

    /** Called once the audio permission is granted. */
    fun startLibrary() {
        if (!observing) {
            observing = true
            getApplication<Application>().contentResolver
                .registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        }
        viewModelScope.launch { refresh() }
    }

    fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(700)
            refresh()
        }
    }

    private suspend fun refresh() {
        val minDuration = if (library.hideShort.value) 30_000L else 0L
        _tracks.value = MusicRepository.loadTracks(getApplication(), minDuration)
        _loaded.value = true
        restoreQueue()
    }

    private fun restoreQueue() {
        if (restored || !_loaded.value) return
        val c = player.controller ?: return
        restored = true
        if (c.mediaItemCount > 0) return
        val saved = library.loadQueue() ?: return
        val byId = trackById.value.ifEmpty { _tracks.value.associateBy { it.id } }
        val currentId = saved.ids.getOrNull(saved.index)
        val list = saved.ids.mapNotNull { byId[it] }
        if (list.isEmpty()) return
        val index = list.indexOfFirst { it.id == currentId }
        player.restore(list, index.coerceAtLeast(0), if (index >= 0) saved.positionMs else 0)
    }

    fun playlistTracks(id: Long): List<Track> {
        val byId = trackById.value
        return when (id) {
            SpecialPlaylist.FAVOURITES -> library.favourites.value.mapNotNull { byId[it] }
            SpecialPlaylist.RECENT -> _tracks.value.sortedByDescending { it.dateAdded }.take(100)
            else -> library.playlists.value.firstOrNull { it.id == id }?.trackIds?.mapNotNull { byId[it] }.orEmpty()
        }
    }

    override fun onCleared() {
        if (observing) getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }
}
