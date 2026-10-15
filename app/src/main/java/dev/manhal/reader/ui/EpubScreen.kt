package dev.manhal.reader.ui

import dev.manhal.reader.R
import androidx.compose.material.icons.rounded.Search
import dev.manhal.reader.s
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.manhal.reader.ManhalViewModel
import dev.manhal.reader.data.PageTheme
import dev.manhal.reader.epub.EpubPackage
import dev.manhal.reader.epub.EpubStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Reads an EPUB one chapter at a time: the book is unpacked once, and each chapter is shown in a page with the reading theme and text size applied. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubScreen(model: ManhalViewModel, id: String) {
    val state by model.repo.state.collectAsState()
    val book = state.books.firstOrNull { it.id == id } ?: return
    val context = LocalView.current.context
    val opened by produceState<Pair<EpubPackage, File>?>(null, id) { value = withContext(Dispatchers.IO) { EpubStore.open(context, Uri.parse(book.uri), id) } }
    ReadingClock(model, id)
    val theme = state.theme
    val percent = state.epubTextPercent
    val (ink, paper) = theme.colors()
    val pkg = opened?.first
    if (pkg == null) {
        Box(Modifier.fillMaxSize().background(paper), contentAlignment = Alignment.Center) { Text(if (opened == null) "Opening…" else "", color = ink) }
        return
    }
    var chapter by remember(id) { mutableStateOf(book.page.coerceIn(0, pkg.spine.lastIndex)) }
    var offset by remember(id) { mutableStateOf(book.offset) }
    var web by remember { mutableStateOf<WebView?>(null) }
    var searching by remember { mutableStateOf(false) }
    // A word to highlight once the chapter it was found in has loaded.
    var pendingFind by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(chapter) { model.pageSeen(id, chapter) }

    fun save() {
        val w = web ?: return
        val range = (w.contentHeight * w.resources.displayMetrics.density - w.height).coerceAtLeast(1f)
        offset = (w.scrollY / range).coerceIn(0f, 1f)
        model.repo.updateBook(id) { it.copy(page = chapter, offset = offset, pages = pkg.spine.size) }
    }
    DisposableEffect(id) { onDispose { save(); web?.destroy() } }

    Column(Modifier.fillMaxSize().background(paper).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { save(); model.closeBook() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ink) }
            Text(pkg.title.ifBlank { book.name }, Modifier.weight(1f), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { searching = true }) { Icon(Icons.Rounded.Search, s(R.string.search_in_this_book), tint = ink) }
            androidx.compose.material3.TextButton(onClick = { model.repo.update { it.copy(epubTextPercent = (percent - 10).coerceAtLeast(70)) } }) { Text("A−", color = ink, modifier = Modifier.semantics { contentDescription = s(R.string.smaller_text) }) }
            androidx.compose.material3.TextButton(onClick = { model.repo.update { it.copy(epubTextPercent = (percent + 10).coerceAtMost(220)) } }) { Text("A+", color = ink, modifier = Modifier.semantics { contentDescription = s(R.string.larger_text) }) }
            IconButton(onClick = { model.repo.update { it.copy(theme = theme.next()) } }) { Icon(if (theme == PageTheme.Normal) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, s(R.string.page_theme), tint = ink) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = true
                        settings.blockNetworkLoads = true
                        settings.setSupportZoom(false)
                        overScrollMode = View.OVER_SCROLL_NEVER
                        webViewClient = object : WebViewClient() {
                            // Links inside the book load here; anything else (the web, other apps) is ignored.
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = !request.url.toString().startsWith("file://")
                            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                                if (request.url.scheme == "file") null else WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
                            override fun onPageFinished(view: WebView, url: String?) {
                                pendingFind?.let { view.findAllAsync(it); pendingFind = null }
                                view.postDelayed({
                                    val range = (view.contentHeight * view.resources.displayMetrics.density - view.height).coerceAtLeast(0f)
                                    view.scrollTo(0, (offset * range).toInt())
                                }, 60)
                            }
                        }
                        web = this
                    }
                },
                update = { w ->
                    val key = "$chapter|$percent|${theme.name}"
                    if (w.tag == key) return@AndroidView
                    w.tag = key
                    val file = EpubStore.file(context, id, pkg, pkg.spine[chapter])
                    val html = runCatching { file.readText() }.getOrDefault("<p>This chapter could not be read.</p>")
                    val css = "<style>html,body{background:${paper.css()} !important;color:${ink.css()} !important;}body{margin:0;padding:18px 22px 48px;line-height:1.55;font-size:${percent}% !important;}img{max-width:100%;height:auto;}a{color:${ink.css()};}</style>"
                    val withCss = if (html.contains("</head>", true)) html.replace(Regex("</head>", RegexOption.IGNORE_CASE), css + "</head>") else css + html
                    w.loadDataWithBaseURL("file://${file.parentFile?.absolutePath}/", withCss, "text/html", "UTF-8", null)
                },
            )
        }
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(enabled = chapter > 0, onClick = { save(); offset = 0f; chapter -= 1; save() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, s(R.string.previous_chapter), tint = ink) }
            Slider(
                value = chapter.toFloat(), onValueChange = { chapter = it.toInt().coerceIn(0, pkg.spine.lastIndex); offset = 0f },
                valueRange = 0f..pkg.spine.lastIndex.coerceAtLeast(1).toFloat(), onValueChangeFinished = { save() }, modifier = Modifier.weight(1f),
            )
            Text(dev.manhal.reader.digits("${chapter + 1}/${pkg.spine.size}"), color = ink, style = MaterialTheme.typography.labelMedium)
            IconButton(enabled = chapter < pkg.spine.lastIndex, onClick = { save(); offset = 0f; chapter += 1; save() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, s(R.string.next_chapter), tint = ink) }
        }
    }
    if (searching) BookSearchSheet(
        search = { q -> dev.manhal.reader.data.BookSearchSources.epub(context, id, pkg, q) },
        where = { s(R.string.search_chapter_hit, it.unit + 1) },
        onPick = { hit, q ->
            searching = false
            if (hit.unit == chapter) web?.findAllAsync(q) else { save(); offset = 0f; pendingFind = q; chapter = hit.unit }
        },
        onDismiss = { searching = false },
    )
}

/** Ink and paper for each reading theme. */
private fun PageTheme.colors(): Pair<Color, Color> = when (this) {
    PageTheme.Normal -> Color(0xFF1C1B17) to Color(0xFFF7F2E8)
    PageTheme.Night -> Color(0xFFD8D4C8) to Color(0xFF0A0A0A)
    PageTheme.SoftNight -> Color(0xFFCFC8B4) to Color(0xFF1B1912)
    PageTheme.Sepia -> Color(0xFF3B2F1E) to Color(0xFFE9DCC0)
}

private fun Color.css(): String = "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
