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
) {
    /** Reading progress 0..1; a finished book counts as complete. */
    val progress: Float
        get() = when {
            status == Status.Finished -> 1f
            kind == Kind.Text -> offset.coerceIn(0f, 1f)
            pages <= 0 || page <= 0 -> 0f
            else -> ((page + offset) / pages).coerceIn(0f, 1f)
        }

    /** Where the reader is: "Page 3 of 12", "Read 40%", or just the size of the book. */
    val placeLabel: String
        get() = when {
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
    /** Ask before leaving Folio when back is pressed on the home screen. */
    val confirmExit: Boolean = true,
    /** Reading time and pages per day, keyed "yyyy-MM-dd" in the phone's time zone. */
    val days: Map<String, DayStat> = emptyMap(),
)

/** What the library screen is showing. */
sealed interface Tab {
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
