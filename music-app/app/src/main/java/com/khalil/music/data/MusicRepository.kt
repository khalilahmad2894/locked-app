package com.khalil.music.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MusicRepository {

    suspend fun loadTracks(context: Context, minDurationMs: Long): List<Track> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TRACK,
        )
        val selection = "${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND " +
            "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND " +
            "${MediaStore.Audio.Media.IS_ALARM} = 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val result = ArrayList<Track>()
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(minDurationMs.toString()),
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            while (c.moveToNext()) {
                val displayName = c.getString(nameCol).orEmpty()
                val artist = c.getString(artistCol)
                result += Track(
                    id = c.getLong(idCol),
                    title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.'),
                    artist = if (artist.isNullOrBlank() || artist == "<unknown>") "Unknown artist" else artist,
                    album = c.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown album",
                    albumId = c.getLong(albumIdCol),
                    durationMs = c.getLong(durCol),
                    dateAdded = c.getLong(dateCol),
                    folder = c.getString(pathCol).orEmpty(),
                    trackNumber = c.getInt(trackCol),
                )
            }
        }
        result
    }

    fun albums(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumId }.map { (id, list) ->
            Album(
                id = id,
                title = list.first().album,
                artist = list.groupingBy { it.artist }.eachCount().maxBy { it.value }.key,
                tracks = list.sortedWith(compareBy<Track>({ it.trackNumber }, { it.title.lowercase() })),
            )
        }.sortedBy { it.title.lowercase() }

    fun artists(tracks: List<Track>): List<Artist> =
        tracks.groupBy { it.artist }.map { (name, list) ->
            Artist(name, list.sortedBy { it.title.lowercase() }, list.distinctBy { it.albumId }.size)
        }.sortedBy { it.name.lowercase() }

    fun folders(tracks: List<Track>): List<Folder> =
        tracks.groupBy { it.folder }.map { (path, list) ->
            val name = path.trimEnd('/').substringAfterLast('/').ifEmpty { "Internal storage" }
            Folder(path, name, list.sortedBy { it.title.lowercase() })
        }.sortedBy { it.name.lowercase() }

    fun sort(tracks: List<Track>, order: SortOrder): List<Track> = when (order) {
        SortOrder.TITLE -> tracks.sortedBy { it.title.lowercase() }
        SortOrder.ARTIST -> tracks.sortedWith(compareBy<Track>({ it.artist.lowercase() }, { it.title.lowercase() }))
        SortOrder.DATE_ADDED -> tracks.sortedByDescending { it.dateAdded }
    }
}
