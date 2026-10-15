package dev.manhal.reader.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryRestoreTest {
    @get:Rule val tmp = TemporaryFolder()
    private val job = Job()
    @After fun stop() = job.cancel()

    private val data by lazy { TempData(tmp.root) }
    private val repo by lazy { LibraryRepository(data, CoroutineScope(job)) }
    private val backups by lazy { Backups(data) }
    private fun book(id: String) = Book(id = id, uri = "u$id", name = "Book $id")
    private fun restore(state: LibraryState?) = LibraryRestore(repo, backups).apply(state)

    @Test fun `an empty object is not a backup and changes nothing`() {
        repo.add(book("mine"))
        val foreign = LibraryRepository.decode("{}")
        assertEquals(LibraryRestore.Result.NotABackup, restore(foreign))
        assertEquals(listOf("mine"), repo.state.value.books.map { it.id })
        assertTrue(backups.files().isEmpty())
    }

    @Test fun `another app's json is not a backup`() {
        repo.add(book("mine"))
        val thabit = LibraryRepository.decode("""{"habits":[{"id":"1","name":"Run"}],"version":3}""")
        assertEquals(LibraryRestore.Result.NotABackup, restore(thabit))
        assertEquals(listOf("mine"), repo.state.value.books.map { it.id })
    }

    @Test fun `a file that did not decode is not a backup`() {
        assertEquals(LibraryRestore.Result.NotABackup, restore(null))
    }

    @Test fun `a real backup is applied after the current library is copied`() {
        repo.add(book("mine"))
        assertEquals(LibraryRestore.Result.Restored, restore(LibraryState(books = listOf(book("old")))))
        assertEquals(listOf("old"), repo.state.value.books.map { it.id })
        val copy = backups.files().single()
        assertEquals(listOf("mine"), backups.read(copy)!!.books.map { it.id })
        assertEquals(listOf("old"), LibraryRepository.decode(data.file(Storage.LIBRARY).readText()).books.map { it.id })
    }

    @Test fun `restoring into an empty library needs no safety copy`() {
        assertEquals(LibraryRestore.Result.Restored, restore(LibraryState(books = listOf(book("old")))))
        assertTrue(backups.files().isEmpty())
    }

    @Test fun `nothing is replaced when the safety copy cannot be written`() {
        repo.add(book("mine"))
        val dir = data.dir(Storage.BACKUPS)
        dir.setWritable(false)
        try {
            assumeFalse("running as a user that ignores permissions", dir.canWrite())
            assertEquals(LibraryRestore.Result.NoSafetyCopy, restore(LibraryState(books = listOf(book("old")))))
            assertEquals(listOf("mine"), repo.state.value.books.map { it.id })
        } finally {
            dir.setWritable(true)
        }
    }
}
