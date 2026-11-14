package com.khalil.music.ui.screens

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.khalil.music.MusicViewModel
import com.khalil.music.data.formatDuration
import com.khalil.music.data.trackUri
import com.khalil.music.playback.PlayerState
import com.khalil.music.ui.components.AlbumArt
import com.khalil.music.ui.components.ArtRequest
import com.khalil.music.ui.theme.AccentOnDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val MiniPlayerHeight = 66.dp

@Composable
private fun rememberPosition(vm: MusicViewModel, state: PlayerState): Long {
    var pos by remember { mutableLongStateOf(vm.player.position()) }
    LaunchedEffect(state.currentId, state.isPlaying) {
        while (isActive) {
            pos = vm.player.position()
            delay(if (state.isPlaying) 250 else 1000)
        }
    }
    return pos
}

@Composable
fun MiniPlayer(vm: MusicViewModel, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val state by vm.player.state.collectAsState()
    val id = state.currentId ?: return
    val pos = rememberPosition(vm, state)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(onDragStart = { total = 0f }) { _, drag ->
                    total += drag
                    if (total < -40f) { total = 0f; onOpen() }
                }
            }
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { if (state.durationMs > 0) (pos.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                drawStopIndicator = {},
                gapSize = 0.dp,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(MiniPlayerHeight - 2.dp)
                    .padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(trackUri(id), Modifier.size(46.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        state.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { vm.player.previous() }) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                IconButton(onClick = { vm.player.togglePlay() }) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp),
                    )
                }
                IconButton(onClick = { vm.player.next() }) { Icon(Icons.Filled.SkipNext, "Next") }
            }
        }
    }
}

