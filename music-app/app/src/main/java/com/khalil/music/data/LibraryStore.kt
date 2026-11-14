package com.khalil.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Favourites, playlists, settings and the last queue, kept in SharedPreferences. */
class LibraryStore(context: Context) {

    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    private val _favourites = MutableStateFlow(readIds("favourites"))
    val favourites: StateFlow<List<Long>> = _favourites.asStateFlow()

    private val _playlists = MutableStateFlow(readPlaylists())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _hideShort = MutableStateFlow(prefs.getBoolean("hide_short", true))
    val hideShort: StateFlow<Boolean> = _hideShort.asStateFlow()

    private val _sort = MutableStateFlow(
        runCatching { SortOrder.valueOf(prefs.getString("sort", null)!!) }.getOrDefault(SortOrder.TITLE)
    )
    val sort: StateFlow<SortOrder> = _sort.asStateFlow()

    var ytNightly: Boolean
        get() = prefs.getBoolean("yt_nightly", true)
        set(value) = prefs.edit().putBoolean("yt_nightly", value).apply()

    fun toggleFavourite(id: Long) {
        _favourites.update { if (id in it) it - id else listOf(id) + it }
        writeIds("favourites", _favourites.value)
    }

    fun setHideShort(value: Boolean) {
        _hideShort.value = value
        prefs.edit().putBoolean("hide_short", value).apply()
    }

    fun setSort(order: SortOrder) {
        _sort.value = order
        prefs.edit().putString("sort", order.name).apply()
    }

    fun createPlaylist(name: String, trackIds: List<Long> = emptyList()): Long {
        val id = System.currentTimeMillis()
        _playlists.update { it + Playlist(id, name, trackIds.distinct()) }
        writePlaylists()
        return id
    }

    fun renamePlaylist(id: Long, name: String) = editPlaylist(id) { it.copy(name = name) }

    fun deletePlaylist(id: Long) {
        _playlists.update { list -> list.filterNot { it.id == id } }
        writePlaylists()
    }

    fun addToPlaylist(id: Long, trackIds: List<Long>) =
        editPlaylist(id) { p -> p.copy(trackIds = (p.trackIds + trackIds).distinct()) }

    fun removeFromPlaylist(id: Long, trackId: Long) =
        editPlaylist(id) { p -> p.copy(trackIds = p.trackIds - trackId) }

    fun saveQueue(ids: List<Long>, index: Int, positionMs: Long) {
        prefs.edit()
            .putString("queue_ids", ids.joinToString(","))
            .putInt("queue_index", index)
            .putLong("queue_pos", positionMs)
            .apply()
    }

    fun loadQueue(): SavedQueue? {
        val ids = readIds("queue_ids")
        if (ids.isEmpty()) return null
        return SavedQueue(ids, prefs.getInt("queue_index", 0), prefs.getLong("queue_pos", 0))
    }

    data class SavedQueue(val ids: List<Long>, val index: Int, val positionMs: Long)

    private fun editPlaylist(id: Long, block: (Playlist) -> Playlist) {
        _playlists.update { list -> list.map { if (it.id == id) block(it) else it } }
        writePlaylists()
    }

    private fun readIds(key: String): List<Long> =
        prefs.getString(key, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    private fun writeIds(key: String, ids: List<Long>) {
        prefs.edit().putString(key, ids.joinToString(",")).apply()
    }

    private fun readPlaylists(): List<Playlist> = runCatching {
        val arr = JSONArray(prefs.getString("playlists", "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ids = o.getJSONArray("tracks")
            Playlist(o.getLong("id"), o.getString("name"), (0 until ids.length()).map { ids.getLong(it) })
        }
    }.getOrDefault(emptyList())

    private fun writePlaylists() {
        val arr = JSONArray()
        _playlists.value.forEach { p ->
            arr.put(JSONObject().put("id", p.id).put("name", p.name).put("tracks", JSONArray(p.trackIds)))
        }
        prefs.edit().putString("playlists", arr.toString()).apply()
    }
}
