package dev.folio.reader

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.folio.reader.data.Backups
import dev.folio.reader.data.Book
import dev.folio.reader.data.FileTypes
import dev.folio.reader.data.Kind
import dev.folio.reader.data.LibraryRepository
import dev.folio.reader.data.LibraryState
import dev.folio.reader.data.deduplicated
import dev.folio.reader.data.findSameFile
import dev.folio.reader.data.ReadSession
import dev.folio.reader.data.Status
import dev.folio.reader.data.Storage
import dev.folio.reader.pdf.Covers
import dev.folio.reader.pdf.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** A text file being read or edited. Lives here so edits survive leaving the screen and switching apps. */
class TextSession(val original: String) {
    var text by mutableStateOf(original)
    /** The text as last saved. */
    var saved by mutableStateOf(original)
    var editing by mutableStateOf(false)
    val dirty: Boolean get() = text != saved
}

class FolioViewModel(app: Application) : AndroidViewModel(app) {
    val storage = Storage(app)
    /** The app's own theme (System, Light or Dark), read before the first frame. */
    val appTheme = MutableStateFlow(dev.folio.reader.data.AppThemePrefs.load(app))
    fun setAppTheme(theme: dev.folio.reader.data.AppTheme) { appTheme.value = theme; dev.folio.reader.data.AppThemePrefs.save(getApplication(), theme) }
    val repo = LibraryRepository(storage, viewModelScope)
    val covers = Covers(app, storage)
    /** Set from Settings to show the first-run tour again. */
    val tourRequested = kotlinx.coroutines.flow.MutableStateFlow(false)
    val pageCache = dev.folio.reader.pdf.PageDiskCache(storage)
    val backups = Backups(storage)

    /** Bumped when a backup is made, so the backup list redraws. */
    private val _backupTick = MutableStateFlow(0)
    val backupTick: StateFlow<Int> = _backupTick

    init {
        // The same file added by two routes becomes one entry.
        repo.update { it.deduplicated() }
        viewModelScope.launch(Dispatchers.IO) { if (backups.auto(repo.state.value)) _backupTick.value++ }
    }

    // ---- PDF edit drafts -------------------------------------------------------------------

    private val editSessions = HashMap<String, dev.folio.reader.edits.EditSession>()

    /** The draft of [id]'s PDF edits; the same object while the app lives, so it survives leaving the reader. */
    @Synchronized
    fun editSession(id: String): dev.folio.reader.edits.EditSession = editSessions.getOrPut(id) {
        dev.folio.reader.edits.EditSession(File(storage.dir("edits"), "$id.json"), viewModelScope) { n -> repo.updateBook(id) { it.copy(draftMarks = n) } }
    }

    /** Bumped after a PDF file is replaced, so an open reader loads the new file. */
    private val _pdfReload = MutableStateFlow(0)
    val pdfReload: StateFlow<Int> = _pdfReload

    enum class SaveMode { AsNew, Overwrite }

    /** The book's file as a plain path when it is one (not a provider's content link). */
    private fun realFile(book: Book): File? =
        Uri.parse(book.uri).takeIf { it.scheme == "file" }?.path?.let(::File)?.takeIf { it.isFile }

    /** Whether the PDF itself may be replaced: only a real file in a folder Rukn can write to. */
    fun canOverwrite(id: String): Boolean {
        val f = repo.book(id)?.let(::realFile) ?: return false
        return f.canWrite() && f.parentFile?.canWrite() == true
    }

    /** Where the original is kept when it is replaced. */
    val originalsFolder: File get() = storage.dir("originals")

