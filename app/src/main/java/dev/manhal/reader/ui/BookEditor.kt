package dev.manhal.reader.ui

import dev.manhal.reader.sp
import dev.manhal.reader.R
import dev.manhal.reader.s
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
import dev.manhal.reader.ManhalViewModel
import dev.manhal.reader.data.Book
import dev.manhal.reader.data.BookLookup
import dev.manhal.reader.data.BookMeta
import dev.manhal.reader.data.Status
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
    /** Where you are: a page counted from 1 (for a paper book, the pages read so far). */
    var readingPage by mutableStateOf(book?.let { if (it.physical) it.page else it.page + 1 }?.takeIf { it > 0 }?.toString().orEmpty())
    var status by mutableStateOf(book?.status ?: Status.ToRead)
    var minutes by mutableStateOf(book?.readSeconds?.let { (it / 60).toString() }.orEmpty())

    /** The names of the details that differ from [book], for the question before saving. */
    fun changes(book: Book): List<String> = buildList {
        if (title != book.name) add(s(R.string.field_title))
        if (author != book.author) add(s(R.string.field_author))
        if (publisher != book.publisher) add(s(R.string.field_publisher))
        if (year != book.year) add(s(R.string.field_year))
        if (book.physical && pages != (book.pages.takeIf { it > 0 }?.toString().orEmpty())) add(s(R.string.field_pages))
        if (isbn != book.isbn) add(s(R.string.field_isbn))
        if (description != book.description) add(s(R.string.field_description))
        if (newCover != null || dropCover) add(s(R.string.field_cover))
        if (readingPage != (if (book.physical) book.page else book.page + 1).takeIf { it > 0 }?.toString().orEmpty()) add(s(R.string.field_reading_page))
        if (status != book.status) add(s(R.string.field_status))
        if (minutes != (book.readSeconds / 60).toString().takeIf { book.readSeconds > 0 }.orEmpty()) add(s(R.string.field_time_read))
    }
}

