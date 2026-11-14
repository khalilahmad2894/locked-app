package com.khalil.music.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khalil.music.MusicViewModel
import com.khalil.music.data.Album
import com.khalil.music.data.SortOrder
import com.khalil.music.data.SpecialPlaylist
import com.khalil.music.data.Track
import com.khalil.music.data.tracksLabel
import com.khalil.music.ui.components.AlbumArt
import com.khalil.music.ui.components.EmptyState
import com.khalil.music.ui.components.IconTile
import com.khalil.music.ui.components.ListRow
import com.khalil.music.ui.components.LocalActions
import com.khalil.music.ui.components.Screen
import com.khalil.music.ui.components.TrackRow
import com.khalil.music.ui.theme.YouTubeRed
import kotlinx.coroutines.launch

private val TABS = listOf("Tracks", "Playlists", "Albums", "Artists", "Folders")

@Composable
fun LibraryScreen(vm: MusicViewModel) {
    val actions = LocalActions.current
    val tracks by vm.sortedTracks.collectAsState()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { TABS.size }
    val density = LocalDensity.current

    // One UI style header: big centred title that collapses into a toolbar as you scroll.
    val maxHeader = 240.dp
    val minHeader = 60.dp
    val rangePx = with(density) { (maxHeader - minHeader).toPx() }
    var offset by rememberSaveable { mutableFloatStateOf(0f) }
    val connection = remember(rangePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0) return Offset.Zero
                val new = (offset + available.y).coerceIn(-rangePx, 0f)
                val consumed = new - offset
                offset = new
                return Offset(0f, consumed)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0) return Offset.Zero
                val new = (offset + available.y).coerceIn(-rangePx, 0f)
                val used = new - offset
                offset = new
                return Offset(0f, used)
            }
        }
    }
    val fraction = if (rangePx == 0f) 0f else -offset / rangePx

    Column(
        Modifier
            .fillMaxSize()
            .nestedScroll(connection)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(with(density) { (maxHeader.toPx() + offset).toDp() })
        ) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(bottom = minHeader)
                    .graphicsLayer { alpha = (1f - fraction * 1.8f).coerceIn(0f, 1f) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Music", fontSize = 38.sp, fontWeight = FontWeight.Normal)
                Spacer(Modifier.height(6.dp))
                Text(
                    tracksLabel(tracks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(minHeader)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Music",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                        .graphicsLayer { alpha = ((fraction - 0.6f) / 0.4f).coerceIn(0f, 1f) },
                )
                IconButton(onClick = { actions.navigate(Screen.YouTube()) }) {
                    Icon(Icons.Filled.SmartDisplay, "YouTube download", tint = YouTubeRed)
                }
                IconButton(onClick = { actions.navigate(Screen.Search) }) {
                    Icon(Icons.Filled.Search, "Search")
                }
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Download from YouTube") },
                            onClick = { menu = false; actions.navigate(Screen.YouTube()) },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { menu = false; actions.navigate(Screen.Settings) },
                        )
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.background,
            divider = {},
            indicator = { positions ->
                if (pagerState.currentPage < positions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[pagerState.currentPage]).padding(horizontal = 16.dp),
                        height = 2.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
        ) {
            TABS.forEachIndexed { i, title ->
                val selected = pagerState.currentPage == i
                Tab(
                    selected = selected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    selectedContentColor = MaterialTheme.colorScheme.onBackground,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            title,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> TracksTab(vm, tracks)
                1 -> PlaylistsTab(vm)
                2 -> AlbumsTab(vm)
                3 -> ArtistsTab(vm)
                else -> FoldersTab(vm)
            }
        }
    }
}

@Composable
private fun TracksTab(vm: MusicViewModel, tracks: List<Track>) {
    val actions = LocalActions.current
    val state by vm.player.state.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val sort by vm.library.sort.collectAsState()
    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(bottom = actions.bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .clickable(enabled = tracks.isNotEmpty()) { vm.player.shuffleAll(tracks) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Shuffle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    tracksLabel(tracks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var sortMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                trailingIcon = { if (order == sort) Icon(Icons.Filled.Check, null) },
                                onClick = { sortMenu = false; vm.library.setSort(order) },
                            )
                        }
                    }
                }
            }
        }
        if (loaded && tracks.isEmpty()) {
            item { EmptyState(Icons.Filled.LibraryMusic, "No music found", "Songs on your phone will show up here") }
        }
        itemsIndexed(tracks, key = { _, t -> t.id }) { i, t ->
            TrackRow(
                track = t,
                isCurrent = t.id == state.currentId,
                onClick = { vm.player.play(tracks, i) },
                onMore = { actions.showTrackMenu(t, null) },
            )
        }
    }
}

