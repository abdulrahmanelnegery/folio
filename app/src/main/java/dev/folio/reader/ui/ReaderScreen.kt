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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
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

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.2f

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
    var panX by remember { mutableFloatStateOf(0f) }
    val theme = state.theme
    ImmersiveBars(barsVisible)
    ReadingClock(model, id)
    var themeSheet by remember { mutableStateOf(false) }
    var bookmarkSheet by remember { mutableStateOf(false) }
    var jumpDialog by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf<NoteEdit?>(null) }
    var menu by remember { mutableStateOf(false) }
    var sizesVersion by remember { mutableStateOf(0) }
    var autoScroll by remember { mutableStateOf(false) }
    var autoSpeed by remember { mutableFloatStateOf(70f) }
    var tool by remember { mutableStateOf<ReaderTool?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val jumpTo = remember { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize().background(theme.background())) {
        val file = pdf
        if (file == null) {
            Text("Opening…", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val list = rememberLazyListState()
            // A page counts as read once the reader stays on it for a moment.
            LaunchedEffect(file) {
                snapshotFlow { list.firstVisibleItemIndex }.debounce(1500).collect { model.pageSeen(id, it) }
            }
            // The other pages' real sizes arrive after the book is already showing.
            LaunchedEffect(file) {
                kotlinx.coroutines.delay(600)
                if (file.readAllSizes()) sizesVersion++
            }
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }.roundToInt()
                val cache = remember(file) { object : LruCache<String, Bitmap>(48 * 1024 * 1024) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount } }

                // Pick up where the book was left: the page and how far down it.
                LaunchedEffect(file) {
                    val firstPx = (widthPx * file.aspect(book.page)).roundToInt()
                    list.scrollToItem(book.page.coerceIn(0, file.pageCount - 1), (book.offset * firstPx).roundToInt())
                }
                LaunchedEffect(jumpTo.value) {
                    jumpTo.value?.let { list.scrollToItem(it.coerceIn(0, file.pageCount - 1)); jumpTo.value = null }
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
                        .graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panX; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { barsVisible = !barsVisible },
                                onDoubleTap = { zoom = if (zoom > 1.05f) 1f else DOUBLE_TAP_ZOOM; panX = 0f },
                            )
                        }
                        .pointerInput(Unit) {
                            // Two fingers pinch and pan; one finger is left to the list so it scrolls.
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                do {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.size >= 2) {
                                        zoom = (zoom * event.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                                        val limit = size.width * (zoom - 1f) / 2f
                                        panX = (panX + event.calculatePan().x).coerceIn(-limit, limit)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(file.pageCount) { index ->
                        @Suppress("UNUSED_VARIABLE") val layoutVersion = sizesVersion
                        PageView(file, index, widthPx, zoom, cache, theme, noteCount = book.comments.count { it.page == index }, onNotes = { bookmarkSheet = true })
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
                    Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { model.closeBook() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                        Text(book.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val marked = (current - 1) in book.bookmarks
                        IconButton(onClick = { model.repo.toggleBookmark(id, current - 1) }) {
                            Icon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (marked) "Remove bookmark" else "Add bookmark", tint = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { editNote = NoteEdit(null, current - 1, "") }) { Icon(Icons.Rounded.AddComment, "Comment on this page") }
                        ThemeToggle(theme, onToggle = { model.repo.update { it.copy(theme = if (it.theme == PageTheme.Normal) PageTheme.Night else PageTheme.Normal) } }, onMore = { themeSheet = true })
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "Reading options") }
                            DropdownMenu(menu, { menu = false }) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.PlayCircle, null) },
                                    text = { Text(if (autoScroll) "Stop auto-scroll" else "Auto-scroll") },
                                    onClick = { menu = false; autoScroll = !autoScroll; tool = if (autoScroll) ReaderTool.AutoScroll else null; barsVisible = !autoScroll },
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.LightMode, null) },
                                    text = { Text("Brightness") },
                                    onClick = { menu = false; tool = ReaderTool.Brightness },
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.Bookmarks, null) },
                                    text = { Text("Bookmarks and notes") },
                                    onClick = { menu = false; bookmarkSheet = true },
                                )
                            }
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
                            ReaderTool.Brightness -> {
                                ToolHeader("Brightness", onClose = { tool = null })
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.BrightnessLow, null, Modifier.size(20.dp))
                                    Slider(
                                        value = if (state.brightness < 0f) 0.5f else state.brightness,
                                        onValueChange = { v -> model.repo.update { it.copy(brightness = v.coerceIn(0.02f, 1f)) } },
                                        valueRange = 0.02f..1f,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(Icons.Rounded.BrightnessHigh, null, Modifier.size(20.dp))
                                }
                                TextButton(onClick = { model.repo.update { it.copy(brightness = -1f) } }, enabled = state.brightness >= 0f) { Text("Use the system brightness") }
                            }
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
                Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f), tonalElevation = 3.dp, shadowElevation = 6.dp) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Page $current of ${file.pageCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { jumpDialog = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            Spacer(Modifier.weight(1f))
                            Text("${(current * 100f / file.pageCount).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PageSlider(current - 1, file.pageCount) { page -> list.requestScrollToItem(page) }
                    }
                }
            }
        }
    }
    val pages = pdf?.pageCount ?: 0
    KeepScreenOn(state.brightness)
    if (themeSheet) ThemeSheet(theme, onPick = { picked -> model.repo.update { it.copy(theme = picked) } }, onDismiss = { themeSheet = false })
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

