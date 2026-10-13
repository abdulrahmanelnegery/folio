package dev.folio.reader.data

import kotlinx.serialization.Serializable

@Serializable
enum class Status { New, Reading, ToRead, Finished }

@Serializable
enum class SortOrder(val label: String) {
    Recent("Last opened"),
    Added("Date added"),
    Name("Name"),
    Size("Size"),
    Progress("Progress"),
    Rating("Rating"),
}

/** How the books are laid out on a shelf. */
@Serializable
enum class ViewMode(val label: String) {
    Grid("Grid"),
    Compact("Small covers"),
    Large("Large covers"),
    List("List"),
    Details("Details"),
}

@Serializable
enum class PageTheme(val label: String) {
    Normal("Normal"),
    Night("Night"),
    SoftNight("Soft night"),
    Sepia("Sepia");

    fun next(): PageTheme = entries[(ordinal + 1) % entries.size]
}

/** What kind of document a library entry is; decides which reader opens it. */
@Serializable
enum class Kind { Pdf, Epub, Comic, Text }

/** A book in the library and where the reader stopped in it. */
@Serializable
data class Book(
    val id: String,
    val uri: String,
    val name: String,
    val fileName: String = "",
    val kind: Kind = Kind.Pdf,
    /** Pages of a PDF, chapters of an EPUB, images of a comic, lines of a text file. */
    val pages: Int = 0,
    val size: Long = 0,
    val added: Long = 0,
    val lastOpened: Long = 0,
    val favorite: Boolean = false,
    val status: Status = Status.New,
    /** Full paths of the lists the book is in ("Work/Courses"). */
    val lists: List<String> = emptyList(),
    /** Zero-based page at the top of the screen when the book was closed. */
    val page: Int = 0,
    /** How far down that page (0..1). */
    val offset: Float = 0f,
    /** Zero-based pages the reader marked. */
    val bookmarks: List<Int> = emptyList(),
    /** Your stars, 0 (none) to 5. */
    val rating: Int = 0,
    /** A note about the whole book. */
    val note: String = "",
    /** Comments pinned to pages. They live in the library, never in the PDF. */
    val comments: List<PageNote> = emptyList(),
    /** Seconds spent reading this book, and pages read in it. */
    val readSeconds: Long = 0,
    val pagesRead: Int = 0,
    /** How this book rotates while it is open; other books and the library are not affected. */
    val orientation: Orientation = Orientation.Normal,
    /** The screen brightness while reading this book (0.02..1); negative follows the system. */
    val brightness: Float = -1f,
    /** Whether the blank page margins are cropped away, and the margins found (left, top, right, bottom as fractions). */
    val cropMargins: Boolean = false,
    val margins: List<Float> = emptyList(),
    /** A paper book: no file, [page] counts the pages read so far and sittings are logged by hand. */
    val physical: Boolean = false,
    val author: String = "",
    val publisher: String = "",
    val year: String = "",
    val isbn: String = "",
    val description: String = "",
    /** Whether a picked or fetched cover replaces the one drawn from the file. */
    val customCover: Boolean = false,
    /** Bumped whenever the book's own cover changes, so a shown cover reloads. */
    val coverRev: Int = 0,
) {
    /** Reading progress 0..1; a finished book counts as complete. */
    val progress: Float
        get() = when {
            status == Status.Finished -> 1f
            kind == Kind.Text -> offset.coerceIn(0f, 1f)
            physical -> if (pages <= 0) 0f else (page.toFloat() / pages).coerceIn(0f, 1f)
            pages <= 0 || page <= 0 -> 0f
            else -> ((page + offset) / pages).coerceIn(0f, 1f)
        }

    /** Where the reader is: "Page 3 of 12", "Read 40%", or just the size of the book. */
    val placeLabel: String
        get() = when {
            physical -> if (page > 0) "Page $page of $pages" else countLabel
            kind == Kind.Text && offset > 0f && status != Status.Finished -> "Read ${(offset * 100).toInt()}%  ·  $countLabel"
            kind != Kind.Text && page > 0 && status != Status.Finished -> "${if (kind == Kind.Epub) "Chapter" else "Page"} ${page + 1} of $pages"
            else -> countLabel
        }

    /** "12 pages", "340 lines": what [pages] counts for this kind. */
    val countLabel: String
        get() {
            val unit = when (kind) {
                Kind.Pdf -> "page"
                Kind.Epub -> "chapter"
                Kind.Comic -> "page"
                Kind.Text -> "line"
            }
            return "$pages $unit${if (pages == 1) "" else "s"}"
        }
}

