package dev.folio.reader.ui

import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.foundation.systemGestureExclusion
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.material.icons.rounded.ScreenRotationAlt
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.StickyNote2
import dev.folio.reader.data.PageNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import dev.folio.reader.data.Status
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.PageTheme
import dev.folio.reader.pdf.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Colour filters that turn the white page into each reading theme. */
private fun PageTheme.filter(): ColorFilter? = when (this) {
    PageTheme.Normal -> null
    // White paper becomes near black and black text becomes near white; hues are kept.
    PageTheme.Night -> channelFilter(12f to 240f, 12f to 240f, 12f to 240f)
    PageTheme.SoftNight -> channelFilter(32f to 214f, 30f to 207f, 26f to 190f)
    PageTheme.Sepia -> ColorFilter.colorMatrix(
        ColorMatrix(floatArrayOf(0.957f, 0f, 0f, 0f, 0f, 0f, 0.91f, 0f, 0f, 0f, 0f, 0f, 0.816f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)),
    )
}

/** Maps each channel so 255 (paper) lands on `paper` and 0 (ink) on `ink`, inverted: (paper, ink). */
private fun channelFilter(r: Pair<Float, Float>, g: Pair<Float, Float>, b: Pair<Float, Float>): ColorFilter {
    fun row(c: Pair<Float, Float>, index: Int): FloatArray {
        val (paper, ink) = c
        val scale = (ink - paper) / 255f
        return FloatArray(5).also { it[index] = -scale; it[4] = ink }
    }
    val m = row(r, 0) + row(g, 1) + row(b, 2) + floatArrayOf(0f, 0f, 0f, 1f, 0f)
    return ColorFilter.colorMatrix(ColorMatrix(m))
}

private fun PageTheme.background(): Color = when (this) {
    PageTheme.Normal -> Color(0xFFE9E5DB)
    PageTheme.Night -> Color(0xFF050505)
    PageTheme.SoftNight -> Color(0xFF16140F)
    PageTheme.Sepia -> Color(0xFFD9CDB2)
}

