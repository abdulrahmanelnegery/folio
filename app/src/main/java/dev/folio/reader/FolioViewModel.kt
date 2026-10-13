package dev.folio.reader

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.folio.reader.data.Book
import dev.folio.reader.data.Backups
import dev.folio.reader.data.LibraryRepository
import dev.folio.reader.data.LibraryState
import dev.folio.reader.data.Storage
import dev.folio.reader.data.Status
import dev.folio.reader.pdf.Covers
import dev.folio.reader.pdf.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

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

    private val _reading = MutableStateFlow<String?>(null)
    /** The id of the book on screen, or null for the library. */
    val reading: StateFlow<String?> = _reading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() { _message.value = null }

    /** Adds the documents the picker or another app handed over, then opens the first one. */
    fun importUris(uris: List<Uri>, flags: Int = 0, open: Boolean = true) {
        viewModelScope.launch {
            var first: String? = null
            for (uri in uris) {
                val book = withContext(Dispatchers.IO) { import(uri, flags) }
                if (book == null) {
                    _message.value = "That file could not be opened as a PDF."
                    continue
                }
                repo.add(book)
                if (first == null) first = book.id
            }
            if (open && first != null) openBook(first)
        }
    }

    private fun import(uri: Uri, flags: Int): Book? {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        // Keep access across restarts when the provider allows it; otherwise keep our own copy.
        var stored = uri
        val persisted = runCatching {
            resolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
        if (!persisted && uri.scheme != "file") {
            val copy = File(context.filesDir, "books").apply { mkdirs() }.resolve("${hash(uri.toString())}.pdf")
            runCatching { resolver.openInputStream(uri)?.use { input -> copy.outputStream().use { input.copyTo(it) } } }
            if (copy.length() == 0L) return null
            stored = Uri.fromFile(copy)
        }
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
        val pdf = PdfFile.open(context, stored) ?: return null
        val pages = pdf.pageCount
        pdf.close()
        return Book(id = hash(uri.toString()), uri = stored.toString(), name = name.removeSuffix(".pdf").removeSuffix(".PDF"), fileName = name, pages = pages, size = size, added = System.currentTimeMillis())
    }

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
        repo.remove(id)
    }

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

    private fun applyRestore(state: dev.folio.reader.data.LibraryState?) {
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
}