/** Reading done on one day. */
@Serializable
data class DayStat(val seconds: Long = 0, val pages: Int = 0)

/** One sitting with a book: when it started, how long you really read, and the pages you stayed on. */
@Serializable
data class ReadSession(val bookId: String, val start: Long, val seconds: Long, val pages: Int)

@Serializable
enum class HomeTab(val label: String, val note: String) {
    Home("Home", "Continue reading and the latest three books"),
    Stats("Stats", "Reading time, sittings and what you read most"),
    Library("Library", "Your shelves, lists and favorites"),
}

@Serializable
data class HomeTabConfig(val tab: HomeTab, val visible: Boolean = true)

/** How the screen rotates. */
@Serializable
enum class Orientation(val label: String, val note: String) {
    Normal("Normal", "Follow the phone's rotation setting"),
    Portrait("Portrait", "Always upright"),
    Landscape("Landscape", "Always sideways"),
    Dynamic("Dynamic", "Rotate with the sensor, even if the phone's rotation is locked"),
    Locked("Locked", "Stay as it is now"),
}

/** A comment on one page. */
@Serializable
data class PageNote(val id: Long, val page: Int, val text: String)

@Serializable
data class LibraryState(
    val books: List<Book> = emptyList(),
    /** Every list as a path ("Work", "Work/Courses"); the order here is the order shown among siblings. */
    val lists: List<String> = emptyList(),
    val view: ViewMode = ViewMode.Grid,
    val sort: SortOrder = SortOrder.Recent,
    val theme: PageTheme = PageTheme.Normal,
    /** Screen brightness while reading, 0.02..1; negative follows the system. */
    val brightness: Float = -1f,
    /** Ask before leaving Rukn when back is pressed on the home screen. */
    val confirmExit: Boolean = true,
    /** Optional free Google Books key; without it the shared quota is often used up for the day. */
    val googleBooksKey: String = "",
    /** The folder the file explorer was last in. */
    val explorerDir: String = "",
    /** One entry per time a book was opened and closed. */
    val sessions: List<ReadSession> = emptyList(),
    /** The tabs of the home page in the order shown; Home is always first and always there. */
    val homeTabs: List<HomeTabConfig> = emptyList(),
    val orientation: Orientation = Orientation.Normal,
    /** Reading time and pages per day, keyed "yyyy-MM-dd" in the phone's time zone. */
    val days: Map<String, DayStat> = emptyMap(),
)

/** What the library screen is showing. */
sealed interface Tab {
    data object Home : Tab
    data object ReadingNow : Tab
    data object ToRead : Tab
    data object Finished : Tab
    data object Favorites : Tab
    data object All : Tab
    data class List(val path: String) : Tab
}

// ---- lists inside lists -------------------------------------------------------------------

fun parentOf(path: String): String = path.substringBeforeLast('/', "")
fun nameOf(path: String): String = path.substringAfterLast('/')
fun depthOf(path: String): Int = path.count { it == '/' }
fun isInside(path: String, ancestor: String): Boolean = path == ancestor || path.startsWith("$ancestor/")

/** Lists in tree order: each list, then its children, siblings in the stored order. */
fun LibraryState.orderedLists(): List<String> {
    val out = ArrayList<String>(lists.size)
    fun walk(parent: String) {
        lists.filter { parentOf(it) == parent }.forEach { out += it; walk(it) }
    }
    walk("")
    // Anything whose parent went missing still shows, at the top level.
    lists.filter { it !in out }.forEach { out += it }
    return out
}

