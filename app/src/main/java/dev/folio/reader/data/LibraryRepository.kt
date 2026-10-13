package dev.folio.reader.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

/** The library on disk: one JSON file, saved shortly after every change. */
class LibraryRepository(context: Context, scope: CoroutineScope) {
    private val file = File(context.filesDir, "library.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LibraryState> = _state

    init {
        scope.launch(Dispatchers.IO) {
            _state.drop(1).debounce(400).collect { save(it) }
        }
    }

    private fun load(): LibraryState = runCatching { json.decodeFromString<LibraryState>(file.readText()) }.getOrDefault(LibraryState())

    private fun save(state: LibraryState) {
        runCatching {
            val tmp = File(file.parentFile, "library.json.tmp")
            tmp.writeText(json.encodeToString(LibraryState.serializer(), state))
            tmp.renameTo(file)
        }
    }

    /** Writes the current state now (the app is going away). */
    fun flush() = save(_state.value)

    fun update(change: (LibraryState) -> LibraryState) = _state.update(change)

    fun book(id: String): Book? = _state.value.books.firstOrNull { it.id == id }

    fun updateBook(id: String, change: (Book) -> Book) = update { s ->
        s.copy(books = s.books.map { if (it.id == id) change(it) else it })
    }

    fun add(book: Book) = update { s ->
        val existing = s.books.firstOrNull { it.id == book.id }
        val merged = existing?.copy(uri = book.uri, name = book.name, pages = book.pages, size = book.size) ?: book
        s.copy(books = s.books.filter { it.id != book.id } + merged)
    }

    fun remove(id: String) = update { s -> s.copy(books = s.books.filter { it.id != id }) }

    fun addList(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || _state.value.lists.any { it.equals(clean, ignoreCase = true) }) return false
        update { it.copy(lists = it.lists + clean) }
        return true
    }

    fun deleteList(name: String) = update { s ->
        s.copy(lists = s.lists - name, books = s.books.map { it.copy(lists = it.lists - name) })
    }

    fun toggleList(id: String, list: String) = updateBook(id) {
        it.copy(lists = if (list in it.lists) it.lists - list else it.lists + list)
    }
}

