package dev.folio.reader.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.Book
import dev.folio.reader.data.BookLookup
import dev.folio.reader.data.BookMeta
import dev.folio.reader.data.Status
import kotlinx.coroutines.launch

/** What the editor holds while you work; nothing reaches the library until Save. */
private class Draft(book: Book?) {
    var title by mutableStateOf(book?.name.orEmpty())
    var author by mutableStateOf(book?.author.orEmpty())
    var publisher by mutableStateOf(book?.publisher.orEmpty())
    var year by mutableStateOf(book?.year.orEmpty())
    var pages by mutableStateOf(book?.pages?.takeIf { it > 0 }?.toString().orEmpty())
    var isbn by mutableStateOf(book?.isbn.orEmpty())
    var description by mutableStateOf(book?.description.orEmpty())
    /** A new cover picked or fetched, not saved yet. */
    var newCover by mutableStateOf<ByteArray?>(null)
    var dropCover by mutableStateOf(false)
}

/**
 * Edits the details and cover of a book, or adds a paper book when [book] is null. "Find online" shows what a catalogue knows,
 * lets you tick only the parts you want, and fills them in here; Save is what actually changes the library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookEditor(model: FolioViewModel, book: Book?, onDismiss: () -> Unit) {
    val draft = remember(book?.id) { Draft(book) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var finding by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null && bytes.size < 12_000_000 && BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inJustDecodeBounds = true }).let { true }) {
                draft.newCover = bytes; draft.dropCover = false
            } else model.notify("Folio could not read that image.")
        }
    }
    val changed = remember(draft.title, draft.author, draft.publisher, draft.year, draft.pages, draft.isbn, draft.description, draft.newCover, draft.dropCover) {
        book == null && draft.title.isNotBlank() || book != null && (
            draft.title != book.name || draft.author != book.author || draft.publisher != book.publisher || draft.year != book.year ||
                draft.pages != (book.pages.takeIf { it > 0 }?.toString().orEmpty()) || draft.isbn != book.isbn || draft.description != book.description ||
                draft.newCover != null || draft.dropCover
            )
    }

    Dialog(onDismissRequest = { if (changed) confirmDiscard = true else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (book == null) "Add a paper book" else "Edit details") },
                        navigationIcon = { IconButton(onClick = { if (changed) confirmDiscard = true else onDismiss() }) { Icon(Icons.Rounded.Close, "Close") } },
                        actions = {
                            TextButton(
                                enabled = draft.title.isNotBlank() && (book != null || changed),
                                onClick = { scope.launch { save(model, book, draft); onDismiss() } },
                            ) { Text("Save") }
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CoverBox(model, book, draft, Modifier.width(96.dp).height(138.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { picker.launch("image/*") }) { Icon(Icons.Rounded.Image, null); Spacer(Modifier.width(8.dp)); Text("Change cover") }
                            if (draft.newCover != null || book?.customCover == true && !draft.dropCover) {
                                TextButton(onClick = { draft.newCover = null; draft.dropCover = book?.customCover == true }) { Text("Remove cover") }
                            }
                        }
                    }
                    Button(onClick = { finding = true }, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text("Find details online") }
                    Field("Title", draft.title) { draft.title = it }
                    Field("Author", draft.author) { draft.author = it }
                    Field("Publisher", draft.publisher) { draft.publisher = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field("Year", draft.year, Modifier.weight(1f), number = true) { draft.year = it.filter(Char::isDigit).take(4) }
                        if (book == null || book.physical) Field("Pages", draft.pages, Modifier.weight(1f), number = true) { draft.pages = it.filter(Char::isDigit).take(5) }
                    }
                    Field("ISBN", draft.isbn) { draft.isbn = it }
                    Field("Description", draft.description, minLines = 3) { draft.description = it }
                    Text(
                        if (book == null || book.physical) "Details are kept in your Folio library."
                        else "Details are kept in your Folio library; the PDF file itself is not changed.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (finding) OnlineFinder(draft, startQuery = listOf(draft.isbn.ifBlank { draft.title }, if (draft.isbn.isBlank()) draft.author else "").filter { it.isNotBlank() }.joinToString(" "), onDismiss = { finding = false })
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text("Discard your changes?") },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
    )
}

@Composable
private fun CoverBox(model: FolioViewModel, book: Book?, draft: Draft, modifier: Modifier) {
    val picked = remember(draft.newCover) { draft.newCover?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val current by androidx.compose.runtime.produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, book?.id, book?.coverRev, draft.dropCover) {
        value = if (book == null || draft.dropCover) null else model.covers.get(book)?.asImageBitmap()
    }
    val shown = picked ?: current
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        if (shown != null) Image(shown, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), number: Boolean = false, minLines: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, modifier, label = { Text(label) }, minLines = minLines, singleLine = minLines == 1,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
    )
}

private suspend fun save(model: FolioViewModel, book: Book?, d: Draft) {
    val pages = d.pages.toIntOrNull() ?: 0
    val id = book?.id ?: java.util.UUID.randomUUID().toString()
    var coverSet = false
    d.newCover?.let { coverSet = model.covers.setCustom(id, it) }
    if (d.dropCover && d.newCover == null) model.covers.removeCustom(id)
    val keepCustom = coverSet || (book?.customCover == true && !d.dropCover)
    if (book == null) {
        model.repo.add(
            Book(
                id = id, uri = "", name = d.title.trim(), kind = dev.folio.reader.data.Kind.Pdf, physical = true, pages = pages,
                added = System.currentTimeMillis(), status = Status.ToRead, author = d.author.trim(), publisher = d.publisher.trim(),
                year = d.year, isbn = d.isbn.trim(), description = d.description.trim(), customCover = keepCustom, coverRev = 1,
            ),
        )
    } else {
        model.repo.updateBook(id) {
            it.copy(
                name = d.title.trim(), author = d.author.trim(), publisher = d.publisher.trim(), year = d.year, isbn = d.isbn.trim(),
                description = d.description.trim(), pages = if (it.physical) pages else it.pages, customCover = keepCustom,
                coverRev = if (coverSet || d.dropCover) it.coverRev + 1 else it.coverRev,
            )
        }
    }
}

/** Search, pick a result, then choose which of its parts to take; "Use selected" only fills the editor. */
@Composable
private fun OnlineFinder(draft: Draft, startQuery: String, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(startQuery) }
    var results by remember { mutableStateOf<List<BookMeta>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<BookMeta?>(null) }
    val scope = rememberCoroutineScope()
    fun run() {
        busy = true; error = null
        scope.launch {
            results = runCatching { BookLookup.search(query) }.onFailure { error = "No connection, or the catalogue did not answer." }.getOrNull()
            busy = false
        }
    }
    val chosen = preview
    if (chosen != null) {
        MetaPreview(draft, chosen, onBack = { preview = null }, onDone = onDismiss)
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find details online") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Title, author or ISBN") }, singleLine = true)
                Button(onClick = ::run, enabled = query.isNotBlank() && !busy) { Text("Search Open Library") }
                Text("Only what you type here is sent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                results?.let { list ->
                    if (list.isEmpty() && !busy) Text("Nothing found.")
                    Column(Modifier.height(260.dp).verticalScroll(rememberScrollState())) {
                        list.forEach { m ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                scope.launch { busy = true; preview = BookLookup.describe(m); busy = false }
                            }.padding(vertical = 8.dp, horizontal = 4.dp)) {
                                Text(m.title, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(listOf(m.author, m.year, m.publisher).filter { it.isNotBlank() }.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** One result against what the editor holds now, each part with its own tick. */
@Composable
private fun MetaPreview(draft: Draft, meta: BookMeta, onBack: () -> Unit, onDone: () -> Unit) {
    class Row(val label: String, val now: String, val next: String, val apply: () -> Unit)
    val rows = listOf(
        Row("Title", draft.title, meta.title) { draft.title = meta.title },
        Row("Author", draft.author, meta.author) { draft.author = meta.author },
        Row("Publisher", draft.publisher, meta.publisher) { draft.publisher = meta.publisher },
        Row("Year", draft.year, meta.year) { draft.year = meta.year },
        Row("Pages", draft.pages, meta.pages.takeIf { it > 0 }?.toString().orEmpty()) { draft.pages = meta.pages.toString() },
        Row("ISBN", draft.isbn, meta.isbn) { draft.isbn = meta.isbn },
        Row("Description", draft.description, meta.description) { draft.description = meta.description },
    ).filter { it.next.isNotBlank() }
    val ticked = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>().apply { rows.forEach { put(it.label, it.next != it.now) } } }
    var takeCover by remember { mutableStateOf(meta.coverUrl.isNotEmpty()) }
    var coverBytes by remember { mutableStateOf<ByteArray?>(null) }
    var coverLoading by remember { mutableStateOf(meta.coverUrl.isNotEmpty()) }
    androidx.compose.runtime.LaunchedEffect(meta.coverUrl) {
        if (meta.coverUrl.isNotEmpty()) { coverBytes = BookLookup.cover(meta.coverUrl); coverLoading = false; if (coverBytes == null) takeCover = false }
    }
    AlertDialog(
        onDismissRequest = onBack,
        title = { Text("Choose what to use") },
        text = {
            Column(Modifier.height(380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (meta.coverUrl.isNotEmpty()) Row(Modifier.fillMaxWidth().clickable(enabled = coverBytes != null) { takeCover = !takeCover }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(takeCover && coverBytes != null, { takeCover = it }, enabled = coverBytes != null)
                    val bmp = remember(coverBytes) { coverBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
                    Box(Modifier.width(60.dp).height(86.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else if (coverLoading) CircularProgressIndicator(Modifier.width(22.dp).height(22.dp)) else Text("No cover")
                    }
                    Text("  Cover", style = MaterialTheme.typography.labelLarge)
                }
                if (rows.isEmpty() && meta.coverUrl.isEmpty()) Text("This result has nothing to add.")
                rows.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { ticked[r.label] = ticked[r.label] != true }, verticalAlignment = Alignment.Top) {
                        Checkbox(ticked[r.label] == true, { ticked[r.label] = it })
                        Column(Modifier.padding(top = 10.dp)) {
                            Text(r.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(r.next, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            if (r.now.isNotBlank() && r.now != r.next) Text("Now: ${r.now}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                rows.filter { ticked[it.label] == true }.forEach { it.apply() }
                if (takeCover) coverBytes?.let { draft.newCover = it; draft.dropCover = false }
                onDone()
            }) { Text("Use selected") }
        },
        dismissButton = { TextButton(onClick = onBack) { Text("Back") } },
    )
}

/** Logs a sitting with a paper book: where you are now, or how many pages you read, and optionally how long it took. */
@Composable
fun LogSitting(model: FolioViewModel, book: Book, onDismiss: () -> Unit) {
    var byCount by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(book.page.toString()) }
    var minutes by remember { mutableStateOf("") }
    val value = input.toIntOrNull()
    val target = when {
        value == null -> null
        byCount -> book.page + value
    else -> value
    }?.let { if (book.pages > 0) it.coerceIn(0, book.pages) else it }
    val gained = target?.let { (it - book.page).coerceAtLeast(0) } ?: 0
    val percent = if (book.pages > 0 && target != null) (target * 100 / book.pages) else null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(book.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("I am on page", "I read pages").forEachIndexed { i, label ->
                        SegmentedButton(
                            selected = byCount == (i == 1),
                            onClick = { byCount = i == 1; input = if (byCount) "" else book.page.toString() },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            label = { Text(label, maxLines = 1) },
                        )
                    }
                }
                OutlinedTextField(
                    input, { input = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(if (byCount) "Pages read this sitting" else "Page you are on now") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    minutes, { minutes = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Minutes (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    buildString {
                        append(if (gained > 0) "+$gained page${if (gained == 1) "" else "s"}" else "No new pages")
                        if (target != null) append("  ·  now on page $target")
                        if (book.pages > 0) append(" of ${book.pages}")
                        if (percent != null) append("  ·  $percent%")
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                )
                if (book.pages <= 0) Text("Add the page count in Edit details to see your progress.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = target != null, onClick = { model.repo.logSitting(book.id, target ?: book.page, minutes.toIntOrNull() ?: 0); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
