package dev.folio.reader.data

import kotlinx.serialization.Serializable

@Serializable
enum class Status { New, Reading, ToRead, Finished }

@Serializable
enum class SortOrder(val label: String) {
    Recent("Last opened"),
    Name("Name"),
    Size("Size"),
}

@Serializable
enum class PageTheme(val label: String) {
    Normal("Normal"),
    Night("Night"),
    SoftNight("Soft night"),
    Sepia("Sepia");

    fun next(): PageTheme = entries[(ordinal + 1) % entries.size]
}

/** A book in the library and where the reader stopped in it. */
@Serializable
data class Book(
    val id: String,
    val uri: String,
    val name: String,
    val pages: Int = 0,
    val size: Long = 0,
    val lastOpened: Long = 0,
    val favorite: Boolean = false,
    val status: Status = Status.New,
    val lists: List<String> = emptyList(),
    /** Zero-based page at the top of the screen when the book was closed. */
    val page: Int = 0,
    /** How far down that page (0..1). */
    val offset: Float = 0f,
) {
    /** Reading progress 0..1; a finished book counts as complete. */
    val progress: Float
        get() = when {
            status == Status.Finished -> 1f
            pages <= 0 || page <= 0 -> 0f
            else -> ((page + offset) / pages).coerceIn(0f, 1f)
        }
}

@Serializable
data class LibraryState(
    val books: List<Book> = emptyList(),
    val lists: List<String> = emptyList(),
    val grid: Boolean = true,
    val sort: SortOrder = SortOrder.Recent,
    val theme: PageTheme = PageTheme.Normal,
)

/** What the library screen is showing. */
sealed interface Tab {
    data object ReadingNow : Tab
    data object ToRead : Tab
    data object Finished : Tab
    data object Favorites : Tab
    data object All : Tab
    data class List(val name: String) : Tab
}

fun Book.inTab(tab: Tab): Boolean = when (tab) {
    Tab.ReadingNow -> status == Status.Reading
    Tab.ToRead -> status == Status.ToRead
    Tab.Finished -> status == Status.Finished
    Tab.Favorites -> favorite
    Tab.All -> true
    is Tab.List -> tab.name in lists
}

/** The books to show: the shelf, the search, in the chosen order. */
fun LibraryState.visible(tab: Tab, query: String): List<Book> {
    val q = query.trim().lowercase()
    val shelf = books.filter { it.inTab(tab) && (q.isEmpty() || it.name.lowercase().contains(q)) }
    return when (sort) {
        SortOrder.Recent -> shelf.sortedByDescending { it.lastOpened }
        SortOrder.Name -> shelf.sortedBy { it.name.lowercase() }
        SortOrder.Size -> shelf.sortedByDescending { it.size }
    }
}