@OptIn(FlowPreview::class)
@Composable
fun ReaderScreen(model: FolioViewModel, id: String) {
    val state by model.repo.state.collectAsState()
    val book = state.books.firstOrNull { it.id == id } ?: return
    val context = LocalView.current.context
    val pdf by produceState<PdfFile?>(null, id) {
        value = withContext(Dispatchers.IO) { PdfFile.open(context, Uri.parse(book.uri)) }
    }
    // Capture the file itself: the delegate would read the *new* value when the effect is disposed.
    val opened = pdf
    DisposableEffect(opened) { onDispose { opened?.closeInBackground() } }

    var barsVisible by remember { mutableStateOf(true) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    val theme = state.theme
    ImmersiveBars(barsVisible)
    ReadingClock(model, id)
    var bookmarkSheet by remember { mutableStateOf(false) }
    var jumpDialog by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<NoteEdit?>(null) }
    var menu by remember { mutableStateOf(false) }
    var scrub by remember { mutableStateOf<Int?>(null) }
    // Where you were before a jump, so one tap can take you back.
    var returnTo by remember { mutableStateOf<ReturnPoint?>(null) }
    var detecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var sizesVersion by remember { mutableStateOf(0) }
    var autoScroll by remember { mutableStateOf(false) }
    var autoSpeed by remember { mutableFloatStateOf(70f) }
    var tool by remember { mutableStateOf<ReaderTool?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val jumpTo = remember { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize().background(theme.background())) {
        val file = pdf
        if (file == null) {
            // The cover is already in memory: show it at once, so opening feels instant while the file loads.
            val cover by produceState<androidx.compose.ui.graphics.ImageBitmap?>(model.covers.peek(book)?.asImageBitmap(), book.id) {
                value = value ?: model.covers.get(book)?.asImageBitmap()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                cover?.let { Image(it, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth, colorFilter = theme.filter(), alignment = Alignment.TopCenter) }
                androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center).size(32.dp), strokeWidth = 3.dp)
            }
        } else {
            val list = rememberLazyListState()
            // A page counts as read once the reader stays on it for a moment.
            LaunchedEffect(file) {
                snapshotFlow { list.firstVisibleItemIndex }.debounce(1500).collect { model.pageSeen(id, it) }
            }
            // A page's real shape is learned as it is drawn; the layout follows.
            DisposableEffect(file) {
                file.onShapeLearned = { sizesVersion++ }
                onDispose { file.onShapeLearned = null }
            }
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }.roundToInt()
                val cache = remember(file) {
                    // A share of the app's memory, never so much that a big page can crowd out everything else.
                    val budget = (Runtime.getRuntime().maxMemory() / 6).toInt().coerceIn(24 * 1024 * 1024, 96 * 1024 * 1024)
                    object : LruCache<String, Bitmap>(budget) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount }
                }
                val appContext = LocalContext.current
                DisposableEffect(cache) {
                    val callbacks = object : android.content.ComponentCallbacks2 {
                        override fun onTrimMemory(level: Int) { if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) cache.evictAll() }
                        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {}
                        @Deprecated("Replaced by onTrimMemory") override fun onLowMemory() { cache.evictAll() }
                    }
                    appContext.registerComponentCallbacks(callbacks)
                    onDispose { appContext.unregisterComponentCallbacks(callbacks); cache.evictAll() }
                }

                // Once scrolling settles, quietly draw the pages just ahead and behind so they are ready when you get there.
                LaunchedEffect(file, widthPx, book.cropMargins, book.margins) {
                    val visW = if (book.cropMargins && book.margins.size == 4) (1f - book.margins[0] - book.margins[2]).coerceAtLeast(0.3f) else 1f
                    val target = (widthPx / visW).roundToInt().coerceAtMost(3600)
                    snapshotFlow { Triple(list.firstVisibleItemIndex, list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0, list.isScrollInProgress) }
                        .debounce(250)
                        .collectLatest { (first, last, scrolling) ->
                            if (scrolling) return@collectLatest
                            for (p in listOf(last + 1, first - 1, last + 2, last + 3, first - 2)) {
                                if (p !in 0 until file.pageCount || cache.get("$p@$target") != null) continue
                                val bmp = model.pageCache.get(book.id, p, target) ?: file.render(p, target)?.also { model.pageCache.put(book.id, p, target, it) } ?: continue
                                cache.put("$p@$target", bmp)
                            }
                        }
                }

                // Pick up where the book was left: the page and how far down it.
                LaunchedEffect(file) {
                    val firstPx = (widthPx * file.aspect(book.page)).roundToInt()
                    list.scrollToItem(book.page.coerceIn(0, file.pageCount - 1), (book.offset * firstPx).roundToInt())
                }
                LaunchedEffect(jumpTo.value) {
                    jumpTo.value?.let { target ->
                        val here = ReturnPoint(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
                        // A real jump (not to a neighbouring page) leaves a way back.
                        if (kotlin.math.abs(target - here.page) >= 3) returnTo = here
                        list.scrollToItem(target.coerceIn(0, file.pageCount - 1))
                        jumpTo.value = null
                    }
                }
                LaunchedEffect(file) {
                    snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                        .debounce(600)
                        .collect { (page, px) ->
                            val height = (widthPx * file.aspect(page)).coerceAtLeast(1f)
                            val atEnd = page >= file.pageCount - 1 && px / height > 0.3f
                            model.repo.updateBook(id) {
                                it.copy(
                                    page = page,
                                    offset = (px / height).coerceIn(0f, 1f),
                                    pages = file.pageCount,
                                    status = if (atEnd && file.pageCount > 1) Status.Finished else it.status,
                                )
                            }
                        }
                }

                LazyColumn(
                    state = list,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = offX; translationY = offY; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f) }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { barsVisible = !barsVisible },
                                // A double tap zooms in on the spot you tapped, or back out, with a short glide.
                                onDoubleTap = { p ->
                                    val z0 = zoom; val x0 = offX; val y0 = offY
                                    val z1 = if (z0 > 1.05f) 1f else DOUBLE_TAP_ZOOM
                                    val w = size.width.toFloat(); val h = size.height.toFloat()
                                    scope.launch {
                                        androidx.compose.animation.core.animate(0f, 1f, animationSpec = androidx.compose.animation.core.tween(240)) { t, _ ->
                                            val z = z0 + (z1 - z0) * t
                                            // The point under your finger stays where it is while the page grows around it.
                                            offX = (p.x * z0 + x0 - p.x * z).coerceIn(w * (1f - z), 0f)
                                            offY = (p.y * z0 + y0 - p.y * z).coerceIn(h * (1f - z), 0f)
                                            zoom = z
                                        }
                                    }
                                },
                            )
                        }
                        .pointerInput(Unit) {
                            // Two fingers pinch and pan, zooming around the point between them; one finger is left to the list so it scrolls.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.size >= 2) {
                                        val nz = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                                        val c = event.calculateCentroid()
                                        val pan = event.calculatePan()
                                        offX = (c.x * zoom + offX + pan.x - c.x * nz).coerceIn(size.width * (1f - nz), 0f)
                                        offY = (c.y * zoom + offY + pan.y - c.y * nz).coerceIn(size.height * (1f - nz), 0f)
                                        zoom = nz
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(file.pageCount) { index ->
                        @Suppress("UNUSED_VARIABLE") val layoutVersion = sizesVersion
                        PageView(file, book.id, model.pageCache, index, widthPx, zoom, cache, theme, crop = if (book.cropMargins && book.margins.size == 4) book.margins else null, noteCount = book.comments.count { it.page == index }, onNotes = { bookmarkSheet = true })
                    }
                }
            }
            val current = (list.firstVisibleItemIndex + 1).coerceIn(1, file.pageCount)

            // Auto-scroll: a steady glide down the book at the chosen speed (points per second).
            LaunchedEffect(autoScroll) {
                if (!autoScroll) return@LaunchedEffect
                var last = withFrameNanos { it }
                var carry = 0f
                while (true) {
                    val now = withFrameNanos { it }
                    carry += autoSpeed * (now - last) / 1_000_000_000f * density
                    last = now
                    model.touched()
                    val whole = carry.toInt()
                    if (whole != 0) {
                        carry -= whole
                        if (list.scrollBy(whole.toFloat()) == 0f) { autoScroll = false; return@LaunchedEffect }
                    }
                }
            }

            AnimatedVisibility(
                barsVisible,
                Modifier.align(Alignment.TopCenter),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f), tonalElevation = 3.dp, shadowElevation = 4.dp) {
                    Column(Modifier.statusBarsPadding().padding(horizontal = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { model.closeBook() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                            Text(book.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val marked = (current - 1) in book.bookmarks
                            IconButton(onClick = { model.repo.toggleBookmark(id, current - 1) }) {
                                Icon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (marked) "Remove bookmark" else "Add bookmark", tint = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Reading options") }
                                DropdownMenu(menu, { menu = false }) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.PlayCircle, null) },
                                        text = { Text(if (autoScroll) "Stop auto-scroll" else "Auto-scroll") },
                                        onClick = { menu = false; autoScroll = !autoScroll; tool = if (autoScroll) ReaderTool.AutoScroll else null; barsVisible = !autoScroll },
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.AddComment, null) },
                                        text = { Text("Comment on this page") },
                                        onClick = { menu = false; editNote = NoteEdit(null, current - 1, "") },
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Rounded.Bookmarks, null) },
                                        text = { Text("Bookmarks and notes") },
                                        onClick = { menu = false; bookmarkSheet = true },
                                    )
                                }
                            }
                        }
                        // The reading controls, each an icon: theme, margins, brightness, rotation.
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            ThemeMenu(theme) { picked -> model.repo.update { it.copy(theme = picked) } }
                            CropButton(book.cropMargins, detecting) {
                                if (book.cropMargins) {
                                    model.repo.updateBook(id) { it.copy(cropMargins = false) }
                                } else if (book.margins.size == 4) {
                                    model.repo.updateBook(id) { it.copy(cropMargins = true) }
                                } else if (!detecting) {
                                    detecting = true
                                    scope.launch {
                                        val found = file.detectMargins()
                                        detecting = false
                                        if (found == null) model.notify("No clear margins to remove in this book.")
                                        else model.repo.updateBook(id) { it.copy(margins = found, cropMargins = true) }
                                    }
                                }
                            }
                            BrightnessButton(book.brightness) { v -> model.repo.updateBook(id) { it.copy(brightness = v) } }
                            RotationMenu(book.orientation) { o -> model.repo.updateBook(id) { it.copy(orientation = o) } }
                        }
                    }
                }
            }

            // The reading tools float above the bottom bar and stay while you use them.
            AnimatedVisibility(
                tool != null && barsVisible,
                Modifier.align(Alignment.BottomCenter).padding(bottom = 112.dp).navigationBarsPadding().padding(horizontal = 16.dp),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp, shadowElevation = 8.dp) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                        when (tool) {
                            ReaderTool.AutoScroll -> {
                                ToolHeader("Auto-scroll", onClose = { autoScroll = false; tool = null })
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { autoScroll = !autoScroll }) {
                                        Icon(if (autoScroll) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle, if (autoScroll) "Pause" else "Play", Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(Icons.Rounded.Speed, null, Modifier.size(20.dp))
                                    Slider(value = autoSpeed, onValueChange = { autoSpeed = it }, valueRange = 15f..400f, modifier = Modifier.weight(1f))
                                }
                                Text("Tap the page to show the controls. Speed ${autoSpeed.roundToInt()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            null -> {}
                        }
                    }
                }
            }

            AnimatedVisibility(
                barsVisible,
                Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                // A floating pill lifted clear of the bottom edge, where Android's own swipe (switch app, go home) lives,
                // so reaching for the slider is never mistaken for it.
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f),
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 26.dp),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$current/${file.pageCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { jumpDialog = true }.padding(horizontal = 6.dp, vertical = 6.dp),
                        )
                        var origin by remember { mutableStateOf<ReturnPoint?>(null) }
                        PageSlider(
                            current - 1, file.pageCount, Modifier.weight(1f).systemGestureExclusion(),
                            onScrub = { p ->
                                if (p != null && scrub == null) origin = ReturnPoint(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
                                // Letting go far from where the drag began leaves a way back to it.
                                if (p == null) origin?.let { o -> if (kotlin.math.abs(list.firstVisibleItemIndex - o.page) >= 3) returnTo = o }
                                scrub = p
                            },
                        ) { page -> list.requestScrollToItem(page) }
                        Text("${(current * 100f / file.pageCount).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            // After a jump: one tap returns to the place you left, for a little while.
            returnTo?.let { point ->
                LaunchedEffect(point) { kotlinx.coroutines.delay(20_000); if (returnTo == point) returnTo = null }
                Surface(
                    shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.tertiaryContainer, shadowElevation = 8.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (barsVisible) 96.dp else 28.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.clip(RoundedCornerShape(22.dp)).clickable {
                                scope.launch { list.scrollToItem(point.page, point.offset) }
                                returnTo = null
                            }.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Back to page ${point.page + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        IconButton(onClick = { returnTo = null }, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Close, "Dismiss", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer) }
                    }
                }
            }
            // The page number while you drag, floating above the slim bar.
            scrub?.let { p ->
                Surface(
                    shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary, shadowElevation = 6.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
                ) {
                    Text("Page ${p + 1}", Modifier.padding(horizontal = 18.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
    val pages = pdf?.pageCount ?: 0
    KeepScreenOn(book.brightness)
    ReadingOrientation(book.orientation)
    if (bookmarkSheet) NotesSheet(
        book,
        onGo = { page -> jumpTo.value = page; bookmarkSheet = false },
        onRemoveBookmark = { page -> model.repo.toggleBookmark(id, page) },
        onEdit = { note -> bookmarkSheet = false; editNote = NoteEdit(note.id, note.page, note.text) },
        onDismiss = { bookmarkSheet = false },
    )
    editNote?.let { edit ->
        NoteDialog(
            edit,
            onSave = { text -> model.repo.saveComment(id, edit.page, text, edit.id); editNote = null },
            onDelete = { edit.id?.let { model.repo.deleteComment(id, it) }; editNote = null },
            onDismiss = { editNote = null },
        )
    }
    if (jumpDialog && pages > 0) JumpDialog(pages, onGo = { page -> jumpTo.value = page; jumpDialog = false }, onDismiss = { jumpDialog = false })
}

/** A place in the book: the page at the top of the screen and how far it was scrolled. */
private data class ReturnPoint(val page: Int, val offset: Int)

private data class NoteEdit(val id: Long?, val page: Int, val text: String)

private enum class ReaderTool { AutoScroll }

/** One page: a placeholder of the right shape until its bitmap is drawn. */
@Composable
private fun PageView(file: PdfFile, bookId: String, disk: dev.folio.reader.pdf.PageDiskCache, index: Int, widthPx: Int, zoom: Float, cache: LruCache<String, Bitmap>, theme: PageTheme, crop: List<Float>?, noteCount: Int, onNotes: () -> Unit) {
    // With the margins cropped only the middle of the page is shown, so it is drawn wider to keep the text as sharp.
    val (l, t, r, b) = crop ?: listOf(0f, 0f, 0f, 0f)
    val visW = (1f - l - r).coerceAtLeast(0.3f)
    val visH = (1f - t - b).coerceAtLeast(0.3f)
    // Sharper pixels when zoomed, in steps so a pinch does not re-render every frame.
    val target = (widthPx / visW * (if (zoom > 3.2f) 4f else if (zoom > 1.6f) 2.5f else if (zoom > 1.05f) 1.6f else 1f)).roundToInt().coerceAtMost(3600)
    val bitmap by produceState<Bitmap?>(cache.get("$index@$target") ?: cache.get("$index@${(target / 1.6f).roundToInt()}"), index, target) {
        cache.get("$index@$target")?.let { value = it; return@produceState }
        // Drawn before and kept on disk: reading a small file is far quicker than drawing the page again.
        disk.get(bookId, index, target)?.let { cache.put("$index@$target", it); value = it; return@produceState }
        // A third-size draw first: it is ready in a fraction of the time, so the page appears at once and sharpens after.
        if (value == null) file.render(index, (target / 3).coerceAtLeast(120))?.also { cache.put("$index@${target / 3}", it); value = it }
        file.render(index, target)?.also { cache.put("$index@$target", it); value = it; disk.put(bookId, index, target, it) }
    }
    val filter = theme.filter()
    Box(Modifier.fillMaxWidth().aspectRatio(visW / (file.aspect(index) * visH)).background(Color.White)) {
        if (bitmap == null) {
            Text("${index + 1}", Modifier.align(Alignment.Center), color = Color(0xFFB8B2A4), style = MaterialTheme.typography.displaySmall)
        }
        if (noteCount > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2B12E)).clickable(onClick = onNotes).padding(horizontal = 8.dp, vertical = 4.dp),
            ) { Text("✎ $noteCount", color = Color(0xFF3B2A00), style = MaterialTheme.typography.labelMedium) }
        }
        bitmap?.let { bmp ->
            val image = remember(bmp) { bmp.asImageBitmap() }
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image,
                    srcOffset = androidx.compose.ui.unit.IntOffset((l * image.width).roundToInt(), (t * image.height).roundToInt()),
                    srcSize = androidx.compose.ui.unit.IntSize((visW * image.width).roundToInt().coerceAtLeast(1), (visH * image.height).roundToInt().coerceAtLeast(1)),
                    dstSize = androidx.compose.ui.unit.IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                    colorFilter = filter,
                )
            }
        }
    }
}

