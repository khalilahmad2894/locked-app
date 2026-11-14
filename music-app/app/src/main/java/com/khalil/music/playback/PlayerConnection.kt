package com.khalil.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.khalil.music.data.LibraryStore
import com.khalil.music.data.Track
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueueEntry(val windowIndex: Int, val id: Long, val title: String, val artist: String)

data class PlayerState(
    val connected: Boolean = false,
    val currentId: Long? = null,
    val currentIndex: Int = C.INDEX_UNSET,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    /** Queue in playback order (respects shuffle). */
    val queue: List<QueueEntry> = emptyList(),
    val sleepTimerEndsAt: Long? = null,
)

/** UI-side handle on [PlaybackService]. Lives for the whole process so the sleep timer keeps working. */
class PlayerConnection(private val context: Context, private val library: LibraryStore) {

    private val scope = MainScope()
    private var future: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    var onConnected: (() -> Unit)? = null

    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val queueChanged = events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            )
            publish(rebuildQueue = queueChanged)
            if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_TIMELINE_CHANGED)) {
                saveQueue()
            }
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = runCatching { f.get() }.getOrNull() ?: run { future = null; return@addListener }
            controller = c
            c.addListener(listener)
            publish(rebuildQueue = true)
            onConnected?.invoke()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun publish(rebuildQueue: Boolean) {
        val c = controller ?: return
        val item = c.currentMediaItem
        _state.update { old ->
            old.copy(
                connected = true,
                currentId = item?.mediaId?.toLongOrNull(),
                currentIndex = c.currentMediaItemIndex,
                title = item?.mediaMetadata?.title?.toString().orEmpty(),
                artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
                isPlaying = c.isPlaying,
                durationMs = c.duration.takeIf { it != C.TIME_UNSET } ?: 0,
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                queue = if (rebuildQueue) buildQueue(c) else old.queue,
            )
        }
    }

    private fun buildQueue(c: MediaController): List<QueueEntry> {
        val timeline = c.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val out = ArrayList<QueueEntry>(timeline.windowCount)
        var i = timeline.getFirstWindowIndex(c.shuffleModeEnabled)
        while (i != C.INDEX_UNSET) {
            val item = c.getMediaItemAt(i)
            out += QueueEntry(
                windowIndex = i,
                id = item.mediaId.toLongOrNull() ?: -1,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
            )
            i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
        }
        return out
    }

    private fun saveQueue() {
        val c = controller ?: return
        val ids = (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() }
        library.saveQueue(ids, c.currentMediaItemIndex, c.currentPosition)
    }

    fun position(): Long = controller?.currentPosition ?: 0

    fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.shuffleModeEnabled = shuffle
        c.setMediaItems(tracks.map { it.toMediaItem() }, startIndex.coerceIn(tracks.indices), 0)
        c.prepare()
        c.play()
    }

    fun shuffleAll(tracks: List<Track>) {
        if (tracks.isNotEmpty()) play(tracks, tracks.indices.random(), shuffle = true)
    }

    /** Puts the queue back the way it was without starting playback. */
    fun restore(tracks: List<Track>, index: Int, positionMs: Long) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        c.setMediaItems(tracks.map { it.toMediaItem() }, index.coerceIn(tracks.indices), positionMs)
        c.prepare()
    }

    fun playNext(tracks: List<Track>) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(tracks, 0)
        c.addMediaItems(c.currentMediaItemIndex + 1, tracks.map { it.toMediaItem() })
    }

    fun addToQueue(tracks: List<Track>) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return play(tracks, 0)
        c.addMediaItems(tracks.map { it.toMediaItem() })
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun next() = controller?.seekToNext()

    /** Samsung behaviour: restart the song if more than ~3s in, otherwise go to the previous one. */
    fun previous() = controller?.seekToPrevious()

    fun seekTo(ms: Long) = controller?.seekTo(ms)

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        val dur = c.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, dur))
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    /** off -> all -> one -> off, like Samsung Music. */
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun skipTo(windowIndex: Int) {
        val c = controller ?: return
        c.seekTo(windowIndex, 0)
        c.play()
    }

    fun removeFromQueue(windowIndex: Int) = controller?.removeMediaItem(windowIndex)

    fun moveInQueue(from: Int, to: Int) = controller?.moveMediaItem(from, to)

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            _state.update { it.copy(sleepTimerEndsAt = null) }
            return
        }
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _state.update { it.copy(sleepTimerEndsAt = endsAt) }
        sleepJob = scope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _state.update { it.copy(sleepTimerEndsAt = null) }
        }
    }
}

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(albumArtUri)
            .build()
    )
    .build()
