package com.khalil.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khalil.music.MusicViewModel
import com.khalil.music.data.Album
import com.khalil.music.data.SpecialPlaylist
import com.khalil.music.data.Track
import com.khalil.music.data.totalDurationLabel
import com.khalil.music.data.tracksLabel
import com.khalil.music.ui.components.AlbumArt
import com.khalil.music.ui.components.EmptyState
import com.khalil.music.ui.components.LocalActions
import com.khalil.music.ui.components.Screen
import com.khalil.music.ui.components.TopBar
import com.khalil.music.ui.components.TrackRow

@Composable
fun AlbumScreen(vm: MusicViewModel, albumId: Long) {
    val albums by vm.albums.collectAsState()
    val album = albums.firstOrNull { it.id == albumId } ?: return
    CollectionScreen(
        vm = vm,
        title = album.title,
        subtitle = album.artist,
        tracks = album.tracks,
        header = { HeaderArt(album.tracks.first(), RoundedCornerShape(16.dp)) },
        numbered = true,
    )
}

@Composable
fun ArtistScreen(vm: MusicViewModel, name: String) {
    val actions = LocalActions.current
    val artists by vm.artists.collectAsState()
    val albums by vm.albums.collectAsState()
    val artist = artists.firstOrNull { it.name == name } ?: return
    val artistAlbums = remember(albums, artist) { albums.filter { a -> a.tracks.any { it.artist == name } } }
    CollectionScreen(
        vm = vm,
        title = artist.name,
        subtitle = if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums",
        tracks = artist.tracks,
        header = { HeaderArt(artist.tracks.first(), CircleShape) },
        extra = {
            if (artistAlbums.size > 1) {
                Text(
                    "Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(artistAlbums, key = { it.id }) { a: Album ->
                        AlbumCard(a, Modifier.width(130.dp)) { actions.navigate(Screen.AlbumDetail(a.id)) }
                    }
                }
                Text(
                    "Tracks",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                )
            }
        },
    )
}

@Composable
fun FolderScreen(vm: MusicViewModel, path: String) {
    val folders by vm.folders.collectAsState()
    val folder = folders.firstOrNull { it.path == path } ?: return
    CollectionScreen(
        vm = vm,
        title = folder.name,
        subtitle = "/" + folder.path.trimEnd('/'),
        tracks = folder.tracks,
        header = { HeaderIcon(Icons.Filled.Folder, Color(0xFFF5A623)) },
    )
}

