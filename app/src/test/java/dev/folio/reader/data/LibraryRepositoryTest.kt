package dev.folio.reader.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LibraryRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val job = Job()
    @After fun stop() = job.cancel()

    private val data by lazy { TempData(tmp.root) }
    private fun repo() = LibraryRepository(data, CoroutineScope(job))
    private fun book(id: String) = Book(id = id, uri = "u$id", name = "Book $id")
    private val libraryFile get() = data.file(Storage.LIBRARY)
    private fun backup(stamp: String, text: String) = File(data.dir(Storage.BACKUPS), "manhal-$stamp.json").apply { writeText(text) }
    private fun withBooks(vararg ids: String) = LibraryRepository.encode(LibraryState(books = ids.map(::book)))
    private fun corruptCopies() = tmp.root.listFiles { f -> f.name.startsWith("library.corrupt-") }.orEmpty().toList()

    @Test fun `no file is a new empty library and nothing to report`() {
        val r = repo()
        assertTrue(r.state.value.isBlank())
        assertNull(r.takeRecovery())
        assertTrue(corruptCopies().isEmpty())
    }

    @Test fun `what is saved is what loads, and no temp file is left behind`() {
        val a = repo()
        a.add(book("1"))
        a.flush()
        assertEquals(listOf("1"), repo().state.value.books.map { it.id })
        assertTrue(tmp.root.listFiles().orEmpty().none { it.name.endsWith(".tmp") })
    }

    @Test fun `a truncated file is kept aside and the newest backup is used`() {
        val whole = withBooks("1", "2")
        libraryFile.writeText(whole.take(whole.length / 2))
        backup("2026-10-01_10-00-00", withBooks("old"))
        val newest = backup("2026-10-02_10-00-00", withBooks("1", "2"))
        val r = repo()
        assertEquals(listOf("1", "2"), r.state.value.books.map { it.id })
        val rec = r.takeRecovery()!!
        assertEquals(newest, rec.restoredFrom)
        assertEquals(whole.take(whole.length / 2), rec.damaged!!.readText())
        assertTrue(rec.damaged!!.name.startsWith("library.corrupt-"))
        assertNull(r.takeRecovery())
    }

    @Test fun `the damaged file is not overwritten by loading`() {
        libraryFile.writeText("{ not json")
        repo()
        assertEquals("{ not json", libraryFile.readText())
    }

    @Test fun `garbage and no backup gives an empty library, a kept copy and a notice`() {
        libraryFile.writeText("garbage")
        val r = repo()
        assertTrue(r.state.value.isBlank())
        val rec = r.takeRecovery()!!
        assertNull(rec.restoredFrom)
        assertEquals("garbage", rec.damaged!!.readText())
    }

    @Test fun `an empty file counts as damaged`() {
        libraryFile.writeText("")
        backup("2026-10-01_10-00-00", withBooks("1"))
        val r = repo()
        assertEquals(listOf("1"), r.state.value.books.map { it.id })
        assertNotNull(r.takeRecovery())
    }

    @Test fun `unreadable and empty backups are skipped for an older good one`() {
        libraryFile.writeText("broken")
        backup("2026-10-03_10-00-00", "{}")
        backup("2026-10-02_10-00-00", "broken too")
        backup("2026-10-01_10-00-00", withBooks("good"))
        assertEquals(listOf("good"), repo().state.value.books.map { it.id })
    }

    @Test fun `two damaged loads keep two copies`() {
        libraryFile.writeText("one")
        repo()
        libraryFile.writeText("two")
        repo()
        assertEquals(setOf("one", "two"), corruptCopies().map { it.readText() }.toSet())
    }

    @Test fun `reload recovers too`() {
        val r = repo()
        r.add(book("1"))
        r.flush()
        libraryFile.writeText("oops")
        backup("2026-10-01_10-00-00", withBooks("1"))
        r.reload()
        assertEquals(listOf("1"), r.state.value.books.map { it.id })
        assertNotNull(r.takeRecovery())
    }

    @Test fun `a good file is not reported`() {
        libraryFile.writeText(withBooks("1"))
        val r = repo()
        assertNull(r.takeRecovery())
        assertFalse(r.state.value.isBlank())
    }

    @Test fun `the reading place survives a restart once it is flushed`() {
        val a = repo()
        a.add(book("1"))
        a.updateBook("1") { it.copy(page = 41, offset = 0.37f, pages = 300, status = Status.Reading) }
        a.flush()
        val b = repo().book("1")!!
        assertEquals(41, b.page)
        assertEquals(0.37f, b.offset, 0.0001f)
        assertEquals(Status.Reading, b.status)
    }
}
