package dev.folio.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {
    @Test fun `case is ignored and positions point at the original text`() {
        val hits = TextSearch.find("The Cat sat. The cat ran.", "CAT")
        assertEquals(listOf(4, 17), hits.map { it.start })
        assertEquals("Cat", "The Cat sat. The cat ran.".substring(hits[0].start, hits[0].end))
    }

    @Test fun `an empty or blank query finds nothing`() {
        assertTrue(TextSearch.find("abc", "").isEmpty())
        assertTrue(TextSearch.find("abc", "   ").isEmpty())
    }

    @Test fun `arabic diacritics are ignored in text and query`() {
        val text = "كَتَبَ الطَّالِبُ الدَّرْسَ"
        val hits = TextSearch.find(text, "الطالب")
        assertEquals(1, hits.size)
        assertEquals("الطَّالِب", text.substring(hits[0].start, hits[0].end))
        assertEquals(1, TextSearch.find("الطالب", "الطَّالِبُ").size)
    }

    @Test fun `alef ya and ta marbuta variants match`() {
        assertEquals(1, TextSearch.find("أحمد في المدرسة", "احمد").size)
        assertEquals(1, TextSearch.find("على الطاولة", "علي").size)
        assertEquals(1, TextSearch.find("المدرسة", "المدرسه").size)
    }

    @Test fun `tatweel is ignored`() = assertEquals(1, TextSearch.find("الســلام", "السلام").size)

    @Test fun `matches do not overlap and the limit is honoured`() {
        assertEquals(2, TextSearch.find("aaaa", "aa").size)
        assertEquals(3, TextSearch.find("a a a a a", "a", limit = 3).size)
    }

    @Test fun `the snippet is trimmed to the context and whitespace is collapsed`() {
        val h = TextSearch.find("one\n\n   two   target   three four", "target", context = 8).single()
        assertTrue(h.snippet.contains("two target"))
        assertTrue(!h.snippet.contains("four") && !h.snippet.contains("\n") && !h.snippet.contains("  "))
    }
}
