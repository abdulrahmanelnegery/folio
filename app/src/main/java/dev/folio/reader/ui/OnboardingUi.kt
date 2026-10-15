package dev.folio.reader.ui

import dev.folio.reader.data.label
import dev.folio.reader.data.note
import dev.folio.reader.R
import dev.folio.reader.s
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.Onboarding
import dev.folio.reader.data.PageTheme
import dev.folio.reader.data.ReleaseNotes
import dev.folio.reader.data.Welcome
import kotlinx.coroutines.launch

/** Opens the first-run tour on a fresh install (or when asked from Settings) and the one-time notes after an update. */
@Composable
fun WelcomeHost(model: FolioViewModel) {
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    val library by model.repo.state.collectAsState()
    val requested by model.tourRequested.collectAsState()
    // Decided once, when Rukn opens: finishing the tour changes what is stored.
    val decision = remember { Onboarding.decide(library.lastSeenVersion, library.tourDone, library.books.isNotEmpty() || library.lists.isNotEmpty(), version) }
    val newsRequested by model.whatsNewRequested.collectAsState()
    var notesClosed by remember { androidx.compose.runtime.mutableStateOf(false) }
    var tourClosed by remember { androidx.compose.runtime.mutableStateOf(false) }
    fun seen() = model.repo.update { it.copy(lastSeenVersion = version, tourDone = true) }
    if (requested || (decision == Welcome.Tour && !tourClosed)) {
        Tour(model, onClose = { seen(); tourClosed = true; model.tourRequested.value = false })
    } else if (newsRequested) {
        val notes = ReleaseNotes.forVersion(version)
        AlertDialog(
            onDismissRequest = { model.whatsNewRequested.value = false },
            title = { Text(s(R.string.what_s_new_in_rukn, version)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { notes.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) } } },
            confirmButton = { TextButton(onClick = { model.whatsNewRequested.value = false }) { Text(s(R.string.close)) } },
        )
    } else if (decision == Welcome.WhatsNew && !notesClosed) {
        val notes = ReleaseNotes.forVersion(version)
        if (notes.isEmpty()) LaunchedEffect(Unit) { seen(); notesClosed = true } else AlertDialog(
            onDismissRequest = { seen(); notesClosed = true },
            title = { Text(s(R.string.what_s_new_in_rukn, version)) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { notes.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) } } },
            confirmButton = { TextButton(onClick = { seen(); notesClosed = true }) { Text(s(R.string.close)) } },
        )
    }
}

private data class TourPage(val title: String, val body: String)

private fun pages() = listOf(
    TourPage(s(R.string.welcome_to_rukn), s(R.string.a_calm_reader_for_pdfs_6d5a)),
    TourPage(s(R.string.your_books_your_way), s(R.string.add_books_from_the_file)),
    TourPage(s(R.string.where_your_data_lives), s(R.string.by_default_inside_the_app)),
    TourPage(s(R.string.pick_a_reading_theme), s(R.string.choose_how_pages_look_you)),
    TourPage(s(R.string.you_are_ready), s(R.string.open_the_menu_for_shelves)),
)

@Composable
private fun Tour(model: FolioViewModel, onClose: () -> Unit) {
    val state = rememberPagerState { pages().size }
    val scope = rememberCoroutineScope()
    val library by model.repo.state.collectAsState()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text(s(R.string.skip)) }
            }
            HorizontalPager(state, Modifier.weight(1f).fillMaxWidth()) { i ->
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Text(pages()[i].title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(16.dp))
                    Text(pages()[i].body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (i == 3) {
                        Spacer(Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PageTheme.entries.forEach { t ->
                                FilterChip(library.theme == t, { model.repo.update { it.copy(theme = t) } }, label = { Text(t.label) })
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages().size) { i ->
                        Box(Modifier.size(8.dp).background(if (i == state.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape))
                    }
                }
                val last = state.currentPage == pages().lastIndex
                Button(onClick = { if (last) onClose() else scope.launch { state.animateScrollToPage(state.currentPage + 1) } }) { Text(if (last) s(R.string.done) else s(R.string.next)) }
            }
        }
    }
}
