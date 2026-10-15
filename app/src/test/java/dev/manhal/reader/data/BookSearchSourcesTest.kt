package dev.manhal.reader.data

import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BookSearchSourcesTest {
    @Test fun `a text file is searched as one unit`() = runBlocking {
        val p = BookSearchSources.text("one\ntwo cat\nthree", "cat").last()
        assertEquals(1, p.hits.size)
        assertEquals(1f, p.fraction, 0.001f)
        assertEquals(2, BookSearch.lineOf("one\ntwo cat\nthree", p.hits.single().start))
    }
}