@Composable
fun PlayerScreen(vm: MusicViewModel, onClose: () -> Unit, onAddToPlaylist: (Long) -> Unit) {
    val state by vm.player.state.collectAsState()
    val favourites by vm.library.favourites.collectAsState()
    val id = state.currentId
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Light status bar icons on the dark, art-tinted background.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val wasLight = controller?.isAppearanceLightStatusBars ?: false
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = wasLight
            controller?.isAppearanceLightNavigationBars = wasLight
        }
    }

    val fallback = Color(0xFF2A2D34)
    var tint by remember { mutableStateOf(fallback) }
    LaunchedEffect(id) {
        if (id == null) return@LaunchedEffect
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context).data(ArtRequest(trackUri(id), 512)).allowHardware(false).build()
        )
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
        tint = if (bitmap == null) fallback else {
            val p = Palette.from(bitmap).generate()
            Color(p.getDarkVibrantColor(p.getDarkMutedColor(p.getDominantColor(fallback.hashCode()))))
        }
    }
    val bg by animateColorAsState(tint, tween(600), label = "bg")

    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    val pos = rememberPosition(vm, state)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(bg, Color.Black, 0.15f), lerp(bg, Color.Black, 0.75f))))
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, drag -> if (drag > 30f) onClose() }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.padding(start = 0.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSleep = true }) {
                    Icon(
                        Icons.Filled.Bedtime,
                        "Sleep timer",
                        tint = if (state.sleepTimerEndsAt != null) AccentOnDark else Color.White.copy(alpha = 0.85f),
                    )
                }
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, "Queue", tint = Color.White)
                }
            }

            Spacer(Modifier.weight(0.5f))

            var dragTotal by remember { mutableFloatStateOf(0f) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onDragEnd = {
                                if (dragTotal < -120f) vm.player.next() else if (dragTotal > 120f) vm.player.previous()
                            },
                        ) { _, amount -> dragTotal += amount }
                    },
            ) {
                AlbumArt(
                    id?.let(::trackUri),
                    Modifier.fillMaxSize(),
                    size = 1000,
                    shape = RoundedCornerShape(18.dp),
                    iconSize = 96.dp,
                )
            }

            Spacer(Modifier.weight(0.5f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { id?.let(onAddToPlaylist) }) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to playlist", tint = Color.White.copy(alpha = 0.85f))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        state.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val fav = id != null && id in favourites
                IconButton(onClick = { id?.let(vm.library::toggleFavourite) }) {
                    Icon(
                        if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        "Favourite",
                        tint = if (fav) Color(0xFFFF4B6E) else Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            val fraction = if (state.durationMs > 0) (pos.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
            Slider(
                value = if (dragging) dragValue else fraction,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    vm.player.seekTo((dragValue * state.durationMs).toLong())
                    dragging = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier.height(28.dp),
            )
            Row(Modifier.fillMaxWidth()) {
                val shown = if (dragging) (dragValue * state.durationMs).toLong() else pos
                Text(formatDuration(shown), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(state.durationMs), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { vm.player.toggleShuffle() }) {
                    ShuffleIcon(on = state.shuffle)
                }
                HoldButton(
                    icon = Icons.Filled.SkipPrevious,
                    label = "Previous",
                    onTap = { vm.player.previous() },
                    onHold = { vm.player.seekBy(-5_000) },
                )
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { vm.player.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (state.isPlaying) "Pause" else "Play",
                        tint = lerp(bg, Color.Black, 0.4f),
                        modifier = Modifier.size(42.dp),
                    )
                }
                HoldButton(
                    icon = Icons.Filled.SkipNext,
                    label = "Next",
                    onTap = { vm.player.next() },
                    onHold = { vm.player.seekBy(5_000) },
                )
                IconButton(onClick = { vm.player.cycleRepeat() }) {
                    RepeatIcon(state.repeatMode)
                }
            }

            Spacer(Modifier.weight(0.35f))

            val upNext = state.queue.getOrNull(state.queue.indexOfFirst { it.windowIndex == state.currentIndex } + 1)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showQueue = true }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Up next", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    upNext?.let { "${it.title} · ${it.artist}" } ?: "End of queue",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showQueue) QueueSheet(vm, state) { showQueue = false }
    if (showSleep) SleepTimerDialog(state.sleepTimerEndsAt, onPick = { vm.player.setSleepTimer(it); showSleep = false }) {
        showSleep = false
    }
}

/** Samsung style: a line through the icon when shuffle is off. */
@Composable
private fun ShuffleIcon(on: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Icon(Icons.Filled.Shuffle, "Shuffle", tint = if (on) AccentOnDark else Color.White.copy(alpha = 0.55f))
        if (!on) {
            Box(
                Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .graphicsLayer { rotationZ = -45f }
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun RepeatIcon(mode: Int) {
    when (mode) {
        Player.REPEAT_MODE_ONE -> Icon(Icons.Filled.RepeatOne, "Repeat one", tint = AccentOnDark)
        Player.REPEAT_MODE_ALL -> Icon(Icons.Filled.Repeat, "Repeat all", tint = AccentOnDark)
        else -> Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Repeat, "Repeat off", tint = Color.White.copy(alpha = 0.55f))
            Box(
                Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .graphicsLayer { rotationZ = -45f }
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }
    }
}

/** Tap = skip; press and hold = keep seeking (rewind / fast-forward). */
@Composable
private fun HoldButton(icon: ImageVector, label: String, onTap: () -> Unit, onHold: () -> Unit) {
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(60.dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    var held = false
                    val job = scope.launch {
                        delay(450)
                        held = true
                        while (isActive) {
                            onHold()
                            delay(200)
                        }
                    }
                    val released = tryAwaitRelease()
                    job.cancel()
                    if (released && !held) onTap()
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(42.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(vm: MusicViewModel, state: PlayerState, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val queue = state.queue
    val currentPos = queue.indexOfFirst { it.windowIndex == state.currentIndex }.coerceAtLeast(0)
    LaunchedEffect(Unit) { listState.scrollToItem((currentPos - 1).coerceAtLeast(0)) }

    val itemHeight = 64.dp
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    var dragFrom by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragTarget = if (dragFrom < 0) -1 else (dragFrom + (dragOffset / itemPx).roundToInt()).coerceIn(0, queue.lastIndex)
    val canReorder = !state.shuffle

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text("Playing queue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text(
                "${currentPos + 1}/${queue.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!canReorder) {
            Text(
                "Turn off shuffle to reorder songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight(0.85f).navigationBarsPadding(),
        ) {
            itemsIndexed(queue, key = { i, e -> "${e.windowIndex}-${e.id}-$i" }) { i, entry ->
                val isCurrent = entry.windowIndex == state.currentIndex
                val translation = when {
                    dragFrom < 0 -> 0f
                    i == dragFrom -> dragOffset
                    i in (dragFrom + 1)..dragTarget -> -itemPx
                    i in dragTarget until dragFrom -> itemPx
                    else -> 0f
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .zIndex(if (i == dragFrom) 1f else 0f)
                        .graphicsLayer { translationY = translation }
                        .background(
                            if (i == dragFrom) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { vm.player.skipTo(entry.windowIndex) }
                        .padding(start = 20.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isCurrent) {
                        Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            entry.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!isCurrent) {
                        IconButton(onClick = { vm.player.removeFromQueue(entry.windowIndex) }) {
                            Icon(Icons.Filled.Close, "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (canReorder) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .pointerInput(i, queue.size) {
                                    detectDragGestures(
                                        onDragStart = { dragFrom = i; dragOffset = 0f },
                                        onDragEnd = {
                                            val target = (dragFrom + (dragOffset / itemPx).roundToInt()).coerceIn(0, queue.lastIndex)
                                            if (dragFrom >= 0 && target != dragFrom) vm.player.moveInQueue(dragFrom, target)
                                            dragFrom = -1; dragOffset = 0f
                                        },
                                        onDragCancel = { dragFrom = -1; dragOffset = 0f },
                                    ) { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.DragHandle, "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(endsAt: Long?, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(endsAt) {
        while (endsAt != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep timer") },
        text = {
            Column {
                if (endsAt != null) {
                    Text(
                        "Music stops in ${formatDuration(endsAt - now)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                listOf(15, 30, 45, 60, 90).forEach { m ->
                    Text(
                        "$m minutes",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(m) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (endsAt != null) TextButton(onClick = { onPick(null) }) { Text("Turn off") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Suppress("unused")
private val unusedAlign = TextAlign.Center
