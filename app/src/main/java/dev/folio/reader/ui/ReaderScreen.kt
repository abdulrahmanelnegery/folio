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
import androidx.compose.material.icons.rounded.Contrast
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
    DisposableEffect(pdf) { onDispose { pdf?.close() } }

    var barsVisible by remember { mutableStateOf(true) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    val theme = state.theme
    ImmersiveBars(barsVisible)

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
                LaunchedEffect(file) {
                    snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                        .debounce(600)
                        .collect { (page, px) ->
                            val height = (widthPx * file.aspect(page)).coerceAtLeast(1f)
                            model.repo.updateBook(id) { it.copy(page = page, offset = (px / height).coerceIn(0f, 1f), pages = file.pageCount) }
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
                        IconButton(onClick = { model.repo.update { it.copy(theme = it.theme.next()) } }) {
                            Icon(Icons.Rounded.Contrast, "Page theme: ${theme.label}")
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
                Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), tonalElevation = 3.dp) {
                    Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Page $current of ${file.pageCount}", style = MaterialTheme.typography.labelLarge)
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
