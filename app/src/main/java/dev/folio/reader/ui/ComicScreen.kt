package dev.folio.reader.ui

import dev.folio.reader.R
import dev.folio.reader.s
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.epub.ComicStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Reads a comic one page at a time: swipe sideways, tap to show or hide the bar, drag the slider to jump. */
@Composable
fun ComicScreen(model: FolioViewModel, id: String) {
    val state by model.repo.state.collectAsState()
    val book = state.books.firstOrNull { it.id == id } ?: return
    val context = LocalView.current.context
    val pages by produceState<List<File>?>(null, id) { value = withContext(Dispatchers.IO) { ComicStore.open(context, Uri.parse(book.uri), id) } }
    ReadingClock(model, id)
    val list = pages
    if (list == null || list.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Text(if (list == null) s(R.string.opening) else s(R.string.no_pages_found), color = Color.White) }
        return
    }
    val pager = rememberPagerState(initialPage = book.page.coerceIn(0, list.lastIndex)) { list.size }
    var bars by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.collect { p ->
            model.pageSeen(id, p)
            model.repo.updateBook(id) { it.copy(page = p, pages = list.size, status = if (p >= list.lastIndex && list.size > 1) dev.folio.reader.data.Status.Finished else it.status) }
        }
    }
    // Comic pages keep their left-to-right order whatever the language of the app is.
    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { bars = !bars } }) { i -> ComicPage(list[i]) }
        if (bars) {
            Row(Modifier.fillMaxWidth().background(Color(0xCC000000)).statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { model.closeBook() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, s(R.string.back), tint = Color.White) }
                Text(book.name, Modifier.weight(1f), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xCC000000)).navigationBarsPadding().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = pager.currentPage.toFloat(), onValueChange = { scope.launch { pager.scrollToPage(it.toInt().coerceIn(0, list.lastIndex)) } },
                    valueRange = 0f..list.lastIndex.coerceAtLeast(1).toFloat(), modifier = Modifier.weight(1f),
                )
                Text(dev.folio.reader.digits("${pager.currentPage + 1}/${list.size}"), color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    }
}

/** One page image, decoded at screen size so a big scan does not fill memory. */
@Composable
private fun ComicPage(file: File) {
    val bitmap by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1800 || bounds.outHeight / (sample * 2) >= 2600) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
    }
}