    /**
     * Writes the draft into a copy ([SaveMode.AsNew]) or over the file ([SaveMode.Overwrite], after keeping the
     * original). [done] gets whether it worked; either way a message is shown. The draft is cleared only on success.
     */
    fun saveEdits(id: String, mode: SaveMode, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val session = editSession(id)
            val result = withContext(Dispatchers.IO) { runSave(id, mode, session.draft()) }
            if (result.first != null) {
                session.discard()
                when (mode) {
                    SaveMode.AsNew -> importUris(listOf(Uri.fromFile(result.first)), open = false)
                    SaveMode.Overwrite -> {
                        pageCache.forget(id); covers.forget(id)
                        _pdfReload.value++
                    }
                }
            }
            _message.value = result.second
            done(result.first != null)
        }
    }

    private fun runSave(id: String, mode: SaveMode, draft: dev.folio.reader.edits.Draft): Pair<File?, String> {
        val book = repo.book(id) ?: return null to "That book is gone."
        if (draft.isEmpty) return null to "There is nothing to save."
        val app = getApplication<Application>()
        val real = realFile(book)
        // A file only reachable through a provider is copied aside first; it can be saved as new but never replaced.
        val temp = if (real == null) File(app.cacheDir, "edit-${book.id}.pdf") else null
        try {
            val source = real ?: temp!!.also { t ->
                val ok = runCatching { app.contentResolver.openInputStream(Uri.parse(book.uri))?.use { i -> t.outputStream().use { i.copyTo(it) } } }.isSuccess
                if (!ok || t.length() == 0L) return null to "The PDF could not be read."
            }
            val work = dev.folio.reader.pdf.PdfEditWriter(app, draft)
            val fileName = book.fileName.ifEmpty { "${book.name}.pdf" }
            return when (mode) {
                SaveMode.AsNew -> {
                    val dir = real?.parentFile?.takeIf { it.canWrite() } ?: storage.dir("edited")
                    val target = File(dir, dev.folio.reader.edits.SafeWrite.editedName(File(dir, fileName)))
                    when (val r = dev.folio.reader.edits.SafeWrite.saveAsNew(source, target, work)) {
                        is dev.folio.reader.edits.SaveResult.Saved -> target to "Saved as “${target.name}”. The original is untouched."
                        is dev.folio.reader.edits.SaveResult.Failed -> null to r.reason
                    }
                }
                SaveMode.Overwrite -> {
                    if (real == null || !canOverwrite(id)) return null to "This file cannot be replaced. Save it as a new file instead."
                    val backup = File(originalsFolder, "${book.id}-${System.currentTimeMillis()}.pdf")
                    when (val r = dev.folio.reader.edits.SafeWrite.overwrite(real, backup, work)) {
                        is dev.folio.reader.edits.SaveResult.Saved -> real to "Saved over the original. A copy of the original is in ${backup.parentFile?.name}/${backup.name}."
                        is dev.folio.reader.edits.SaveResult.Failed -> null to r.reason
                    }
                }
            }
        } finally {
            temp?.delete()
        }
    }

    // ---- reading time ----------------------------------------------------------------------

    private var lastTouch = android.os.SystemClock.elapsedRealtime()
    private var pendingSeconds = 0.0
    private var pendingPages = 0
    private var pendingBook: String? = null
    private val pagesSeen = HashSet<String>()

    // The sitting in progress: which book, since when, and what it has added up to.
    private var sessionBook: String? = null
    private var sessionStart = 0L
    private var sessionSeconds = 0.0
    private var sessionPages = 0

    /** Any touch counts as "still reading"; the clock stops after [IDLE_MS] without one. */
    @Synchronized
    fun touched() { lastTouch = android.os.SystemClock.elapsedRealtime() }

    /** Called about once a second while a book is open and the app is in front; [elapsedMs] is the real time since the last call. */
    @Synchronized
    fun readingTick(id: String, elapsedMs: Long) {
        if (android.os.SystemClock.elapsedRealtime() - lastTouch > IDLE_MS) return
        if (pendingBook != null && pendingBook != id) commitReading()
        pendingBook = id
        pendingSeconds += elapsedMs.coerceIn(0, 2000) / 1000.0
        if (sessionBook == id) sessionSeconds += elapsedMs.coerceIn(0, 2000) / 1000.0
        if (pendingSeconds >= 20) commitReading()
    }

    /** A page the reader stayed on; each page counts once per opening of the book. */
    @Synchronized
    fun pageSeen(id: String, page: Int) {
        if (!pagesSeen.add("$id:$page")) return
        if (pendingBook != null && pendingBook != id) commitReading()
        pendingBook = id
        pendingPages++
        if (sessionBook == id) sessionPages++
    }

    /** Writes the time and pages gathered so far into the library. */
    @Synchronized
    fun commitReading() {
        val id = pendingBook ?: return
        val seconds = pendingSeconds.toLong()
        val pages = pendingPages
        pendingSeconds -= seconds
        pendingPages = 0
        repo.addReading(id, seconds, pages)
    }

    // Where you were in the library, kept here so coming back from a book (or the explorer) lands on the same shelf,
    // list and search instead of starting over at Home.
    var libraryTab by mutableStateOf<dev.folio.reader.data.Tab>(dev.folio.reader.data.Tab.Home)
    var libraryQuery by mutableStateOf("")
    var librarySearching by mutableStateOf(false)

    private val _reading = MutableStateFlow<String?>(null)
    /** The id of the book on screen, or null for the library. */
    val reading: StateFlow<String?> = _reading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() { _message.value = null }

    private val _migrating = MutableStateFlow(false)
    /** True while the library is being copied into the Zimam folder in the background. */
    val migrating: StateFlow<Boolean> = _migrating

    /** Shows a short message at the bottom of the screen. */
    fun notify(text: String) { _message.value = text }

    /** The "All files access" setting may have changed; move to the Rukn folder when it has. */
    fun refreshStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            // The banner only shows if the move takes a moment; a quick check never flashes it.
            val banner = launch { delay(400); _migrating.value = true }
            val changed = try { storage.refresh() } finally { banner.cancel(); _migrating.value = false }
            if (changed) {
                repo.reload()
                _backupTick.value++
                _message.value = "Rukn now keeps its data in ${storage.root.path}"
            }
            storage.migrationMessage?.let { _message.value = it }
        }
    }

    fun backupNow() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = backups.now(repo.state.value) != null
            _backupTick.value++
            _message.value = if (ok) "Backup made." else "The backup could not be made."
        }
    }

    /** Adds the documents the picker or another app handed over, then opens the first one. */
    fun importUris(uris: List<Uri>, flags: Int = 0, open: Boolean = true, addToList: String? = null) {
        viewModelScope.launch {
            var first: String? = null
            for (uri in uris) {
                val book = withContext(Dispatchers.IO) { import(uri) }
                if (book == null) continue
                repo.add(book)
                if (addToList != null && addToList !in (repo.book(book.id)?.lists ?: emptyList())) repo.toggleList(book.id, addToList)
                if (first == null) first = book.id
            }
            if (open && first != null) openBook(first)
        }
    }

    private fun import(uri: Uri): Book? {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        var name = "Document"
        var size = 0L
        runCatching {
            resolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) ?: name }
                    c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
                }
            }
        }
        if (name == "Document") uri.lastPathSegment?.substringAfterLast('/')?.let { name = it }
        if (size == 0L && uri.scheme == "file") size = runCatching { File(uri.path.orEmpty()).length() }.getOrDefault(0L)
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
        val kind = FileTypes.kindOf(name, mime) ?: sniffKind(uri) ?: run {
            _message.value = "Rukn cannot open “$name”. It reads PDF, EPUB, comic and text files."
            return null
        }
        if (kind == Kind.Comic && !name.lowercase().endsWith(".cbz") && !name.lowercase().endsWith(".zip")) {
            _message.value = "Rukn reads comics in .cbz (a zip of page images). “$name” is another format; convert it to .cbz first."
            return null
        }

        // Keep access across restarts (with write access for text files) when the provider allows it;
        // otherwise keep our own copy, which is then the file that gets edited.
        var stored = uri
        val persisted = runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }.isSuccess ||
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess
        if (!persisted && uri.scheme != "file") {
            val copy = File(context.filesDir, "books").apply { mkdirs() }.resolve("${hash(uri.toString())}.${FileTypes.extension(name).ifEmpty { "bin" }}")
            runCatching { resolver.openInputStream(uri)?.use { input -> copy.outputStream().use { input.copyTo(it) } } }
            if (copy.length() == 0L) return null
            stored = Uri.fromFile(copy)
        }
        val clean = name.substringBeforeLast('.', name).ifEmpty { name }
        // The same file reached another way (picker, explorer, a shared link) keeps its one entry.
        val sameId = repo.state.value.findSameFile(name, size)?.id
        val base = Book(id = sameId ?: hash(uri.toString()), uri = stored.toString(), name = clean, fileName = name, kind = kind, size = size, added = System.currentTimeMillis())
        return when (kind) {
            Kind.Pdf -> {
                val pdf = PdfFile.open(context, stored) ?: run { _message.value = "That file could not be opened as a PDF."; return null }
                val pages = pdf.pageCount
                pdf.close()
                base.copy(pages = pages)
            }
            Kind.Text -> {
                if (size > MAX_TEXT) { _message.value = "That file is too large to open as text (limit ${MAX_TEXT / 1048576} MB)."; return null }
                val text = readText(stored) ?: run { _message.value = "That file could not be read."; return null }
                base.copy(name = name, pages = text.count { it == '\n' } + 1, size = if (size > 0) size else text.length.toLong())
            }
            Kind.Epub -> {
                val (pkg, _) = dev.folio.reader.epub.EpubStore.open(context, stored, base.id) ?: run { _message.value = "That file could not be opened as an EPUB."; return null }
                base.copy(name = pkg.title.ifBlank { clean }, author = pkg.author, pages = pkg.spine.size)
            }
            Kind.Comic -> {
                val pages = dev.folio.reader.epub.ComicStore.open(context, stored, base.id)
                if (pages.isEmpty()) { _message.value = "That file has no page images Rukn can read."; return null }
                base.copy(pages = pages.size)
            }
        }
    }

    /** Files whose name says nothing: text when the first bytes look like it. */
    private fun sniffKind(uri: Uri): Kind? = runCatching {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            val buf = ByteArray(4096)
            val n = input.read(buf).coerceAtLeast(0)
            if (n >= 4 && String(buf, 0, 4, Charsets.ISO_8859_1) == "%PDF") Kind.Pdf
            else if (FileTypes.looksLikeText(buf, n)) Kind.Text else null
        }
    }.getOrNull()

    private fun readText(uri: Uri): String? = runCatching {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
    }.getOrNull()

    fun openBook(id: String) {
        if (repo.book(id)?.physical == true) return
        _reading.value = id
        sessionBook = id; sessionStart = System.currentTimeMillis(); sessionSeconds = 0.0; sessionPages = 0
        repo.updateBook(id) { it.copy(lastOpened = System.currentTimeMillis(), status = if (it.status == Status.Finished) it.status else Status.Reading) }
    }

    fun closeBook() {
        val closing = _reading.value
        endSession()
        // The screen changes first; saving and the backup follow off the main thread.
        _reading.value = null
        // The book you just read goes to the front of the Recent order.
        closing?.let { id -> repo.updateBook(id) { it.copy(lastOpened = System.currentTimeMillis()) } }
        viewModelScope.launch(Dispatchers.IO) {
            commitReading()
            pagesSeen.removeAll { it.startsWith("$closing:") }
            repo.flush()
            if (backups.auto(repo.state.value)) _backupTick.value++
        }
    }

    /** Records the sitting that is ending, if it was long enough to count. */
    @Synchronized
    private fun endSession() {
        val id = sessionBook ?: return
        sessionBook = null
        if (sessionSeconds >= 5) repo.addSession(ReadSession(id, sessionStart, sessionSeconds.toLong(), sessionPages))
    }

    fun remove(id: String) {
        covers.forget(id)
        pageCache.forget(id)
        dev.folio.reader.epub.EpubStore.delete(getApplication(), id)
        dev.folio.reader.epub.ComicStore.delete(getApplication(), id)
        discardText(id)
        editSession(id).discard()
        synchronized(this) { editSessions.remove(id) }
        repo.remove(id)
    }

    // ---- text files ------------------------------------------------------------------------

    val texts = mutableStateMapOf<String, TextSession>()

    private fun draftFile(id: String) = File(storage.dir("drafts"), "$id.draft")

    /** Loads the file (and any draft left by an earlier session) into [texts]. */
    fun loadText(book: Book) {
        if (texts.containsKey(book.id)) return
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) {
                val text = readText(Uri.parse(book.uri)) ?: return@withContext null
                TextSession(text).also { s ->
                    val draft = draftFile(book.id)
                    if (draft.exists()) runCatching { s.text = draft.readText() }
                }
            }
            if (session == null) _message.value = "That file could not be read." else texts[book.id] = session
        }
    }

    /** Writes the edited text back to the file it came from. */
    fun saveText(id: String, after: (Boolean) -> Unit = {}) {
        val book = repo.book(id) ?: return
        val session = texts[id] ?: return
        val text = session.text
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(Uri.parse(book.uri), "wt")?.use { it.write(text.toByteArray()) } != null
                }.getOrDefault(false)
            }
            if (ok) {
                session.saved = text
                draftFile(id).delete()
                repo.updateBook(id) { it.copy(pages = text.count { c -> c == '\n' } + 1, size = text.toByteArray().size.toLong()) }
                _message.value = "Saved."
            } else {
                _message.value = "Rukn could not write to this file. Use Save as to keep your changes."
            }
            after(ok)
        }
    }

    /** Writes the edited text to a new file the user chose. */
    fun saveTextAs(id: String, uri: Uri, after: (Boolean) -> Unit = {}) {
        val session = texts[id] ?: return
        val text = session.text
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } != null }.getOrDefault(false)
            }
            _message.value = if (ok) "Saved a new file." else "The file could not be saved."
            after(ok)
        }
    }

    /** Drops the edits and the draft; the file is read again next time. */
    fun discardText(id: String) {
        texts.remove(id)
        runCatching { draftFile(id).delete() }
    }

    /** Keeps unsaved edits on disk so they survive the app being swapped out of memory. */
    fun persistDrafts() {
        val dirty = texts.filterValues { it.dirty }
        if (dirty.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            dirty.forEach { (id, s) -> runCatching { draftFile(id).writeText(s.text) } }
        }
    }

    // ---- backups ---------------------------------------------------------------------------

    /** Saves the whole library (reading places, lists, notes) to a file the user picked. */
    fun exportBackup(uri: Uri) {
        val text = LibraryRepository.encode(repo.state.value)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) } != null }.getOrDefault(false)
            }
            _message.value = if (ok) "Backup saved." else "The backup could not be saved."
        }
    }

    /** Replaces the library with a backup file the user picked. */
    fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { LibraryRepository.decode(String(it.readBytes())) }
                }.getOrNull()
            }
            applyRestore(restored)
        }
    }

    fun restoreFile(file: File) {
        viewModelScope.launch { applyRestore(withContext(Dispatchers.IO) { backups.read(file) }) }
    }

    private fun applyRestore(state: LibraryState?) {
        if (state == null) {
            _message.value = "That is not a Rukn backup."
        } else {
            repo.replace(state)
            repo.flush()
            _message.value = "Library restored: ${state.books.size} books."
        }
    }

    override fun onCleared() { storage.cancelled = true; repo.flush() }

    // Last, so every property above exists before the background check starts.
    init { refreshStorage() }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)

    private companion object {
        const val IDLE_MS = 120_000L
        const val MAX_TEXT = 5L * 1024 * 1024
    }
}
