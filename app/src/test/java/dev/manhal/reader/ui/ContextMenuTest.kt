package dev.manhal.reader.ui

import dev.manhal.reader.EnglishStrings
import dev.manhal.reader.data.Book
import dev.manhal.reader.data.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextMenuTest {
    @Before fun english() = EnglishStrings.install()

    private val calls = mutableListOf<String>()
    private fun handlers() = BookMenuHandlers(
        open = { calls += "open" }, details = { calls += "details" }, edit = { calls += "edit" }, toggleFavorite = { calls += "favorite" },
        addToList = { calls += "list" }, toggleFinished = { calls += "finished" }, remove = { calls += "remove" },
    )
    private fun book(favorite: Boolean = false, status: Status = Status.Reading, physical: Boolean = false) = Book(id = "1", uri = "u", name = "B", favorite = favorite, status = status, physical = physical)

    @Test fun `a book menu has the standard entries and the destructive one is last`() {
        val m = bookMenu(book(), handlers())
        assertEquals(listOf("Open", "Details", "Edit details and cover…", "Add to favorites", "Add to a list", "Mark as finished", "Remove from library"), m.map { it.label })
        assertTrue(m.last().destructive)
        assertEquals(1, m.count { it.destructive })
    }

    @Test fun `every entry runs its own handler`() {
        bookMenu(book(), handlers()).forEach { it.onClick() }
        assertEquals(listOf("open", "details", "edit", "favorite", "list", "finished", "remove"), calls)
    }

    @Test fun `labels follow the book's state`() {
        val m = bookMenu(book(favorite = true, status = Status.Finished), handlers()).map { it.label }
        assertTrue("Remove from favorites" in m)
        assertTrue("Mark as reading" in m)
        assertFalse("Add to favorites" in m)
        assertEquals("Log a reading session", bookMenu(book(physical = true), handlers()).first().label)
    }

    @Test fun `a list menu ends with delete and a bookmark menu with remove`() {
        val l = listMenu({}, {}, {}, {}, {})
        assertEquals(listOf("Open", "Rename", "Move to another list…", "New list inside", "Delete this list"), l.map { it.label })
        assertTrue(l.last().destructive)
        val b = bookmarkMenu({ calls += "go" }, { calls += "rm" })
        assertEquals(listOf("Go to page", "Remove bookmark"), b.map { it.label })
        assertTrue(b.last().destructive)
        b.forEach { it.onClick() }
        assertTrue(calls.takeLast(2) == listOf("go", "rm"))
    }
}