private data class NoteEdit(val id: Long?, val page: Int, val text: String)

private enum class ReaderTool { Brightness, AutoScroll }

/** One page: a placeholder of the right shape until its bitmap is drawn. */
@Composable
private fun PageView(file: PdfFile, index: Int, widthPx: Int, zoom: Float, cache: LruCache<String, Bitmap>, theme: PageTheme, noteCount: Int, onNotes: () -> Unit) {
    // Sharper pixels when zoomed, in steps so a pinch does not re-render every frame.
    val target = (widthPx * (if (zoom > 1.6f) 2.5f else if (zoom > 1.05f) 1.6f else 1f)).roundToInt().coerceAtMost(3000)
    val bitmap by produceState<Bitmap?>(cache.get("$index@$target") ?: cache.get("$index@$widthPx"), index, target) {
        cache.get("$index@$target")?.let { value = it; return@produceState }
        file.render(index, target)?.also { cache.put("$index@$target", it); value = it }
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1f / file.aspect(index)).background(Color.White)) {
        if (bitmap == null) {
            Text("${index + 1}", Modifier.align(Alignment.Center), color = Color(0xFFB8B2A4), style = MaterialTheme.typography.displaySmall)
        }
        if (noteCount > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF2B12E)).clickable(onClick = onNotes).padding(horizontal = 8.dp, vertical = 4.dp),
            ) { Text("✎ $noteCount", color = Color(0xFF3B2A00), style = MaterialTheme.typography.labelMedium) }
        }
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = theme.filter()) }
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
private fun ThemeSheet(current: PageTheme, onPick: (PageTheme) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text("Page theme", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PageTheme.entries.forEach { t ->
                    val (paper, ink) = when (t) {
                        PageTheme.Normal -> Color.White to Color(0xFF222222)
                        PageTheme.Night -> Color(0xFF0C0C0C) to Color(0xFFF0F0F0)
                        PageTheme.SoftNight -> Color(0xFF201E1A) to Color(0xFFD6CFBE)
                        PageTheme.Sepia -> Color(0xFFF4E8D0) to Color(0xFF3B3226)
                    }
                    Column(Modifier.weight(1f).clickable { onPick(t) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(paper)
                                .border(if (t == current) 3.dp else 1.dp, if (t == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Aa", color = ink, style = MaterialTheme.typography.titleLarge)
                            if (t == current) Icon(Icons.Rounded.Check, null, Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(t.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
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

/** Sun or moon: one tap flips between the plain page and night; long press picks any theme. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ThemeToggle(theme: PageTheme, onToggle: () -> Unit, onMore: () -> Unit) {
    val night = theme != PageTheme.Normal
    Box(
        Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).combinedClickable(onClick = onToggle, onLongClick = onMore),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (night) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (night) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                if (night) "Switch to the plain page" else "Switch to night",
                Modifier.size(22.dp),
                tint = if (night) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
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

/** A page slider with a bubble over the thumb that names the page while you drag. */
@Composable
private fun PageSlider(page: Int, pageCount: Int, onSeek: (Int) -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val maxIndex = (pageCount - 1).coerceAtLeast(1)
    val value = dragging ?: page.toFloat()
    BoxWithConstraints(Modifier.fillMaxWidth().height(60.dp)) {
        val fraction = (value / maxIndex).coerceIn(0f, 1f)
        val track = maxWidth - 32.dp
        if (dragging != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.TopStart).offset(x = 16.dp + track * fraction - 26.dp, y = 0.dp).width(52.dp),
            ) {
                Text("${value.roundToInt() + 1}", Modifier.padding(vertical = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
            }
        }
        Slider(
            value = value,
            onValueChange = { dragging = it; onSeek(it.roundToInt()) },
            onValueChangeFinished = { dragging = null },
            valueRange = 0f..maxIndex.toFloat(),
            modifier = Modifier.align(Alignment.BottomStart),
        )
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
