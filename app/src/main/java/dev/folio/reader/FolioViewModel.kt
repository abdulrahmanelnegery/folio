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
import dev.folio.reader.data.Status
import dev.folio.reader.data.Storage
import dev.folio.reader.pdf.Covers
import dev.folio.reader.pdf.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val storage = Storage(app).also { it.refresh() }
    val repo = LibraryRepository(storage, viewModelScope)
    val covers = Covers(app, storage)
    val backups = Backups(storage)

    /** Bumped when a backup is made, so the backup list redraws. */
    private val _backupTick = MutableStateFlow(0)
    val backupTick: StateFlow<Int> = _backupTick

    init {
        viewModelScope.launch(Dispatchers.IO) { if (backups.auto(repo.state.value)) _backupTick.value++ }
    }

    private val _reading = MutableStateFlow<String?>(null)
    /** The id of the book on screen, or null for the library. */
    val reading: StateFlow<String?> = _reading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() { _message.value = null }

    /** The "All files access" setting may have changed; move to the .folio folder when it has. */
    fun refreshStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            if (storage.refresh()) {
                repo.reload()
                _backupTick.value++
                _message.value = "Folio now keeps its data in ${storage.root.path}"
            }
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
    fun importUris(uris: List<Uri>, flags: Int = 0, open: Boolean = true) {
        viewModelScope.launch {
            var first: String? = null
            for (uri in uris) {
                val book = withContext(Dispatchers.IO) { import(uri) }
                if (book == null) continue
                repo.add(book)
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
        val mime = runCatching { resolver.getType(uri) }.getOrNull()
        val kind = FileTypes.kindOf(name, mime) ?: sniffKind(uri) ?: run {
            _message.value = "Folio cannot open “$name”. It reads PDF, EPUB, comic and text files."
            return null
        }
        if (kind == Kind.Epub || kind == Kind.Comic) {
            _message.value = "${if (kind == Kind.Epub) "EPUB" else "Comic"} files are coming in the next update."
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
        val base = Book(id = hash(uri.toString()), uri = stored.toString(), name = clean, fileName = name, kind = kind, size = size, added = System.currentTimeMillis())
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
            else -> null
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
        repo.updateBook(id) { it.copy(lastOpened = System.currentTimeMillis(), status = if (it.status == Status.Finished) it.status else Status.Reading) }
        _reading.value = id
    }

    fun closeBook() {
        _reading.value = null
        // Saving and the backup happen off the main thread so the screen change stays smooth.
        viewModelScope.launch(Dispatchers.IO) {
            repo.flush()
            if (backups.auto(repo.state.value)) _backupTick.value++
        }
    }

    fun remove(id: String) {
        covers.forget(id)
        discardText(id)
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
                _message.value = "Folio could not write to this file. Use Save as to keep your changes."
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
            _message.value = "That is not a Folio backup."
        } else {
            repo.replace(state)
            repo.flush()
            _message.value = "Library restored: ${state.books.size} books."
        }
    }

    override fun onCleared() = repo.flush()

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)

    private companion object {
        const val MAX_TEXT = 5L * 1024 * 1024
    }
}
