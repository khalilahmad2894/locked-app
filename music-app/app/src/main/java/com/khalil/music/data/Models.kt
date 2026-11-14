package com.khalil.music.data

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore

private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAdded: Long,
    val folder: String,
    val trackNumber: Int,
) {
    val uri: Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val albumArtUri: Uri get() = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)
}

data class Album(val id: Long, val title: String, val artist: String, val tracks: List<Track>)

data class Artist(val name: String, val tracks: List<Track>, val albumCount: Int)

data class Folder(val path: String, val name: String, val tracks: List<Track>)

data class Playlist(val id: Long, val name: String, val trackIds: List<Long>)

enum class SortOrder(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    DATE_ADDED("Recently added"),
}

object SpecialPlaylist {
    const val FAVOURITES = -1L
    const val RECENT = -2L
}

fun trackUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun List<Track>.totalDurationLabel(): String {
    val minutes = sumOf { it.durationMs } / 60000
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

fun tracksLabel(n: Int) = if (n == 1) "1 track" else "$n tracks"
