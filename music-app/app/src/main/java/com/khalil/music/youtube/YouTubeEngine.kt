package com.khalil.music.youtube

import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Wraps yt-dlp (via youtubedl-android). Files are downloaded into the cache dir, then copied into
 * Music/YouTube through MediaStore so they show up in the library like any other song.
 */
object YouTubeEngine {

    sealed interface EngineState {
        data object Initializing : EngineState
        data class Ready(val version: String?) : EngineState
        data class Failed(val message: String) : EngineState
    }

    enum class AudioFormat(val label: String, val ext: String, val mime: String) {
        M4A("M4A · best quality", "m4a", "audio/mp4"),
        MP3("MP3", "mp3", "audio/mpeg"),
    }

    enum class Status { FETCHING, DOWNLOADING, PROCESSING, SAVING, DONE, FAILED, CANCELLED }

    data class Job(
        val id: String,
        val url: String,
        val format: AudioFormat,
        val title: String? = null,
        val thumbnail: String? = null,
        val uploader: String? = null,
        val status: Status = Status.FETCHING,
        val progress: Float = 0f,
        val etaSeconds: Long = 0,
        val error: String? = null,
        val savedId: Long? = null,
    ) {
        val active: Boolean get() = status <= Status.SAVING
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ready = CompletableDeferred<Boolean>()

    private val _state = MutableStateFlow<EngineState>(EngineState.Initializing)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    // Crops YouTube's 16:9 thumbnail to a square so it looks right as album art.
    private const val SQUARE_THUMB =
        "ThumbnailsConvertor:-qmin 1 -q:v 1 -vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\""

    fun init(app: Application) {
        scope.launch {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                _state.value = EngineState.Ready(YoutubeDL.getInstance().version(app))
                ready.complete(true)
            } catch (e: Throwable) {
                _state.value = EngineState.Failed(e.message ?: e.javaClass.simpleName)
                ready.complete(false)
            }
        }
    }

    fun download(context: Context, url: String, format: AudioFormat) {
        val job = Job(id = UUID.randomUUID().toString().take(8), url = url.trim(), format = format)
        _jobs.update { it + job }
        val app = context.applicationContext
        scope.launch { run(app, job.id) }
    }

    fun retry(context: Context, id: String) {
        edit(id) { it.copy(status = Status.FETCHING, progress = 0f, error = null) }
        val app = context.applicationContext
        scope.launch { run(app, id) }
    }

    fun cancel(id: String) {
        runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
        edit(id) { it.copy(status = Status.CANCELLED) }
    }

    fun dismiss(id: String) = _jobs.update { list -> list.filterNot { it.id == id } }

    fun clearFinished() = _jobs.update { list -> list.filter { it.active } }

