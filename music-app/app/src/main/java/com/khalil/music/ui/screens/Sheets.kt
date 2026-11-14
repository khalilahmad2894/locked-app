package com.khalil.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khalil.music.MusicViewModel
import com.khalil.music.data.Track
import com.khalil.music.data.tracksLabel
import com.khalil.music.ui.components.AlbumArt
import com.khalil.music.ui.components.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsSheet(
    vm: MusicViewModel,
    track: Track,
    playlistId: Long?,
    onDismiss: () -> Unit,
    navigate: (Screen) -> Unit,
    addToPlaylist: (List<Track>) -> Unit,
    delete: (Track) -> Unit,
) {
    val favourites by vm.library.favourites.collectAsState()
    val isFav = track.id in favourites
    val albums by vm.albums.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(track.uri, Modifier.size(52.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SheetItem(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") {
                vm.player.playNext(listOf(track)); onDismiss()
            }
            SheetItem(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") {
                vm.player.addToQueue(listOf(track)); onDismiss()
            }
            SheetItem(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist") {
                onDismiss(); addToPlaylist(listOf(track))
            }
            SheetItem(
                if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (isFav) "Remove from favourites" else "Add to favourites",
                tint = if (isFav) Color(0xFFFF4B6E) else null,
            ) {
                vm.library.toggleFavourite(track.id); onDismiss()
            }
            if (playlistId != null && playlistId > 0) {
                SheetItem(Icons.Filled.RemoveCircleOutline, "Remove from playlist") {
                    vm.library.removeFromPlaylist(playlistId, track.id); onDismiss()
                }
            }
            if (albums.any { it.id == track.albumId }) {
                SheetItem(Icons.Filled.Album, "Go to album") {
                    onDismiss(); navigate(Screen.AlbumDetail(track.albumId))
                }
            }
            SheetItem(Icons.Filled.Person, "Go to artist") {
                onDismiss(); navigate(Screen.ArtistDetail(track.artist))
            }
            SheetItem(Icons.Filled.Delete, "Delete from phone") {
                onDismiss(); delete(track)
            }
        }
    }
}

@Composable
private fun SheetItem(icon: ImageVector, label: String, tint: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint ?: MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AddToPlaylistDialog(vm: MusicViewModel, tracks: List<Track>, onDismiss: () -> Unit) {
    val playlists by vm.library.playlists.collectAsState()
    var creating by remember { mutableStateOf(false) }
    if (creating) {
        NameDialog(
            title = "Create playlist",
            initial = "",
            confirmLabel = "Create",
            onDismiss = onDismiss,
            onConfirm = { name ->
                vm.library.createPlaylist(name, tracks.map { it.id })
                onDismiss()
            },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { creating = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text("Create playlist", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                items(playlists, key = { it.id }) { p ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.library.addToPlaylist(p.id, tracks.map { it.id })
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(p.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            tracksLabel(p.trackIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Playlist name") },
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
