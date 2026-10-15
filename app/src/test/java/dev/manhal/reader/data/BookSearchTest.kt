package dev.manhal.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSearchTest {
    @Test fun `html becomes plain text without tags, scripts or entities`() {
        val html = "<html><head><style>p{color:red}</style></head><body><h1>Title</h1><p>Tom &amp; Jerry&nbsp;ran&#33; &#x41;</p><script>var x='cat';</script></body></html>"
        assertEquals("Title Tom & Jerry ran! A", BookSearch.htmlToText(html))
    }

    @Test fun `arabic html is searchable after conversion`() {
        val text = BookSearch.htmlToText("<p>كَانَ يا مَا كَانَ</p>")
        assertEquals(1, TextSearch.find(text, "كان").size.coerceAtMost(1))
        assertTrue(TextSearch.find(text, "يا ما").isNotEmpty())
    }

    @Test fun `line numbers count from one`() {
        assertEquals(1, BookSearch.lineOf("abc\ndef", 0))
        assertEquals(2, BookSearch.lineOf("abc\ndef", 5))
        assertEquals(2, BookSearch.lineOf("abc\ndef", 999))
    }

    private fun units(vararg t: String) = t.withIndex().map { it.index to it.value }.asSequence()

    @Test fun `hits come back in book order with their unit`() {
        val hits = BookSearch.search(units("no match", "a cat here", "cat and cat"), 3, "cat")
        assertEquals(listOf(1, 2, 2), hits.map { it.unit })
    }

    @Test fun `one unit cannot flood the results and the total is capped`() {
        val many = "cat ".repeat(50)
        assertEquals(5, BookSearch.search(units(many), 1, "cat").size)
        assertEquals(7, BookSearch.search(units(many, many, many), 3, "cat", limit = 7).size)
    }

    @Test fun `progress is reported after every unit and stops early at the limit`() {
        val seen = mutableListOf<Float>()
        BookSearch.search(units("cat", "cat", "cat", "cat"), 4, "cat", limit = 2, perUnit = 1) { seen += it.fraction }
        assertEquals(listOf(0.25f, 0.5f), seen)
    }

    @Test fun `a book with no text says so`() {
        var last: SearchProgress? = null
        BookSearch.search(units("", "  "), 2, "cat") { last = it }
        assertFalse(last!!.textFound)
        assertTrue(last!!.hits.isEmpty())
    }

    @Test fun `a blank query finds nothing`() = assertTrue(BookSearch.search(units("cat"), 1, "  ").isEmpty())
}
