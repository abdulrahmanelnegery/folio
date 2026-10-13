package dev.folio.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {
    private fun book(id: String, vararg lists: String, status: Status = Status.Reading) =
        Book(id = id, uri = "u$id", name = "Book $id", lists = lists.toList(), status = status)

    private fun state(vararg books: Book, lists: List<String> = emptyList()) = LibraryState(books = books.toList(), lists = lists)

    @Test
    fun `a list shows its own books, a search looks through the lists inside`() {
        val s = state(book("1", "Work"), book("2", "Work/Courses"), book("3"), lists = listOf("Work", "Work/Courses"))
        assertEquals(listOf("1"), s.visible(Tab.List("Work"), "").map { it.id })
        assertEquals(listOf("2"), s.visible(Tab.List("Work"), "Book 2").map { it.id })
        assertEquals(listOf("2"), s.visible(Tab.List("Work/Courses"), "").map { it.id })
    }

    @Test
    fun `counts for a list include the lists inside it`() {
        val s = state(book("1", "Work"), book("2", "Work/Courses"))
        assertEquals(2, s.books.count { it.inTab(Tab.List("Work")) })
        assertEquals(1, s.books.count { it.inTab(Tab.List("Work/Courses")) })
    }

    @Test
    fun `ordered lists put children right after their parent`() {
        val s = state(lists = listOf("B", "A", "A/x", "B/y", "A/x/z"))
        assertEquals(listOf("B", "B/y", "A", "A/x", "A/x/z"), s.orderedLists())
    }

    @Test
    fun `a list whose parent is missing still shows`() {
        assertEquals(listOf("Gone/child"), state(lists = listOf("Gone/child")).orderedLists())
    }

    @Test
    fun `path helpers`() {
        assertEquals("A/b", parentOf("A/b/c"))
        assertEquals("", parentOf("A"))
        assertEquals("c", nameOf("A/b/c"))
        assertEquals(2, depthOf("A/b/c"))
        assertTrue(isInside("A/b", "A"))
        assertFalse(isInside("AB", "A"))
    }

    @Test
    fun `progress of a text book follows the offset and a finished book is complete`() {
        assertEquals(0.4f, Book("t", "u", "n", kind = Kind.Text, offset = 0.4f).progress, 0.0001f)
        assertEquals(1f, Book("t", "u", "n", status = Status.Finished).progress, 0.0001f)
        assertEquals(0.5f, Book("p", "u", "n", pages = 10, page = 5).progress, 0.0001f)
    }

    @Test
    fun `sorting by rating puts the best first`() {
        val s = state(book("1").copy(rating = 2), book("2").copy(rating = 5)).copy(sort = SortOrder.Rating)
        assertEquals(listOf("2", "1"), s.visible(Tab.All, "").map { it.id })
    }

    @Test
    fun `streak counts days in a row, and today may still be empty`() {
        val now = 1_760_000_000_000L
        val day = 24L * 60 * 60 * 1000
        val days = mapOf(
            dayKey(now - day) to DayStat(120, 3),
            dayKey(now - 2 * day) to DayStat(300, 9),
            dayKey(now - 4 * day) to DayStat(600, 20),
        )
        val s = LibraryState(days = days)
        assertEquals(2, s.streak(now))
        assertEquals(DayStat(), s.day(0, now))
        assertEquals(420, s.lastDays(3, now).seconds)
        val withToday = s.copy(days = days + (dayKey(now) to DayStat(90, 1)))
        assertEquals(3, withToday.streak(now))
    }

    @Test
    fun `file types are told apart by name`() {
        assertEquals(Kind.Pdf, FileTypes.kindOf("a.PDF"))
        assertEquals(Kind.Epub, FileTypes.kindOf("a.epub"))
        assertEquals(Kind.Comic, FileTypes.kindOf("a.cbz"))
        assertEquals(Kind.Text, FileTypes.kindOf("notes.md"))
        assertEquals(Kind.Text, FileTypes.kindOf("Dockerfile"))
        assertEquals(TextLang.Docker, FileTypes.textLang("Dockerfile.dev"))
        assertEquals(TextLang.Yaml, FileTypes.textLang("docker-compose.yml"))
        assertEquals(null, FileTypes.kindOf("photo.png"))
    }

    @Test
    fun `binary data is not taken for text`() {
        assertTrue(FileTypes.looksLikeText("hello\nworld".toByteArray(), 11))
        assertFalse(FileTypes.looksLikeText(byteArrayOf(1, 0, 2, 3), 4))
    }
}
