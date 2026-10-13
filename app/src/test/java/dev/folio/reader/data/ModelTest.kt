package dev.folio.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun `moving a list takes its lists and books along`() {
        val s = state(book("1", "Work/Courses"), book("2", "Work"), lists = listOf("Work", "Work/Courses", "Home"))
        val moved = s.withListMoved("Work", "Home")!!
        assertEquals(listOf("Home/Work", "Home/Work/Courses", "Home"), moved.lists)
        assertEquals(listOf("Home/Work/Courses"), moved.books.first { it.id == "1" }.lists)
        assertEquals(listOf("Home/Work"), moved.books.first { it.id == "2" }.lists)
    }

    @Test
    fun `a list cannot move into itself or a clash`() {
        val s = state(lists = listOf("A", "A/B", "C", "C/A"))
        assertNull(s.withListMoved("A", "A/B"))
        assertNull(s.withListMoved("A", "A"))
        assertNull("C already has an A", s.withListMoved("A", "C"))
        assertEquals("already there", s, s.withListMoved("A/B", "A"))
    }

    @Test
    fun `a list can move to the top level`() {
        val s = state(lists = listOf("A", "A/B"))
        assertEquals(listOf("A", "B"), s.withListMoved("A/B", "")!!.lists)
    }

    @Test
    fun `move targets leave out the list itself, what is inside it and where it already is`() {
        val s = state(lists = listOf("A", "A/B", "C"))
        assertEquals(listOf("", "C"), s.moveTargets("A/B"))
        assertEquals(listOf("C"), s.moveTargets("A"))
    }

    @Test
    fun `most read ranks books by time in the last days and ignores old sessions`() {
        val now = 10L * 24 * 60 * 60 * 1000
        val day = 24L * 60 * 60 * 1000
        val s = state(book("1"), book("2"), book("3")).copy(
            sessions = listOf(
                ReadSession("1", now - day, 600, 5),
                ReadSession("2", now - 2 * day, 1200, 9),
                ReadSession("1", now - 3 * day, 900, 7),
                ReadSession("3", now - 9 * day, 5000, 50),
            ),
        )
        val top = s.mostRead(7, 5, now)
        assertEquals(listOf("1" to 1500L, "2" to 1200L), top.map { it.first.id to it.second })
        assertEquals(3, s.sessionsSince(7, now).size)
    }

    @Test
    fun `home sections keep the user's order and add new ones at the end`() {
        val s = LibraryState(home = listOf(HomeSectionConfig(HomeSection.Stats, false), HomeSectionConfig(HomeSection.Continue)))
        val sections = s.homeSections()
        assertEquals(HomeSection.Stats, sections[0].section)
        assertFalse(sections[0].visible)
        assertEquals(HomeSection.Continue, sections[1].section)
        assertEquals(HomeSection.entries.size, sections.size)
        assertEquals("an empty setting shows every section", HomeSection.entries.size, LibraryState().homeSections().size)
    }

    @Test
    fun `the same file added twice becomes one entry that keeps everything`() {
        val a = Book("a", "content://picker/1", "Book", fileName = "Book.pdf", size = 1000, lists = listOf("X"), favorite = true, lastOpened = 5, readSeconds = 30)
        val b = Book("b", "file:///x/Book.pdf", "Book", fileName = "Book.pdf", size = 1000, pages = 10, page = 5, lists = listOf("Y"), rating = 4, lastOpened = 9, readSeconds = 60, status = Status.Reading)
        val other = Book("c", "u", "Other", fileName = "Other.pdf", size = 10)
        val s = LibraryState(books = listOf(a, b, other), sessions = listOf(ReadSession("a", 1, 10, 1), ReadSession("b", 2, 20, 2))).deduplicated()
        assertEquals(2, s.books.size)
        val merged = s.books.first { it.fileName == "Book.pdf" }
        assertEquals("the one with more progress stays", "b", merged.id)
        assertEquals(listOf("Y", "X"), merged.lists)
        assertEquals(4, merged.rating)
        assertTrue(merged.favorite)
        assertEquals(90, merged.readSeconds)
        assertEquals(listOf("b", "b"), s.sessions.map { it.bookId })
    }

    @Test
    fun `entries of different files or unknown sizes are left alone`() {
        val s = state(
            Book("a", "u", "A", fileName = "A.pdf", size = 5),
            Book("b", "u", "A", fileName = "A.pdf", size = 6),
            Book("c", "u", "C", fileName = "C.pdf", size = 0),
            Book("d", "u", "C", fileName = "C.pdf", size = 0),
        )
        assertEquals(4, s.deduplicated().books.size)
        assertEquals("b", s.findSameFile("a.PDF", 6)?.id)
        assertNull(s.findSameFile("C.pdf", 0))
    }
}