/** [deep]: a list also holds the books of the lists inside it. */
fun Book.inTab(tab: Tab, deep: Boolean = true): Boolean = when (tab) {
    Tab.Home -> true
    Tab.ReadingNow -> status == Status.Reading
    Tab.ToRead -> status == Status.ToRead
    Tab.Finished -> status == Status.Finished
    Tab.Favorites -> favorite
    Tab.All -> true
    is Tab.List -> if (deep) lists.any { isInside(it, tab.path) } else tab.path in lists
}

/** The books to show: the shelf, the search, in the chosen order. */
fun LibraryState.visible(tab: Tab, query: String): List<Book> {
    val q = query.trim().lowercase()
    // Browsing a list shows its own books (the lists inside are folders); searching looks through all of them.
    val shelf = books.filter { it.inTab(tab, deep = q.isNotEmpty()) && (q.isEmpty() || it.name.lowercase().contains(q) || it.fileName.lowercase().contains(q)) }
    return when (sort) {
        SortOrder.Recent -> shelf.sortedByDescending { it.lastOpened }
        SortOrder.Added -> shelf.sortedByDescending { it.added }
        SortOrder.Name -> shelf.sortedBy { it.name.lowercase() }
        SortOrder.Size -> shelf.sortedByDescending { it.size }
        SortOrder.Progress -> shelf.sortedByDescending { it.progress }
        SortOrder.Rating -> shelf.sortedByDescending { it.rating }
    }
}

/** Lists directly inside [parent] ("" for the top level), in order. */
fun LibraryState.childLists(parent: String): List<String> = lists.filter { parentOf(it) == parent }

// ---- reading stats --------------------------------------------------------------------------

fun dayKey(millis: Long): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(millis))

private const val DAY_MS = 24L * 60 * 60 * 1000

/** The stats of the day [back] days before today (0 is today). */
fun LibraryState.day(back: Int, now: Long = System.currentTimeMillis()): DayStat = days[dayKey(now - back * DAY_MS)] ?: DayStat()

/** Reading time and pages over the last [count] days, today included. */
fun LibraryState.lastDays(count: Int, now: Long = System.currentTimeMillis()): DayStat =
    (0 until count).map { day(it, now) }.let { d -> DayStat(d.sumOf { it.seconds }, d.sumOf { it.pages }) }

/** Days in a row, up to today, with at least a minute of reading; today may still be empty. */
fun LibraryState.streak(now: Long = System.currentTimeMillis()): Int {
    var n = 0
    var back = if (day(0, now).seconds >= 60) 0 else 1
    while (day(back, now).seconds >= 60) { n++; back++ }
    return n
}

/**
 * The library with [path] (and everything inside it) moved under [newParent] ("" for the top level).
 * Null when that is not allowed: into itself or a list inside it, or onto a name already taken there.
 */
fun LibraryState.withListMoved(path: String, newParent: String): LibraryState? {
    if (path !in lists) return null
    if (newParent == parentOf(path)) return this
    if (newParent.isNotEmpty() && (newParent !in lists || isInside(newParent, path))) return null
    val target = if (newParent.isEmpty()) nameOf(path) else "$newParent/${nameOf(path)}"
    if (lists.any { it.equals(target, ignoreCase = true) }) return null
    fun swap(p: String) = if (isInside(p, path)) target + p.removePrefix(path) else p
    return copy(lists = lists.map(::swap), books = books.map { b -> b.copy(lists = b.lists.map(::swap).distinct()) })
}

/** Where [path] could move to: the top level and every list that is not inside it, minus where it already is. */
fun LibraryState.moveTargets(path: String): List<String> =
    (listOf("") + orderedLists().filter { !isInside(it, path) }).filter { it != parentOf(path) }

// ---- the home page ----------------------------------------------------------------------------

/** The home tabs: Home first and always shown, the others in the user's order with any new one appended. */
fun LibraryState.homeTabs(): List<HomeTabConfig> {
    val others = homeTabs.filter { it.tab != HomeTab.Home }.distinctBy { it.tab }
    return listOf(HomeTabConfig(HomeTab.Home)) + others + HomeTab.entries.filter { t -> t != HomeTab.Home && others.none { it.tab == t } }.map { HomeTabConfig(it) }
}