/** Hides the status and navigation bars while reading without the toolbars. */
@Composable
private fun ImmersiveBars(barsVisible: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    DisposableEffect(barsVisible) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (barsVisible) controller.show(WindowInsetsCompat.Type.systemBars()) else controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesSheet(book: dev.folio.reader.data.Book, onGo: (Int) -> Unit, onRemoveBookmark: (Int) -> Unit, onEdit: (PageNote) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (book.bookmarks.isEmpty()) {
                Text("Tap the bookmark icon on a page to mark it.", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            book.bookmarks.forEach { page ->
                Row(Modifier.fillMaxWidth().clickable { onGo(page) }.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text("Page ${page + 1}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onRemoveBookmark(page) }) { Text("Remove") }
                }
            }
            Text("Page comments", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp))
            if (book.comments.isEmpty()) {
                Text("Use the comment icon to write a note on a page.", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            book.comments.forEach { note ->
                Row(Modifier.fillMaxWidth().clickable { onGo(note.page) }.padding(horizontal = 24.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.StickyNote2, null, tint = Color(0xFFC08A3E))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Page ${note.page + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(note.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    IconButton(onClick = { onEdit(note) }) { Icon(Icons.Rounded.Edit, "Edit") }
                }
            }
        }
    }
}

@Composable
private fun NoteDialog(edit: NoteEdit, onSave: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(edit.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comment on page ${edit.page + 1}") },
        text = {
            OutlinedTextField(text, { text = it }, placeholder = { Text("Write your comment") }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = {
            Row {
                if (edit.id != null) TextButton(onClick = onDelete) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** The page theme as a dropdown, each choice drawn as the paper and ink it gives. */
@Composable
private fun ThemeMenu(current: PageTheme, onPick: (PageTheme) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(if (current == PageTheme.Normal) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, "Page theme", tint = if (current == PageTheme.Normal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }) {
            PageTheme.entries.forEach { t ->
                val (paper, ink) = when (t) {
                    PageTheme.Normal -> Color.White to Color(0xFF222222)
                    PageTheme.Night -> Color(0xFF0C0C0C) to Color(0xFFF0F0F0)
                    PageTheme.SoftNight -> Color(0xFF201E1A) to Color(0xFFD6CFBE)
                    PageTheme.Sepia -> Color(0xFFF4E8D0) to Color(0xFF3B3226)
                }
                DropdownMenuItem(
                    leadingIcon = {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(paper).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text("A", color = ink, style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    text = { Text(t.label, fontWeight = if (t == current) FontWeight.SemiBold else FontWeight.Normal) },
                    trailingIcon = { if (t == current) Icon(Icons.Rounded.Check, null) },
                    onClick = { onPick(t); open = false },
                )
            }
        }
    }
}

/** Remove the blank margins around the text: on or off for this book. */
@Composable
private fun CropButton(on: Boolean, detecting: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (detecting) androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        else Icon(Icons.Rounded.Crop, if (on) "Show the margins" else "Remove the margins", tint = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

private fun dev.folio.reader.data.Orientation.icon() = when (this) {
    dev.folio.reader.data.Orientation.Normal -> Icons.Rounded.ScreenRotation
    dev.folio.reader.data.Orientation.Portrait -> Icons.Rounded.StayCurrentPortrait
    dev.folio.reader.data.Orientation.Landscape -> Icons.Rounded.StayCurrentLandscape
    dev.folio.reader.data.Orientation.Dynamic -> Icons.Rounded.ScreenRotationAlt
    dev.folio.reader.data.Orientation.Locked -> Icons.Rounded.ScreenLockRotation
}

/** How this book turns: the button shows the current choice, the menu shows each with its own icon. */
@Composable
private fun RotationMenu(current: dev.folio.reader.data.Orientation, onPick: (dev.folio.reader.data.Orientation) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(current.icon(), "Rotation: ${current.label}", tint = if (current == dev.folio.reader.data.Orientation.Normal) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(open, { open = false }) {
            dev.folio.reader.data.Orientation.entries.forEach { o ->
                DropdownMenuItem(
                    leadingIcon = { Icon(o.icon(), null) },
                    text = { Column { Text(o.label, fontWeight = if (o == current) FontWeight.SemiBold else FontWeight.Normal); Text(o.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    trailingIcon = { if (o == current) Icon(Icons.Rounded.Check, null) },
                    onClick = { onPick(o); open = false },
                )
            }
        }
    }
}

@Composable
private fun ToolHeader(title: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Close, "Close", Modifier.size(18.dp)) }
    }
}

/** A slim page slider; it reports the page while you drag so the screen can name it. */
@Composable
private fun PageSlider(page: Int, pageCount: Int, modifier: Modifier, onScrub: (Int?) -> Unit, onSeek: (Int) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val maxIndex = (pageCount - 1).coerceAtLeast(1)
    Slider(
        value = dragging ?: page.toFloat(),
        onValueChange = { dragging = it; onScrub(it.roundToInt()); onSeek(it.roundToInt()) },
        onValueChangeFinished = { dragging = null; onScrub(null) },
        valueRange = 0f..maxIndex.toFloat(),
        modifier = modifier,
    )
}

/** A sun button that opens a small slider for this book's brightness. */
@Composable
private fun BrightnessButton(brightness: Float, onChange: (Float) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) { Icon(if (brightness < 0f) Icons.Rounded.BrightnessMedium else Icons.Rounded.BrightnessHigh, "Brightness", tint = if (brightness < 0f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary) }
        DropdownMenu(open, { open = false }) {
            Column(Modifier.width(240.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Brightness for this book", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.BrightnessLow, null, Modifier.size(18.dp))
                    Slider(value = if (brightness < 0f) 0.5f else brightness, onValueChange = { onChange(it.coerceIn(0.02f, 1f)) }, valueRange = 0.02f..1f, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.BrightnessHigh, null, Modifier.size(18.dp))
                }
                TextButton(onClick = { onChange(-1f) }, enabled = brightness >= 0f) { Text("Use the system brightness") }
            }
        }
    }
}

/** Applies a book's rotation while it is open, and gives the phone's own setting back afterwards. */
@Composable
private fun ReadingOrientation(orientation: dev.folio.reader.data.Orientation) {
    val activity = LocalActivity.current
    DisposableEffect(orientation) {
        activity?.requestedOrientation = when (orientation) {
            dev.folio.reader.data.Orientation.Normal -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            dev.folio.reader.data.Orientation.Portrait -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            dev.folio.reader.data.Orientation.Landscape -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            dev.folio.reader.data.Orientation.Dynamic -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            dev.folio.reader.data.Orientation.Locked -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LOCKED
        }
        onDispose { activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

@Composable
private fun JumpDialog(pages: Int, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val number = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to page") },
        text = {
            OutlinedTextField(
                text, { text = it.filter(Char::isDigit).take(6) }, singleLine = true,
                placeholder = { Text("1 to $pages") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(onClick = { number?.let { onGo((it - 1).coerceIn(0, pages - 1)) } }, enabled = number != null) { Text("Go") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The screen stays on while a book is open, at the reading brightness (negative follows the system). */
@Composable
private fun KeepScreenOn(brightness: Float) {
    val view = LocalView.current
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(brightness) {
        val window = activity?.window
        if (window != null) window.attributes = window.attributes.also { it.screenBrightness = if (brightness < 0f) -1f else brightness }
        onDispose {
            if (window != null) window.attributes = window.attributes.also { it.screenBrightness = -1f }
        }
    }
}