@Composable
fun PlaylistScreen(vm: MusicViewModel, id: Long) {
    val actions = LocalActions.current
    val playlists by vm.library.playlists.collectAsState()
    val favourites by vm.library.favourites.collectAsState()
    val byId by vm.trackById.collectAsState()
    val tracks = remember(id, playlists, favourites, byId) { vm.playlistTracks(id) }
    val playlist = playlists.firstOrNull { it.id == id }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val (title, icon, color) = when (id) {
        SpecialPlaylist.FAVOURITES -> Triple("Favourites", Icons.Filled.Favorite, Color(0xFFFF4B6E))
        SpecialPlaylist.RECENT -> Triple("Recently added", Icons.Filled.History, Color(0xFF00B89F))
        else -> Triple(playlist?.name ?: return, Icons.AutoMirrored.Filled.QueueMusic, MaterialTheme.colorScheme.primary)
    }

    CollectionScreen(
        vm = vm,
        title = title,
        subtitle = null,
        tracks = tracks,
        playlistId = id,
        header = {
            val first = tracks.firstOrNull()
            if (playlist != null && first != null) HeaderArt(first, RoundedCornerShape(16.dp)) else HeaderIcon(icon, color)
        },
        menu = if (playlist == null) null else { close ->
            DropdownMenuItem(text = { Text("Rename") }, onClick = { close(); renaming = true })
            DropdownMenuItem(text = { Text("Delete playlist") }, onClick = { close(); confirmDelete = true })
        },
    )

    if (renaming && playlist != null) {
        NameDialog("Rename playlist", playlist.name, "Rename", { renaming = false }) {
            vm.library.renamePlaylist(playlist.id, it)
            renaming = false
        }
    }
    if (confirmDelete && playlist != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${playlist.name}\"?") },
            text = { Text("The songs stay on your phone. Only the playlist is removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.library.deletePlaylist(playlist.id)
                    actions.back()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CollectionScreen(
    vm: MusicViewModel,
    title: String,
    subtitle: String?,
    tracks: List<Track>,
    header: @Composable () -> Unit,
    numbered: Boolean = false,
    playlistId: Long? = null,
    extra: @Composable () -> Unit = {},
    menu: (@Composable (close: () -> Unit) -> Unit)? = null,
) {
    val actions = LocalActions.current
    val state by vm.player.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "", onBack = actions.back) {
            if (menu != null) {
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = actions.bottomPadding + 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    header()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(subtitle, tracksLabel(tracks.size), tracks.totalDurationLabel()).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { vm.player.play(tracks, 0) },
                            enabled = tracks.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.width(140.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                        OutlinedButton(
                            onClick = { vm.player.shuffleAll(tracks) },
                            enabled = tracks.isNotEmpty(),
                            modifier = Modifier.width(140.dp),
                        ) {
                            Icon(Icons.Filled.Shuffle, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            item { extra() }
            if (tracks.isEmpty()) {
                item { EmptyState(Icons.AutoMirrored.Filled.QueueMusic, "No songs yet", "Use \"Add to playlist\" from any song's menu") }
            }
            itemsIndexed(tracks, key = { _, t -> t.id }) { i, t ->
                TrackRow(
                    track = t,
                    isCurrent = t.id == state.currentId,
                    onClick = { vm.player.play(tracks, i) },
                    onMore = { actions.showTrackMenu(t, playlistId) },
                    leadingNumber = if (numbered) i + 1 else null,
                )
            }
        }
    }
}

@Composable
private fun HeaderArt(track: Track, shape: Shape) {
    AlbumArt(track.uri, Modifier.size(200.dp), size = 600, shape = shape, iconSize = 64.dp)
}

@Composable
private fun HeaderIcon(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(72.dp))
    }
}

@Composable
fun SearchScreen(vm: MusicViewModel) {
    val actions = LocalActions.current
    val tracks by vm.tracks.collectAsState()
    val albums by vm.albums.collectAsState()
    val artists by vm.artists.collectAsState()
    val state by vm.player.state.collectAsState()
    var query by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val q = query.trim().lowercase()
    val matchTracks = remember(q, tracks) {
        if (q.isEmpty()) emptyList() else tracks.filter { q in it.title.lowercase() || q in it.artist.lowercase() || q in it.album.lowercase() }
    }
    val matchAlbums = remember(q, albums) { if (q.isEmpty()) emptyList() else albums.filter { q in it.title.lowercase() } }
    val matchArtists = remember(q, artists) { if (q.isEmpty()) emptyList() else artists.filter { q in it.name.lowercase() } }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "", onBack = actions.back)
        androidx.compose.material3.TextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search songs, albums, artists") },
            shape = RoundedCornerShape(28.dp),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focus),
        )
        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = actions.bottomPadding + 8.dp)) {
            if (matchArtists.isNotEmpty()) {
                item { SectionTitle("Artists") }
                items(matchArtists.take(5), key = { "ar" + it.name }) { a ->
                    com.khalil.music.ui.components.ListRow(
                        title = a.name,
                        subtitle = tracksLabel(a.tracks.size),
                        onClick = { actions.navigate(Screen.ArtistDetail(a.name)) },
                        leading = { AlbumArt(a.tracks.first().uri, Modifier.size(48.dp), shape = CircleShape) },
                    )
                }
            }
            if (matchAlbums.isNotEmpty()) {
                item { SectionTitle("Albums") }
                items(matchAlbums.take(5), key = { "al" + it.id }) { a ->
                    com.khalil.music.ui.components.ListRow(
                        title = a.title,
                        subtitle = a.artist,
                        onClick = { actions.navigate(Screen.AlbumDetail(a.id)) },
                        leading = { AlbumArt(a.tracks.first().uri, Modifier.size(48.dp)) },
                    )
                }
            }
            if (matchTracks.isNotEmpty()) {
                item { SectionTitle("Tracks") }
                itemsIndexed(matchTracks, key = { _, t -> "t" + t.id }) { i, t ->
                    TrackRow(
                        track = t,
                        isCurrent = t.id == state.currentId,
                        onClick = { vm.player.play(matchTracks, i) },
                        onMore = { actions.showTrackMenu(t, null) },
                    )
                }
            }
            if (q.isNotEmpty() && matchTracks.isEmpty() && matchAlbums.isEmpty() && matchArtists.isEmpty()) {
                item { EmptyState(Icons.Filled.MoreVert, "No results for \"$query\"") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
    )
}
