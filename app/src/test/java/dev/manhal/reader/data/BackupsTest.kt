package dev.manhal.reader.data

import org.junit.Assume.assumeFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupsTest {
    @get:Rule val tmp = TemporaryFolder()
    private val data by lazy { TempData(tmp.root) }
    private val state = LibraryState(books = listOf(Book(id = "1", uri = "u", name = "B")))

    private fun seed(count: Int) = (1..count).forEach { n ->
        data.dir(Storage.BACKUPS).resolve("manhal-2026-01-01_00-00-%02d.json".format(n)).writeText(LibraryRepository.encode(state))
    }

    @Test fun `a new backup is written and old ones beyond the limit are pruned`() {
        seed(Backups.KEEP)
        val file = Backups(data).now(state)
        assertNotNull(file)
        assertEquals(Backups.KEEP, Backups(data).files().size)
        assertEquals(file, Backups(data).files().first())
    }

    @Test fun `a failed write prunes nothing`() {
        seed(Backups.KEEP)
        val dir = data.dir(Storage.BACKUPS)
        dir.setWritable(false)
        try {
            assumeFalse("running as a user that ignores permissions", dir.canWrite())
            assertNull(Backups(data).now(state))
        } finally {
            dir.setWritable(true)
        }
        assertEquals(Backups.KEEP, Backups(data).files().size)
    }

    @Test fun `newestValid skips blank and unreadable copies`() {
        val dir = data.dir(Storage.BACKUPS)
        dir.resolve("manhal-2026-01-03_00-00-00.json").writeText("{}")
        dir.resolve("manhal-2026-01-02_00-00-00.json").writeText("nope")
        dir.resolve("manhal-2026-01-01_00-00-00.json").writeText(LibraryRepository.encode(state))
        assertEquals("manhal-2026-01-01_00-00-00.json", Backups(data).newestValid()!!.first.name)
    }
}