@Composable
private fun PlaylistsTab(vm: MusicViewModel) {
    val actions = LocalActions.current
    val playlists by vm.library.playlists.collectAsState()
    val favourites by vm.library.favourites.collectAsState()
    val byId by vm.trackById.collectAsState()
    val tracks by vm.tracks.collectAsState()
    var creating by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    LazyColumn(
        contentPadding = PaddingValues(top = 4.dp, bottom = actions.bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ListRow(
                title = "Create playlist",
                subtitle = null,
                onClick = { creating = true },
                leading = { IconTile(Icons.Filled.Add, accent) },
            )
        }
        item {
            ListRow(
                title = "Favourites",
                subtitle = tracksLabel(favourites.count { it in byId }),
                onClick = { actions.navigate(Screen.PlaylistDetail(SpecialPlaylist.FAVOURITES)) },
                leading = { IconTile(Icons.Filled.Favorite, androidx.compose.ui.graphics.Color(0xFFFF4B6E)) },
            )
        }
        item {
            ListRow(
                title = "Recently added",
                subtitle = tracksLabel(minOf(100, tracks.size)),
                onClick = { actions.navigate(Screen.PlaylistDetail(SpecialPlaylist.RECENT)) },
                leading = { IconTile(Icons.Filled.History, androidx.compose.ui.graphics.Color(0xFF00B89F)) },
            )
        }
        items(playlists, key = { it.id }) { p ->
            val first = p.trackIds.firstNotNullOfOrNull { byId[it] }
            ListRow(
                title = p.name,
                subtitle = tracksLabel(p.trackIds.count { it in byId }),
                onClick = { actions.navigate(Screen.PlaylistDetail(p.id)) },
                leading = {
                    if (first != null) AlbumArt(first.uri, Modifier.size(48.dp), shape = RoundedCornerShape(10.dp))
                    else IconTile(Icons.AutoMirrored.Filled.QueueMusic, accent)
                },
            )
        }
    }

    if (creating) {
        NameDialog(
            title = "Create playlist",
            initial = "",
            confirmLabel = "Create",
            onDismiss = { creating = false },
            onConfirm = { name ->
                creating = false
                actions.navigate(Screen.PlaylistDetail(vm.library.createPlaylist(name)))
            },
        )
    }
}

@Composable
private fun AlbumsTab(vm: MusicViewModel) {
    val actions = LocalActions.current
    val albums by vm.albums.collectAsState()
    if (albums.isEmpty()) {
        EmptyState(Icons.Filled.LibraryMusic, "No albums")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        state = rememberLazyGridState(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = actions.bottomPadding + 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album) { actions.navigate(Screen.AlbumDetail(album.id)) }
        }
    }
}

@Composable
fun AlbumCard(album: Album, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick)) {
        AlbumArt(
            album.tracks.first().uri,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            size = 400,
            shape = RoundedCornerShape(14.dp),
            iconSize = 40.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ArtistsTab(vm: MusicViewModel) {
    val actions = LocalActions.current
    val artists by vm.artists.collectAsState()
    if (artists.isEmpty()) {
        EmptyState(Icons.Filled.LibraryMusic, "No artists")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(top = 4.dp, bottom = actions.bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(artists, key = { it.name }) { artist ->
            ListRow(
                title = artist.name,
                subtitle = "${if (artist.albumCount == 1) "1 album" else "${artist.albumCount} albums"} · ${tracksLabel(artist.tracks.size)}",
                onClick = { actions.navigate(Screen.ArtistDetail(artist.name)) },
                leading = { AlbumArt(artist.tracks.first().uri, Modifier.size(52.dp), shape = CircleShape) },
            )
        }
    }
}

@Composable
private fun FoldersTab(vm: MusicViewModel) {
    val actions = LocalActions.current
    val folders by vm.folders.collectAsState()
    if (folders.isEmpty()) {
        EmptyState(Icons.Filled.Folder, "No folders")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(top = 4.dp, bottom = actions.bottomPadding),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(folders, key = { it.path }) { folder ->
            ListRow(
                title = folder.name,
                subtitle = "${tracksLabel(folder.tracks.size)} · /${folder.path.trimEnd('/')}",
                onClick = { actions.navigate(Screen.FolderDetail(folder.path)) },
                leading = { IconTile(Icons.Filled.Folder, androidx.compose.ui.graphics.Color(0xFFF5A623)) },
            )
        }
    }
}