fun LibraryState.sessionsSince(days: Int, now: Long = System.currentTimeMillis()): List<ReadSession> =
    sessions.filter { it.start >= now - days * DAY_MS }

/** The books read the most in the last [days] days, with the seconds, longest first. */
fun LibraryState.mostRead(days: Int, limit: Int, now: Long = System.currentTimeMillis()): List<Pair<Book, Long>> =
    sessionsSince(days, now).groupBy { it.bookId }.mapValues { (_, s) -> s.sumOf { it.seconds } }
        .mapNotNull { (id, secs) -> books.firstOrNull { it.id == id }?.let { it to secs } }
        .sortedByDescending { it.second }.take(limit)

// ---- the same file added twice -----------------------------------------------------------------

/** The key two entries share when they are the same file reached by different routes: its name and size. */
private fun Book.sameFileKey(): String? {
    // Older entries have no stored file name; their title is the name without ".pdf", which is what is compared.
    val base = fileName.ifEmpty { name }.substringBeforeLast('.', fileName.ifEmpty { name }).lowercase()
    return if (base.isNotEmpty() && size > 0) "$base|$size" else null
}

/** Finds the library entry for a file with this name and size, if there is one. */
fun LibraryState.findSameFile(fileName: String, size: Long): Book? {
    if (fileName.isEmpty() || size <= 0) return null
    val key = fileName.substringBeforeLast('.', fileName).lowercase() + "|" + size
    return books.firstOrNull { it.sameFileKey() == key }
}

/**
 * Merges entries that are the same file (opened once from the picker and once from the explorer, say):
 * progress, lists, notes, bookmarks, ratings and reading time are all kept on the one that stays.
 */
fun LibraryState.deduplicated(): LibraryState {
    val groups = books.groupBy { it.sameFileKey() }
    if (groups.all { (key, list) -> key == null || list.size == 1 }) return this
    val remap = HashMap<String, String>()
    val merged = ArrayList<Book>()
    for (book in books) {
        val key = book.sameFileKey()
        val group = if (key == null) listOf(book) else groups.getValue(key)
        if (group.size == 1) { merged += book; continue }
        val keep = group.maxWith(compareBy<Book>({ it.progress }, { it.lastOpened }))
        if (book.id != keep.id) { remap[book.id] = keep.id; continue }
        val others = group - keep
        // The explorer's file:// path is more dependable than a picker address, so it is the one kept when there is one.
        val bestUri = (listOf(keep) + others).map { it.uri }.firstOrNull { it.startsWith("file://") } ?: keep.uri
        merged += keep.copy(
            uri = bestUri,
            fileName = (listOf(keep) + others).map { it.fileName }.firstOrNull { it.isNotEmpty() } ?: keep.fileName,
            lists = (keep.lists + others.flatMap { it.lists }).distinct(),
            bookmarks = (keep.bookmarks + others.flatMap { it.bookmarks }).distinct().sorted(),
            comments = (keep.comments + others.flatMap { it.comments }).distinctBy { it.page to it.text },
            favorite = keep.favorite || others.any { it.favorite },
            rating = maxOf(keep.rating, others.maxOf { it.rating }),
            note = keep.note.ifBlank { others.map { it.note }.firstOrNull { it.isNotBlank() }.orEmpty() },
            readSeconds = keep.readSeconds + others.sumOf { it.readSeconds },
            pagesRead = keep.pagesRead + others.sumOf { it.pagesRead },
            lastOpened = maxOf(keep.lastOpened, others.maxOf { it.lastOpened }),
            added = listOf(keep.added).plus(others.map { it.added }).filter { it > 0 }.minOrNull() ?: keep.added,
            status = if (keep.status == Status.New) others.map { it.status }.firstOrNull { it != Status.New } ?: Status.New else keep.status,
        )
    }
    return copy(books = merged, sessions = sessions.map { if (it.bookId in remap) it.copy(bookId = remap.getValue(it.bookId)) else it })
}
