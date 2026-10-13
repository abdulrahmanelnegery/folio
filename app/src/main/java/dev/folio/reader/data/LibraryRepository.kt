package dev.folio.reader.data

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
class LibraryRepository(private val storage: Storage, scope: CoroutineScope) {
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LibraryState> = _state

    init {
        scope.launch(Dispatchers.IO) {
            _state.drop(1).debounce(400).collect { save(it) }
        }
    }

    private fun load(): LibraryState = runCatching { decode(storage.file(Storage.LIBRARY).readText()) }.getOrDefault(LibraryState())

    /** Reads the library again from the current folder (it moved). */
    fun reload() { _state.value = load() }

    private fun save(state: LibraryState) {
        runCatching {
            val file = storage.file(Storage.LIBRARY)
            val tmp = File(file.parentFile, "library.json.tmp")
            tmp.writeText(encode(state))
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
        val merged = existing?.copy(uri = book.uri, name = book.name, fileName = book.fileName, pages = book.pages, size = book.size) ?: book
        s.copy(books = s.books.filter { it.id != book.id } + merged)
    }

    fun remove(id: String) = update { s -> s.copy(books = s.books.filter { it.id != id }) }

    /** Adds a list (inside [parent] when given); false for an empty, slashed or duplicate name. */
    fun addList(name: String, parent: String = ""): Boolean {
        val clean = name.trim()
        if (clean.isEmpty() || '/' in clean) return false
        val path = if (parent.isEmpty()) clean else "$parent/$clean"
        if (_state.value.lists.any { it.equals(path, ignoreCase = true) }) return false
        update { it.copy(lists = it.lists + path) }
        return true
    }

    /** Renames the last part of [path]; its sub-lists and the books in them follow. */
    fun renameList(path: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isEmpty() || '/' in clean) return false
        val parent = parentOf(path)
        val target = if (parent.isEmpty()) clean else "$parent/$clean"
        if (target == path) return true
        if (_state.value.lists.any { it.equals(target, ignoreCase = true) }) return false
        fun swap(p: String) = if (isInside(p, path)) target + p.removePrefix(path) else p
        update { s -> s.copy(lists = s.lists.map(::swap), books = s.books.map { b -> b.copy(lists = b.lists.map(::swap)) }) }
        return true
    }

    /** Moves a list (with the lists and books inside it) under [newParent]; false when that is not allowed. */
    fun moveListTo(path: String, newParent: String): Boolean {
        if (_state.value.withListMoved(path, newParent) == null) return false
        update { it.withListMoved(path, newParent) ?: it }
        return true
    }

    /** Moves [path] one place earlier (or later) among its siblings. */
    fun moveList(path: String, earlier: Boolean) = update { s ->
        val siblings = s.lists.filter { parentOf(it) == parentOf(path) }
        val at = siblings.indexOf(path)
        val other = siblings.getOrNull(if (earlier) at - 1 else at + 1) ?: return@update s
        val lists = s.lists.toMutableList()
        val i = lists.indexOf(path)
        val j = lists.indexOf(other)
        lists[i] = other
        lists[j] = path
        s.copy(lists = lists)
    }

    /** Deletes the list and everything inside it; the books stay in the library. */
    fun deleteList(path: String) = update { s ->
        s.copy(
            lists = s.lists.filterNot { isInside(it, path) },
            books = s.books.map { b -> b.copy(lists = b.lists.filterNot { isInside(it, path) }) },
        )
    }

    fun toggleBookmark(id: String, page: Int) = updateBook(id) {
        it.copy(bookmarks = if (page in it.bookmarks) it.bookmarks - page else (it.bookmarks + page).sorted())
    }

    /** Puts every book in [ids] in [list] (when [add]) or takes them all out. */
    fun setInList(ids: Set<String>, list: String, add: Boolean) = update { s ->
        s.copy(books = s.books.map { b ->
            if (b.id !in ids) b else b.copy(lists = if (add) (if (list in b.lists) b.lists else b.lists + list) else b.lists - list)
        })
    }

    fun updateBooks(ids: Set<String>, change: (Book) -> Book) = update { s -> s.copy(books = s.books.map { if (it.id in ids) change(it) else it }) }

    fun removeBooks(ids: Set<String>) = update { s -> s.copy(books = s.books.filter { it.id !in ids }) }

    /** Adds reading time and pages to the book and to today. */
    fun addReading(id: String, seconds: Long, pages: Int, now: Long = System.currentTimeMillis()) {
        if (seconds <= 0 && pages <= 0) return
        update { s ->
            val key = dayKey(now)
            val day = s.days[key] ?: DayStat()
            s.copy(
                days = s.days + (key to DayStat(day.seconds + seconds, day.pages + pages)),
                books = s.books.map { if (it.id == id) it.copy(readSeconds = it.readSeconds + seconds, pagesRead = it.pagesRead + pages) else it },
            )
        }
    }

    fun toggleList(id: String, list: String) = updateBook(id) {
        it.copy(lists = if (list in it.lists) it.lists - list else it.lists + list)
    }

    fun setRating(id: String, stars: Int) = updateBook(id) { it.copy(rating = if (it.rating == stars) 0 else stars.coerceIn(0, 5)) }

    fun setNote(id: String, note: String) = updateBook(id) { it.copy(note = note) }

    /** Adds a comment on [page], or replaces the text of [noteId] when given. */
    fun saveComment(id: String, page: Int, text: String, noteId: Long? = null) = updateBook(id) { b ->
        val clean = text.trim()
        when {
            noteId != null && clean.isEmpty() -> b.copy(comments = b.comments.filter { it.id != noteId })
            noteId != null -> b.copy(comments = b.comments.map { if (it.id == noteId) it.copy(text = clean) else it })
            clean.isEmpty() -> b
            else -> b.copy(comments = (b.comments + PageNote(System.nanoTime(), page, clean)).sortedBy { it.page })
        }
    }

    fun deleteComment(id: String, noteId: Long) = updateBook(id) { b -> b.copy(comments = b.comments.filter { it.id != noteId }) }

    fun addSession(session: ReadSession) = update { s -> s.copy(sessions = (s.sessions + session).takeLast(1500)) }

    fun setHomeTabs(list: List<HomeTabConfig>) = update { it.copy(homeTabs = list) }

    /** Replaces the whole library, for a restored backup. */
    fun replace(state: LibraryState) = update { state }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
        fun encode(state: LibraryState): String = json.encodeToString(LibraryState.serializer(), state)
        fun decode(text: String): LibraryState = json.decodeFromString(LibraryState.serializer(), text)
    }
}
