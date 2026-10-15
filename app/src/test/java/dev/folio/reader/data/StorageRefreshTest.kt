package dev.folio.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StorageRefreshTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var priv: File
    private lateinit var ext: File
    private var allFiles = true

    @Before fun dirs() { priv = tmp.newFolder("private"); ext = tmp.newFolder("sdcard") }

    private fun storage() = Storage(object : StorageEnv {
        override val privateDir get() = priv
        override val externalRoot get() = ext
        override fun canUseShared() = allFiles
    }) { old, target -> "could not verify $old -> $target" }

    private fun library(vararg ids: String) = LibraryRepository.encode(LibraryState(books = ids.map { Book(id = it, uri = "u$it", name = it) }))
    private fun at(folder: String, name: String = Storage.LIBRARY) = File(ext, "$folder/$name").also { it.parentFile.mkdirs() }
    private val shared get() = File(ext, "Zimam/Manhal")

    @Test fun `without all files access it stays in the private folder`() {
        allFiles = false
        at("Rukn").writeText(library("a"))
        val s = storage()
        assertEquals(priv, s.root)
        assertFalse(s.refresh())
        assertFalse(s.shared)
    }

    @Test fun `with access and no old folder it moves to the Zimam folder`() {
        val s = storage()
        assertEquals(priv, s.root)
        assertTrue(s.refresh())
        assertEquals(shared, s.root)
        assertFalse(s.refresh())
    }

    @Test fun `an old folder is copied across, verified, left in place, and its library is used`() {
        at("Rukn").writeText(library("a", "b"))
        at("Rukn", "covers/a.jpg").writeText("img")
        val s = storage()
        assertEquals(File(ext, "Rukn"), s.root)
        assertTrue(s.refresh())
        assertEquals(shared, s.root)
        assertEquals(2, LibraryRepository.decode(s.file(Storage.LIBRARY).readText()).books.size)
        assertEquals("img", File(shared, "covers/a.jpg").readText())
        assertTrue(File(ext, "Rukn/${Storage.LIBRARY}").isFile)
        assertNull(s.migrationMessage)
        assertNull(s.otherLibraryBooks)
    }

    @Test fun `a second start goes straight to the Zimam folder once every old folder is marked`() {
        at("Rukn").writeText(library("a"))
        storage().refresh()
        assertEquals(shared, storage().root)
    }

    @Test fun `two different libraries keep the target's own, and the other one is offered as a backup`() {
        at("Rukn").writeText(library("old1", "old2", "old3"))
        File(shared, Storage.LIBRARY).apply { parentFile.mkdirs() }.writeText(library("new"))
        val s = storage()
        assertTrue(s.refresh())
        assertEquals(listOf("new"), LibraryRepository.decode(s.file(Storage.LIBRARY).readText()).books.map { it.id })
        assertEquals(3, s.otherLibraryBooks)
        val backups = Backups(s)
        assertEquals(listOf("old1", "old2", "old3"), backups.read(backups.files().single())!!.books.map { it.id })
        assertTrue(File(shared, "library (from Rukn).json").isFile)
        assertTrue(shared.resolve("migration-backup").listFiles()!!.isNotEmpty())
    }

    @Test fun `identical libraries make no extra backup and no notice`() {
        at("Rukn").writeText(library("a"))
        File(shared, Storage.LIBRARY).apply { parentFile.mkdirs() }.writeText(library("a"))
        val s = storage()
        s.refresh()
        assertNull(s.otherLibraryBooks)
        assertTrue(Backups(s).files().isEmpty())
    }

    @Test fun `an old folder that cannot be copied keeps the old location in use and says so`() {
        val lib = at("Rukn").apply { writeText(library("a")) }
        val secret = at("Rukn", "covers/x.jpg").apply { writeText("img") }
        secret.setReadable(false)
        try {
            assumeFalse("running as a user that ignores permissions", secret.canRead())
            val s = storage()
            assertFalse(s.refresh())
            assertEquals(File(ext, "Rukn"), s.root)
            assertNotNull(s.migrationMessage)
            assertTrue(lib.isFile)
        } finally {
            secret.setReadable(true)
        }
    }

    @Test fun `the old Rukn folder and the old Zimam Rukn folder do not share a marker`() {
        at("Rukn").writeText(library("top"))
        at("Zimam/Rukn", "notes.txt").writeText("n")
        val s = storage()
        s.refresh()
        assertEquals(setOf(".migrated-Rukn", ".migrated-Zimam-Rukn"), shared.list()!!.filter { it.startsWith(".migrated") }.toSet())
    }
}
