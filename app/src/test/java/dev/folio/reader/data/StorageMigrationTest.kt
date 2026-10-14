package dev.folio.reader.data

import java.io.File
import java.nio.file.Files
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class StorageMigrationTest {
    private fun tmp(): File = Files.createTempDirectory("rukn-mig").toFile()

    @Test fun movesTheOldFolderIntoANewParentAndMarksIt() {
        val root = tmp()
        val old = File(root, "Rukn").apply { mkdirs() }
        File(old, "library.json").writeText("{}")
        File(old, "backups").mkdirs(); File(old, "backups/a.json").writeText("a")
        val target = File(root, "Zimam/Rukn")
        assertTrue(Storage.migrateLegacyFolder(old, target))
        assertFalse(old.exists())
        assertEquals("{}", File(target, "library.json").readText())
        assertEquals("a", File(target, "backups/a.json").readText())
        assertTrue(File(target, Storage.MIGRATED).exists())
    }

    @Test fun mergesIntoAnExistingTargetAndKeepsTheNewerFile() {
        val root = tmp()
        val old = File(root, "Rukn").apply { mkdirs() }
        File(old, "library.json").apply { writeText("old"); setLastModified(1_000) }
        File(old, "extra.txt").writeText("extra")
        val target = File(root, "Zimam/Rukn").apply { mkdirs() }
        File(target, "library.json").apply { writeText("new!"); setLastModified(5_000) }
        assertTrue(Storage.migrateLegacyFolder(old, target))
        assertEquals("new!", File(target, "library.json").readText())
        assertEquals("extra", File(target, "extra.txt").readText())
        assertFalse(old.exists())
    }

    @Test fun neverDeletesTheOldFolderWhenACopyIsMissing() {
        val root = tmp()
        val old = File(root, "Rukn").apply { mkdirs() }
        File(old, "a.txt").writeText("a")
        val target = File(root, "Zimam/Rukn").apply { mkdirs() }
        assertFalse(Storage.isCovered(old, target))
        // A target that cannot hold a file (a directory sits where the file should go) makes the move fail and keep the source.
        File(target, "a.txt").mkdirs()
        assertFalse(Storage.migrateLegacyFolder(old, target))
        assertTrue(File(old, "a.txt").exists())
    }

    @Test fun doesNothingForTheSameFolderOrAMissingOne() {
        val root = tmp()
        val same = File(root, "Rukn").apply { mkdirs() }
        assertFalse(Storage.migrateLegacyFolder(same, same))
        assertFalse(Storage.migrateLegacyFolder(File(root, "none"), File(root, "Zimam/Rukn")))
    }
}
