package com.lockedapp.vault.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.lockedapp.vault.VaultViewModel
import com.lockedapp.vault.data.VaultItemEntity

@Composable
fun VideoPlayerScreen(viewModel: VaultViewModel, onBack: () -> Unit) {
    val videos by viewModel.videos.collectAsState()
    if (videos.isEmpty()) {
        onBack()
        return
    }
    val startIndex = videos.indexOfFirst { it.id == viewModel.activeVideoItem?.id }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = startIndex) { videos.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            VideoPage(
                item = videos[page],
                isActive = pagerState.currentPage == page,
                viewModel = viewModel
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

@Composable
private fun VideoPage(item: VaultItemEntity, isActive: Boolean, viewModel: VaultViewModel) {
    // Each page gets its own player so Compose can create/release them lazily as the
    // Pager scrolls — only the currently visible page's player is actually playing.
    val context = LocalContext.current
    var videoUri by remember(item.id) { mutableStateOf<Uri?>(null) }

    LaunchedEffect(item.id) { videoUri = viewModel.openForViewing(item) }

    val exoPlayer = remember(item.id) { ExoPlayer.Builder(context).build() }

    DisposableEffect(item.id) {
        onDispose { exoPlayer.release() }
    }

    LaunchedEffect(videoUri) {
        videoUri?.let { uri ->
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }

    LaunchedEffect(isActive) {
        exoPlayer.playWhenReady = isActive
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (videoUri == null) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
