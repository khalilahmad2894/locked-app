package com.lockedapp.vault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockedapp.vault.VaultViewModel
import com.lockedapp.vault.data.VaultItemEntity
import com.lockedapp.vault.data.VaultItemType
import com.lockedapp.vault.ui.theme.DangerRed
import com.lockedapp.vault.ui.theme.FilesAccent
import com.lockedapp.vault.ui.theme.PhotosAccent
import com.lockedapp.vault.ui.theme.VaultSurfaceElevated
import com.lockedapp.vault.ui.theme.VaultVoid
import com.lockedapp.vault.ui.theme.VideosAccent

private enum class VaultTab(val label: String, val accent: Color) {
    PHOTOS("Photos", PhotosAccent),
    VIDEOS("Videos", VideosAccent),
    FILES("Files", FilesAccent)
}

@Composable
fun VaultScreen(
    viewModel: VaultViewModel,
    onOpenPhoto: (startIndex: Int) -> Unit,
    onOpenVideo: (VaultItemEntity) -> Unit,
    onOpenDocument: (VaultItemEntity) -> Unit,
    onOpenCamera: () -> Unit,
    onOpenVideoRecorder: () -> Unit
) {
    var tab by remember { mutableStateOf(VaultTab.PHOTOS) }
    val photos by viewModel.photos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val files by viewModel.files.collectAsState()

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isSelecting = selectedIds.isNotEmpty()

    // Selecting is a per-tab affordance — switching tabs (or backing out of it) clears it.
    LaunchedEffect(tab) { selectedIds = emptySet() }
    BackHandler(enabled = isSelecting) { selectedIds = emptySet() }

    val currentItems = when (tab) {
        VaultTab.PHOTOS -> photos
        VaultTab.VIDEOS -> videos
        VaultTab.FILES -> files
    }

    fun toggleSelection(id: Long) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFile(it) } }

    Scaffold(
        containerColor = VaultVoid,
        topBar = {
            if (isSelecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { selectedIds = emptySet() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection", tint = Color.White)
                    }
                    Text(
                        "${selectedIds.size} selected",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete selected", tint = DangerRed)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        "My Vault",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        bottomBar = {
            if (!isSelecting) {
                VaultBottomBar(
                    selected = tab,
                    onSelect = { tab = it },
                    onCapture = { if (tab == VaultTab.VIDEOS) onOpenVideoRecorder() else onOpenCamera() },
                    onImport = { importLauncher.launch(arrayOf("*/*")) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                VaultTab.PHOTOS -> PhotosGrid(
                    items = photos,
                    viewModel = viewModel,
                    accent = VaultTab.PHOTOS.accent,
                    selectedIds = selectedIds,
                    isSelecting = isSelecting,
                    onOpen = { index -> onOpenPhoto(index) },
                    onToggleSelect = ::toggleSelection
                )
                VaultTab.VIDEOS -> VideosGrid(
                    items = videos,
                    viewModel = viewModel,
                    accent = VaultTab.VIDEOS.accent,
                    selectedIds = selectedIds,
                    isSelecting = isSelecting,
                    onOpen = onOpenVideo,
                    onToggleSelect = ::toggleSelection
                )
                VaultTab.FILES -> FilesList(
                    items = files,
                    accent = VaultTab.FILES.accent,
                    selectedIds = selectedIds,
                    isSelecting = isSelecting,
                    onOpen = onOpenDocument,
                    onToggleSelect = ::toggleSelection
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = VaultSurfaceElevated,
            title = {
                Text(
                    "Delete ${selectedIds.size} item${if (selectedIds.size != 1) "s" else ""}?",
                    color = Color.White
                )
            },
            text = { Text("This can't be undone.", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    currentItems.filter { it.id in selectedIds }.forEach { viewModel.deleteItem(it) }
                    selectedIds = emptySet()
                    showDeleteConfirm = false
                }) { Text("Delete", color = DangerRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = Color.White) }
            }
        )
    }
}

@Composable
private fun VaultBottomBar(
    selected: VaultTab,
    onSelect: (VaultTab) -> Unit,
    onCapture: () -> Unit,
    onImport: () -> Unit
) {
    BottomAppBar(
        containerColor = VaultSurfaceElevated,
        contentColor = Color.White,
        actions = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                VaultTab.entries.forEach { t ->
                    BottomTabItem(tab = t, selected = t == selected, onClick = { onSelect(t) })
                }
                Spacer(modifier = Modifier.weight(1f))
                if (selected != VaultTab.FILES) {
                    IconButton(onClick = onCapture) {
                        Icon(
                            imageVector = if (selected == VaultTab.VIDEOS) Icons.Filled.Videocam else Icons.Filled.CameraAlt,
                            contentDescription = "Capture",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onImport,
                containerColor = selected.accent,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Import file")
            }
        }
    )
}

@Composable
private fun BottomTabItem(tab: VaultTab, selected: Boolean, onClick: () -> Unit) {
    val icon = when (tab) {
        VaultTab.PHOTOS -> Icons.Filled.Image
        VaultTab.VIDEOS -> Icons.Filled.Videocam
        VaultTab.FILES -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = tab.label,
            tint = if (selected) tab.accent else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            tab.label,
            color = if (selected) tab.accent else Color.White.copy(alpha = 0.5f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PhotosGrid(
    items: List<VaultItemEntity>,
    viewModel: VaultViewModel,
    accent: Color,
    selectedIds: Set<Long>,
    isSelecting: Boolean,
    onOpen: (Int) -> Unit,
    onToggleSelect: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(accent = accent, text = "No photos yet\nTap + to add one")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            val index = items.indexOf(item)
            ThumbnailTile(
                item = item,
                viewModel = viewModel,
                accent = accent,
                showPlayIcon = false,
                isSelected = item.id in selectedIds,
                isSelecting = isSelecting,
                onClick = { if (isSelecting) onToggleSelect(item.id) else onOpen(index) },
                onLongPress = { onToggleSelect(item.id) }
            )
        }
    }
}

@Composable
private fun VideosGrid(
    items: List<VaultItemEntity>,
    viewModel: VaultViewModel,
    accent: Color,
    selectedIds: Set<Long>,
    isSelecting: Boolean,
    onOpen: (VaultItemEntity) -> Unit,
    onToggleSelect: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(accent = accent, text = "No videos yet\nTap + to add one")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            ThumbnailTile(
                item = item,
                viewModel = viewModel,
                accent = accent,
                showPlayIcon = true,
                isSelected = item.id in selectedIds,
                isSelecting = isSelecting,
                onClick = { if (isSelecting) onToggleSelect(item.id) else onOpen(item) },
                onLongPress = { onToggleSelect(item.id) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThumbnailTile(
    item: VaultItemEntity,
    viewModel: VaultViewModel,
    accent: Color,
    showPlayIcon: Boolean,
    isSelected: Boolean,
    isSelecting: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    var bitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(item.id) { bitmap = viewModel.thumbnailFor(item) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.15f))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
        if (showPlayIcon && !isSelecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        if (isSelecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent else Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilesList(
    items: List<VaultItemEntity>,
    accent: Color,
    selectedIds: Set<Long>,
    isSelecting: Boolean,
    onOpen: (VaultItemEntity) -> Unit,
    onToggleSelect: (Long) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState(accent = accent, text = "No files yet\nTap + to add one")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { item ->
            val isSelected = item.id in selectedIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) accent.copy(alpha = 0.18f) else VaultSurfaceElevated)
                    .combinedClickable(
                        onClick = { if (isSelecting) onToggleSelect(item.id) else onOpen(item) },
                        onLongClick = { onToggleSelect(item.id) }
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelecting) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) accent else Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    } else {
                        Icon(
                            imageVector = when (item.type) {
                                VaultItemType.AUDIO -> Icons.Filled.AudioFile
                                VaultItemType.DOCUMENT -> Icons.Filled.Description
                                else -> Icons.AutoMirrored.Filled.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = accent
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        item.displayName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                    Text(
                        "${item.sizeBytes / 1024} KB",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(accent: Color, text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
            )
            Text(
                text,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}
