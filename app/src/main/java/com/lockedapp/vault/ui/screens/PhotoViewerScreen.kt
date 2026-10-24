package com.lockedapp.vault.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.lockedapp.vault.VaultViewModel
import com.lockedapp.vault.data.VaultItemEntity
import me.saket.telephoto.zoomable.ZoomableContentLocation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun PhotoViewerScreen(
    viewModel: VaultViewModel,
    onBack: () -> Unit
) {
    val photos by viewModel.photos.collectAsState()
    if (photos.isEmpty()) {
        onBack()
        return
    }
    val pagerState = rememberPagerState(
        initialPage = viewModel.photoViewerStartIndex.coerceIn(0, photos.lastIndex)
    ) { photos.size }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // HorizontalPager owns the swipe gesture; each page's zoomable Image only
        // takes over pan/drag once the user has actually zoomed in past 1x.
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableImage(item = photos[page], viewModel = viewModel)
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

        IconButton(
            onClick = {
                val current = photos.getOrNull(pagerState.currentPage)
                current?.let { viewModel.deleteItem(it) }
                if (photos.size <= 1) onBack()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
        }
    }
}

@Composable
private fun ZoomableImage(item: VaultItemEntity, viewModel: VaultViewModel) {
    var bitmap by remember(item.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(item.id) { bitmap = viewModel.fullImageFor(item) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp == null) {
            CircularProgressIndicator(color = Color.White)
        } else {
            val imageBitmap = remember(item.id) { bmp.asImageBitmap() }
            val zoomableState = rememberZoomableState().apply {
                setContentLocation(
                    ZoomableContentLocation.scaledInsideAndCenterAligned(
                        Size(imageBitmap.width.toFloat(), imageBitmap.height.toFloat())
                    )
                )
            }
            Image(
                bitmap = imageBitmap,
                contentDescription = item.displayName,
                contentScale = ContentScale.Inside,
                alignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .zoomable(zoomableState)
            )
        }
    }
}
