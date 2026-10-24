package com.lockedapp.vault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lockedapp.vault.VaultViewModel
import com.lockedapp.vault.data.VaultItemEntity
import com.lockedapp.vault.data.VaultItemType
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
    onOpenDocument: (VaultItemEntity) -> Unit
) {
    var tab by remember { mutableStateOf(VaultTab.PHOTOS) }
    val photos by viewModel.photos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val files by viewModel.files.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importFile(it) } }

    Scaffold(
        containerColor = VaultVoid,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Vault",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(tab.accent, tab.accent.copy(alpha = 0.6f))))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Import", tint = Color.White)
                    }
                }
                SegmentedTabs(selected = tab, onSelect = { tab = it })
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
                    onOpen = { index -> onOpenPhoto(index) },
                    onDelete = { viewModel.deleteItem(it) }
                )
                VaultTab.VIDEOS -> VideosGrid(
                    items = videos,
                    viewModel = viewModel,
                    accent = VaultTab.VIDEOS.accent,
                    onOpen = onOpenVideo,
                    onDelete = { viewModel.deleteItem(it) }
                )
                VaultTab.FILES -> FilesList(
                    items = files,
                    accent = VaultTab.FILES.accent,
                    onOpen = onOpenDocument,
                    onDelete = { viewModel.deleteItem(it) }
                )
            }
        }
    }
}

@Composable
private fun SegmentedTabs(selected: VaultTab, onSelect: (VaultTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(VaultSurfaceElevated, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VaultTab.entries.forEach { t ->
            val isSelected = t == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) t.accent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    t.label,
                    color = if (isSelected) t.accent else Color.White.copy(alpha = 0.5f),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun PhotosGrid(
    items: List<VaultItemEntity>,
    viewModel: VaultViewModel,
    accent: Color,
    onOpen: (Int) -> Unit,
    onDelete: (VaultItemEntity) -> Unit
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
                onClick = { onOpen(index) },
                onLongPress = { onDelete(item) }
            )
        }
    }
}

@Composable
private fun VideosGrid(
    items: List<VaultItemEntity>,
    viewModel: VaultViewModel,
    accent: Color,
    onOpen: (VaultItemEntity) -> Unit,
    onDelete: (VaultItemEntity) -> Unit
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
                onClick = { onOpen(item) },
                onLongPress = { onDelete(item) }
            )
        }
    }
}

@Composable
private fun ThumbnailTile(
    item: VaultItemEntity,
    viewModel: VaultViewModel,
    accent: Color,
    showPlayIcon: Boolean,
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
            .clickable { onClick() }
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        if (showPlayIcon) {
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
        IconButton(
            onClick = onLongPress,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(2.dp)
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun FilesList(
    items: List<VaultItemEntity>,
    accent: Color,
    onOpen: (VaultItemEntity) -> Unit,
    onDelete: (VaultItemEntity) -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(VaultSurfaceElevated)
                    .clickable { onOpen(item) }
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
                IconButton(onClick = { onDelete(item) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.6f))
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}