    /** Replaces the bundled yt-dlp with the newest build, for when YouTube changes something. */
    suspend fun update(context: Context, nightly: Boolean): String = withContext(Dispatchers.IO) {
        if (!ready.await()) return@withContext "Engine failed to start"
        val channel = if (nightly) YoutubeDL.UpdateChannel.NIGHTLY else YoutubeDL.UpdateChannel.STABLE
        val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, channel)
        val version = YoutubeDL.getInstance().version(context.applicationContext)
        _state.value = EngineState.Ready(version)
        when (status) {
            YoutubeDL.UpdateStatus.DONE -> "Updated to $version"
            YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "Already up to date ($version)"
            else -> "Update finished ($version)"
        }
    }

    private suspend fun run(context: Context, id: String) {
        if (!ready.await()) {
            edit(id) { it.copy(status = Status.FAILED, error = "Download engine failed to start") }
            return
        }
        val dir = File(context.cacheDir, "yt").apply { mkdirs() }
        try {
            val start = get(id) ?: return
            val info = YoutubeDL.getInstance().getInfo(start.url)
            if (get(id)?.status == Status.CANCELLED) return
            edit(id) {
                it.copy(
                    title = info.title ?: it.title,
                    thumbnail = info.thumbnail ?: it.thumbnail,
                    uploader = info.uploader ?: it.uploader,
                    status = Status.DOWNLOADING,
                )
            }

            // Thumbnail embedding is the most fragile step, so fall back to a plain download if it breaks.
            try {
                execute(dir, id, withArtwork = true)
            } catch (e: YoutubeDL.CanceledException) {
                throw e
            } catch (e: Exception) {
                if (get(id)?.status == Status.CANCELLED) return
                cleanup(dir, id)
                edit(id) { it.copy(status = Status.DOWNLOADING, progress = 0f) }
                execute(dir, id, withArtwork = false)
            }

            val job = get(id) ?: return
            val file = dir.listFiles()?.firstOrNull { it.name.startsWith(id) && it.extension == job.format.ext }
                ?: error("Downloaded file not found")
            edit(id) { it.copy(status = Status.SAVING) }
            val savedId = saveToMusic(context, file, job)
            edit(id) { it.copy(status = Status.DONE, progress = 1f, savedId = savedId) }
        } catch (e: YoutubeDL.CanceledException) {
            edit(id) { it.copy(status = Status.CANCELLED) }
        } catch (e: Exception) {
            if (get(id)?.status != Status.CANCELLED) {
                edit(id) { it.copy(status = Status.FAILED, error = readableError(e)) }
            }
        } finally {
            cleanup(dir, id)
        }
    }

    private fun execute(dir: File, id: String, withArtwork: Boolean) {
        val job = get(id) ?: return
        val request = YoutubeDLRequest(job.url).apply {
            addOption("-o", "${dir.absolutePath}/$id.%(ext)s")
            addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")
            addOption("-x")
            addOption("--audio-format", job.format.ext)
            if (job.format == AudioFormat.MP3) addOption("--audio-quality", "0")
            addOption("--embed-metadata")
            // Give every download its own album (unless YouTube Music provides a real one), so each song keeps its own art.
            addOption("--parse-metadata", "%(album,title)s:%(meta_album)s")
            if (withArtwork) {
                addOption("--embed-thumbnail")
                addOption("--convert-thumbnails", "jpg")
                addOption("--ppa", SQUARE_THUMB)
            }
        }
        YoutubeDL.getInstance().execute(request, id) { progress, eta, line ->
            val postProcessing = line.startsWith("[ExtractAudio]") || line.startsWith("[EmbedThumbnail]") ||
                line.startsWith("[Metadata]") || line.startsWith("[ThumbnailsConvertor]") ||
                line.startsWith("[FixupM4a]")
            edit(id) {
                when {
                    it.status == Status.CANCELLED -> it
                    postProcessing -> it.copy(status = Status.PROCESSING)
                    progress >= 0f && it.status == Status.DOWNLOADING ->
                        it.copy(progress = (progress / 100f).coerceIn(0f, 1f), etaSeconds = eta)
                    else -> it
                }
            }
        }
    }

    private fun saveToMusic(context: Context, file: File, job: Job): Long {
        val resolver = context.contentResolver
        val baseName = (job.title ?: job.id).replace(Regex("[\\\\/:*?\"<>|]"), " ").trim().take(120)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$baseName.${job.format.ext}")
            put(MediaStore.Audio.Media.MIME_TYPE, job.format.mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/YouTube")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: error("Could not create file in Music/YouTube")
        try {
            resolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return ContentUris.parseId(uri)
    }

    private fun cleanup(dir: File, id: String) {
        dir.listFiles()?.filter { it.name.startsWith(id) }?.forEach { it.delete() }
    }

    private fun readableError(e: Exception): String {
        val msg = e.message.orEmpty()
        return msg.lines().lastOrNull { it.contains("ERROR") }?.substringAfter("ERROR:")?.trim()
            ?: msg.lines().lastOrNull { it.isNotBlank() }?.take(200)
            ?: e.javaClass.simpleName
    }

    private fun get(id: String): Job? = _jobs.value.firstOrNull { it.id == id }

    private fun edit(id: String, block: (Job) -> Job) =
        _jobs.update { list -> list.map { if (it.id == id) block(it) else it } }
}
