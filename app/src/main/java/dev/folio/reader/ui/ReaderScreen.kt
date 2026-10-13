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
    DisposableEffect(opened) { onDispose { opened?.close() } }

    var barsVisible by remember { mutableStateOf(true) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    val theme = state.theme
    ImmersiveBars(barsVisible)
    KeepScreenOn()
    var themeSheet by remember { mutableStateOf(false) }
    var bookmarkSheet by remember { mutableStateOf(false) }
    var jumpDialog by remember { mutableStateOf(false) }
    val jumpTo = remember { mutableStateOf<Int?>(null) }

    Box(Modifier.fillMaxSize().background(theme.background())) {
        val file = pdf
        if (file == null) {
            Text("Opening…", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val list = rememberLazyListState()
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                val widthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }.roundToInt()
                val cache = remember(file) { object : LruCache<String, Bitmap>(96 * 1024 * 1024) { override fun sizeOf(k: String, v: Bitmap) = v.byteCount } }

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
                        PageView(file, index, widthPx, zoom, cache, theme)
                    }
                }
            }
            val current = (list.firstVisibleItemIndex + 1).coerceIn(1, file.pageCount)
            AnimatedVisibility(
                barsVisible,
                Modifier.align(Alignment.TopCenter),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), tonalElevation = 3.dp) {
                    Row(Modifier.statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { model.closeBook() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                        Text(book.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val marked = (current - 1) in book.bookmarks
                        IconButton(onClick = { model.repo.toggleBookmark(id, current - 1) }) {
                            Icon(if (marked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, if (marked) "Remove bookmark" else "Add bookmark", tint = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { bookmarkSheet = true }) { Icon(Icons.Rounded.Bookmarks, "Bookmarks") }
                        IconButton(onClick = { themeSheet = true }) { Icon(Icons.Rounded.Contrast, "Page theme") }
                    }
                }
            }
            AnimatedVisibility(
                barsVisible,
                Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), tonalElevation = 3.dp) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Page $current of ${file.pageCount}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { jumpDialog = true }.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                            Text(theme.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        var dragging by remember { mutableStateOf<Float?>(null) }
                        val scope = rememberCoroutineScope()
                        Slider(
                            value = dragging ?: (current - 1).toFloat(),
                            onValueChange = { dragging = it },
                            onValueChangeFinished = {
                                dragging?.let { target -> scope.launch { list.scrollToItem(target.roundToInt()) } }
                                dragging = null
                            },
                            valueRange = 0f..(file.pageCount - 1).coerceAtLeast(1).toFloat(),
                        )
                    }
                }
            }
        }
    }
    val pages = pdf?.pageCount ?: 0
    if (themeSheet) ThemeSheet(theme, onPick = { picked -> model.repo.update { it.copy(theme = picked) } }, onDismiss = { themeSheet = false })
    if (bookmarkSheet) BookmarkSheet(
        book.bookmarks,
        onGo = { page -> jumpTo.value = page; bookmarkSheet = false },
        onRemove = { page -> model.repo.toggleBookmark(id, page) },
        onDismiss = { bookmarkSheet = false },
    )
    if (jumpDialog && pages > 0) JumpDialog(pages, onGo = { page -> jumpTo.value = page; jumpDialog = false }, onDismiss = { jumpDialog = false })
}

/** One page: a placeholder of the right shape until its bitmap is drawn. */
@Composable
private fun PageView(file: PdfFile, index: Int, widthPx: Int, zoom: Float, cache: LruCache<String, Bitmap>, theme: PageTheme) {
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
private fun BookmarkSheet(pages: List<Int>, onGo: (Int) -> Unit, onRemove: (Int) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text("Bookmarks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (pages.isEmpty()) {
                Text("Tap the bookmark icon on a page to mark it.", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pages.forEach { page ->
                Row(Modifier.fillMaxWidth().clickable { onGo(page) }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Text("Page ${page + 1}", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { onRemove(page) }) { Text("Remove") }
                }
            }
        }
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

/** The screen stays on while a book is open. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