/**
 * Edits the details and cover of a book, or adds a paper book when [book] is null. "Find online" shows what a catalogue knows,
 * lets you tick only the parts you want, and fills them in here; Save is what actually changes the library.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookEditor(model: ManhalViewModel, book: Book?, onDismiss: () -> Unit) {
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
            } else model.notify(s(R.string.manhal_could_not_read_that))
        }
    }
    var confirmSave by remember { mutableStateOf(false) }
    val changed = book == null && draft.title.isNotBlank() || book != null && draft.changes(book).isNotEmpty()

    Dialog(onDismissRequest = { if (changed) confirmDiscard = true else onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (book == null) s(R.string.add_a_paper_book) else s(R.string.edit_details)) },
                        navigationIcon = { IconButton(onClick = { if (changed) confirmDiscard = true else onDismiss() }) { Icon(Icons.Rounded.Close, s(R.string.close)) } },
                        actions = {
                            TextButton(
                                enabled = draft.title.isNotBlank() && (book != null || changed),
                                // Changing a book that is already in the library asks first, and says what will change.
                                onClick = { if (book == null) scope.launch { save(model, book, draft); onDismiss() } else confirmSave = true },
                            ) { Text(s(R.string.save)) }
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CoverBox(model, book, draft, Modifier.width(96.dp).height(138.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { picker.launch("image/*") }) { Icon(Icons.Rounded.Image, null); Spacer(Modifier.width(8.dp)); Text(s(R.string.change_cover)) }
                            if (draft.newCover != null || book?.customCover == true && !draft.dropCover) {
                                TextButton(onClick = { draft.newCover = null; draft.dropCover = book?.customCover == true }) { Text(s(R.string.remove_cover)) }
                            }
                        }
                    }
                    Button(onClick = { finding = true }, Modifier.fillMaxWidth()) { Icon(Icons.Rounded.CloudDownload, null); Spacer(Modifier.width(8.dp)); Text(s(R.string.find_details_online)) }
                    Field(s(R.string.title), draft.title) { draft.title = it }
                    Field(s(R.string.author), draft.author) { draft.author = it }
                    Field(s(R.string.publisher), draft.publisher) { draft.publisher = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field(s(R.string.year), draft.year, Modifier.weight(1f), number = true) { draft.year = it.filter(Char::isDigit).take(4) }
                        if (book == null || book.physical) Field(s(R.string.pages), draft.pages, Modifier.weight(1f), number = true) { draft.pages = it.filter(Char::isDigit).take(5) }
                    }
                    Field(s(R.string.isbn), draft.isbn) { draft.isbn = it }
                    Field(s(R.string.description), draft.description, minLines = 3) { draft.description = it }
                    if (book != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Field(if (book.kind == dev.manhal.reader.data.Kind.Epub) s(R.string.reading_chapter) else s(R.string.reading_page), draft.readingPage, Modifier.weight(1f), number = true) { draft.readingPage = it.filter(Char::isDigit).take(5) }
                            Field(s(R.string.time_read_minutes), draft.minutes, Modifier.weight(1f), number = true) { draft.minutes = it.filter(Char::isDigit).take(6) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(Status.New to s(R.string.new_label), Status.ToRead to s(R.string.to_read), Status.Reading to s(R.string.reading), Status.Finished to s(R.string.finished)).forEach { (st, label) ->
                                androidx.compose.material3.FilterChip(draft.status == st, { draft.status = st }, { Text(label) })
                            }
                        }
                    }
                    Text(
                        if (book == null || book.physical) s(R.string.details_are_kept_in_your)
                        else s(R.string.details_are_kept_in_your_1158),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (finding) OnlineFinder(draft, model.repo.state.value.googleBooksKey, startQuery = listOf(draft.isbn.ifBlank { draft.title }, if (draft.isbn.isBlank()) draft.author else "").filter { it.isNotBlank() }.joinToString(" "), onDismiss = { finding = false })
    if (confirmSave && book != null) AlertDialog(
        onDismissRequest = { confirmSave = false },
        title = { Text(s(R.string.save_these_changes)) },
        text = { Text(s(R.string.this_will_change_the_book, draft.changes(book).joinToString(", "))) },
        confirmButton = { TextButton(onClick = { confirmSave = false; scope.launch { save(model, book, draft); onDismiss() } }) { Text(s(R.string.save)) } },
        dismissButton = { TextButton(onClick = { confirmSave = false }) { Text(s(R.string.keep_editing)) } },
    )
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text(s(R.string.discard_your_changes)) },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onDismiss() }) { Text(s(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(s(R.string.keep_editing)) } },
    )
}

@Composable
private fun CoverBox(model: ManhalViewModel, book: Book?, draft: Draft, modifier: Modifier) {
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

private suspend fun save(model: ManhalViewModel, book: Book?, d: Draft) {
    val pages = d.pages.toIntOrNull() ?: 0
    val id = book?.id ?: java.util.UUID.randomUUID().toString()
    var coverSet = false
    d.newCover?.let { coverSet = model.covers.setCustom(id, it) }
    if (d.dropCover && d.newCover == null) model.covers.removeCustom(id)
    val keepCustom = coverSet || (book?.customCover == true && !d.dropCover)
    if (book == null) {
        model.repo.add(
            Book(
                id = id, uri = "", name = d.title.trim(), kind = dev.manhal.reader.data.Kind.Pdf, physical = true, pages = pages,
                added = System.currentTimeMillis(), status = Status.ToRead, author = d.author.trim(), publisher = d.publisher.trim(),
                year = d.year, isbn = d.isbn.trim(), description = d.description.trim(), customCover = keepCustom, coverRev = 1,
            ),
        )
    } else {
        model.repo.updateBook(id) {
            it.copy(
                name = d.title.trim(), author = d.author.trim(), publisher = d.publisher.trim(), year = d.year, isbn = d.isbn.trim(),
                description = d.description.trim(), pages = if (it.physical) pages else it.pages, customCover = keepCustom,
                page = d.readingPage.toIntOrNull()?.let { n -> if (it.physical) n else (n - 1).coerceAtLeast(0) }?.coerceIn(0, if (it.pages > 0) (if (it.physical) it.pages else it.pages - 1) else Int.MAX_VALUE) ?: 0,
                offset = if (d.readingPage != (if (it.physical) it.page else it.page + 1).takeIf { n -> n > 0 }?.toString().orEmpty()) 0f else it.offset,
                status = d.status,
                readSeconds = if (d.minutes != (it.readSeconds / 60).toString().takeIf { _ -> it.readSeconds > 0 }.orEmpty()) (d.minutes.toLongOrNull() ?: 0L) * 60L else it.readSeconds,
                coverRev = if (coverSet || d.dropCover) it.coverRev + 1 else it.coverRev,
            )
        }
    }
}

/** Search, pick a result, then choose which of its parts to take; "Use selected" only fills the editor. */
@Composable
private fun OnlineFinder(draft: Draft, googleKey: String, startQuery: String, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(startQuery) }
    var results by remember { mutableStateOf<List<BookMeta>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<BookMeta?>(null) }
    val scope = rememberCoroutineScope()
    fun run() {
        busy = true; error = null
        scope.launch {
            results = runCatching { BookLookup.search(query, googleKey) }.onFailure { error = s(R.string.no_connection_or_the_catalogue) }.getOrNull()
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
        modifier = Modifier.fillMaxWidth(0.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(s(R.string.find_details_online)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(s(R.string.title_author_isbn_or_any)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { if (query.isNotBlank() && !busy) run() }))
                Button(onClick = ::run, enabled = query.isNotBlank() && !busy) { Text(s(R.string.search)) }
                Text(s(R.string.searches_open_library_and_google), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                results?.let { list ->
                    if (list.isEmpty() && !busy) {
                        Text(s(R.string.nothing_found_translated_books_are))
                        TextButton(onClick = { draft.title = query.trim(); onDismiss() }) { Text(s(R.string.use_as_the_title, query.trim())) }
                    }
                    Column(Modifier.height(340.dp).verticalScroll(rememberScrollState())) {
                        list.forEach { m ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                scope.launch { busy = true; preview = BookLookup.describe(m); busy = false }
                            }.padding(vertical = 8.dp, horizontal = 4.dp)) {
                                Text(m.title, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(listOf(m.author, m.year, m.publisher, m.source).filter { it.isNotBlank() }.joinToString("  ·  "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

/** One result against what the editor holds now, each part with its own tick. */
@Composable
private fun MetaPreview(draft: Draft, meta: BookMeta, onBack: () -> Unit, onDone: () -> Unit) {
    class Row(val label: String, val now: String, val next: String, val apply: () -> Unit)
    val rows = listOf(
        Row(s(R.string.title), draft.title, meta.title) { draft.title = meta.title },
        Row(s(R.string.author), draft.author, meta.author) { draft.author = meta.author },
        Row(s(R.string.publisher), draft.publisher, meta.publisher) { draft.publisher = meta.publisher },
        Row(s(R.string.year), draft.year, meta.year) { draft.year = meta.year },
        Row(s(R.string.pages), draft.pages, meta.pages.takeIf { it > 0 }?.toString().orEmpty()) { draft.pages = meta.pages.toString() },
        Row("ISBN", draft.isbn, meta.isbn) { draft.isbn = meta.isbn },
        Row(s(R.string.description), draft.description, meta.description) { draft.description = meta.description },
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
        modifier = Modifier.fillMaxWidth(0.96f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(s(R.string.choose_what_to_use)) },
        text = {
            Column(Modifier.height(380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (meta.coverUrl.isNotEmpty()) Row(Modifier.fillMaxWidth().clickable(enabled = coverBytes != null) { takeCover = !takeCover }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(takeCover && coverBytes != null, { takeCover = it }, enabled = coverBytes != null)
                    val bmp = remember(coverBytes) { coverBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
                    Box(Modifier.width(60.dp).height(86.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else if (coverLoading) CircularProgressIndicator(Modifier.width(22.dp).height(22.dp)) else Text(s(R.string.no_cover))
                    }
                    Text(s(R.string.cover), style = MaterialTheme.typography.labelLarge)
                }
                if (rows.isEmpty() && meta.coverUrl.isEmpty()) Text(s(R.string.this_result_has_nothing_to))
                rows.forEach { r ->
                    Row(Modifier.fillMaxWidth().clickable { ticked[r.label] = ticked[r.label] != true }, verticalAlignment = Alignment.Top) {
                        Checkbox(ticked[r.label] == true, { ticked[r.label] = it })
                        Column(Modifier.padding(top = 10.dp)) {
                            Text(r.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(r.next, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            if (r.now.isNotBlank() && r.now != r.next) Text(s(R.string.now, r.now), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            }) { Text(s(R.string.use_selected)) }
        },
        dismissButton = { TextButton(onClick = onBack) { Text(s(R.string.back)) } },
    )
}

/** Logs a sitting with a paper book: where you are now, or how many pages you read, and optionally how long it took. */
@Composable
fun LogSitting(model: ManhalViewModel, book: Book, onDismiss: () -> Unit) {
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
                    listOf(s(R.string.i_am_on_page), s(R.string.i_read_pages)).forEachIndexed { i, label ->
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
                    label = { Text(if (byCount) s(R.string.pages_read_this_sitting) else s(R.string.page_you_are_on_now)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    minutes, { minutes = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(s(R.string.minutes_optional)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    buildString {
                        append(if (gained > 0) "+" + sp(R.plurals.pages_n, gained) else s(R.string.no_new_pages))
                        if (target != null) append(s(R.string.now_on_page, target))
                        if (book.pages > 0) append(s(R.string.of_n, book.pages))
                        if (percent != null) append("  ·  $percent%")
                    },
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                )
                if (book.pages <= 0) Text(s(R.string.add_the_page_count_in), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = target != null, onClick = { model.repo.logSitting(book.id, target ?: book.page, minutes.toIntOrNull() ?: 0); onDismiss() }) { Text(s(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}
