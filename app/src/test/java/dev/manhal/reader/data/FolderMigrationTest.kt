package dev.manhal.reader.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderMigrationTest {
    private val root: File = Files.createTempDirectory("migration").toFile()
    private fun old(): File = File(root, LegacyNames.TOP_FOLDER).apply { mkdirs() }
    private fun target(): File = File(root, LegacyNames.ZIMAM_FOLDER)
    private fun File.put(name: String, text: String, time: Long? = null): File = File(this, name).apply { parentFile.mkdirs(); writeText(text); time?.let { setLastModified(it) } }

    @Test fun copiesEverythingVerifiedAndLeavesTheOldFolderUntouched() {
        val o = old(); o.put("library.json", "{}"); o.put("backups/a.json", "a")
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.problems.toString(), r.ok)
        assertEquals(2, r.copied)
        assertEquals("{}", File(target(), "library.json").readText())
        assertEquals("a", File(target(), "backups/a.json").readText())
        assertTrue(File(o, "library.json").exists() && File(o, "backups/a.json").exists())
        assertTrue(FolderMigration.marker(target(), LegacyNames.TOP_FOLDER).isFile)
        assertTrue(FolderMigration.safeToRemove(o, target()))
    }

    @Test fun neverOverwritesAClashingFileAndKeepsBoth() {
        val o = old(); o.put("library.json", "old data")
        target().put("library.json", "new data")
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok)
        assertEquals(1, r.kept)
        assertEquals("new data", File(target(), "library.json").readText())
        assertEquals("old data", File(target(), "library (from ${LegacyNames.TOP_FOLDER}).json").readText())
        assertTrue(FolderMigration.safeToRemove(o, target()))
    }

    @Test fun identicalFilesAreNotCopiedAgain() {
        val o = old(); o.put("a.txt", "same")
        target().put("a.txt", "same")
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok); assertEquals(0, r.copied); assertEquals(0, r.kept)
        assertEquals(listOf("a.txt", ".migrated-${LegacyNames.TOP_FOLDER}").sorted(), target().list()!!.toList().sorted())
    }

    @Test fun runningTwiceChangesNothing() {
        val o = old(); o.put("library.json", "x"); o.put("b/c.txt", "c")
        target().put("library.json", "other")
        FolderMigration.copyAndVerify(o, target())
        val before = target().walkTopDown().filter { it.isFile && !it.name.startsWith(".migrated") }.map { it.relativeTo(target()).path }.sorted().toList()
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok); assertEquals(0, r.copied); assertEquals(0, r.kept)
        assertEquals(before, target().walkTopDown().filter { it.isFile && !it.name.startsWith(".migrated") }.map { it.relativeTo(target()).path }.sorted().toList())
    }

    @Test fun anInterruptedCopyLeavesAPartFileThatTheNextRunReplaces() {
        val o = old(); o.put("big.bin", "0123456789")
        target().put("big.bin.part", "0123")
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok)
        assertEquals("0123456789", File(target(), "big.bin").readText())
        assertFalse(File(target(), "big.bin.part").exists())
    }

    @Test fun aPartialCopyIsCompletedOnTheNextRun() {
        val o = old(); o.put("a.txt", "a"); o.put("b.txt", "b"); o.put("c/d.txt", "d")
        target().put("a.txt", "a")
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok); assertEquals(2, r.copied)
        assertTrue(FolderMigration.verify(o, target()).isEmpty())
    }

    @Test fun aTargetThatCannotTakeAFileFailsAndKeepsEverything() {
        val o = old(); o.put("a.txt", "a")
        target().mkdirs(); File(target(), "a.txt").mkdirs()
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok.not() || File(target(), "a (from ${LegacyNames.TOP_FOLDER}).txt").readText() == "a")
        assertTrue(File(o, "a.txt").readText() == "a")
    }

    @Test fun verifyFailsWhenACopiedFileIsChangedOrMissing() {
        val o = old(); o.put("a.txt", "abc"); o.put("b.txt", "bcd")
        FolderMigration.copyAndVerify(o, target())
        File(target(), "a.txt").writeText("abX")
        File(target(), "b.txt").delete()
        val problems = FolderMigration.verify(o, target())
        assertEquals(2, problems.size)
        assertFalse(FolderMigration.safeToRemove(o, target()))
    }

    @Test fun sameSizeButDifferentContentIsNotTreatedAsCopied() {
        val o = old(); o.put("a.txt", "abc")
        target().put("a.txt", "abd")
        assertEquals(1, FolderMigration.verify(o, target()).size)
        val r = FolderMigration.copyAndVerify(o, target())
        assertTrue(r.ok); assertEquals("abd", File(target(), "a.txt").readText()); assertEquals("abc", File(target(), "a (from ${LegacyNames.TOP_FOLDER}).txt").readText())
    }

    @Test fun theCopyIsSkippedOnlyWhileTheOldFolderIsUnchanged() {
        val o = old(); o.put("a.txt", "a", 1_000)
        FolderMigration.copyAndVerify(o, target())
        assertTrue(FolderMigration.upToDate(o, target()))
        o.put("b.txt", "b", System.currentTimeMillis() + 60_000)
        assertFalse(FolderMigration.upToDate(o, target()))
    }

    @Test fun backupStateSavesTheLibraryOnlyOnce() {
        val t = target().apply { mkdirs() }; t.put("library.json", "{\"v\":1}")
        FolderMigration.backupState(t, 1)
        t.put("library.json", "{\"v\":2}")
        FolderMigration.backupState(t, 2)
        assertEquals(listOf("library-1.json"), File(t, "migration-backup").list()!!.toList())
        assertEquals("{\"v\":1}", File(t, "migration-backup/library-1.json").readText())
    }

    @Test fun nothingToDoForAMissingOrIdenticalFolder() {
        assertTrue(FolderMigration.copyAndVerify(File(root, "none"), target()).ok)
        val o = old(); assertTrue(FolderMigration.copyAndVerify(o, o).ok)
    }

    @Test fun aCancelledCopyStopsWithoutAMarkerAndTheNextRunFinishesIt() {
        val old = old(); val new = target()
        repeat(4) { old.put("f$it.txt", "data $it") }
        var seen = 0
        val first = FolderMigration.copyAndVerify(old, new, LegacyNames.TOP_FOLDER, cancelled = { seen++ >= 2 })
        assertFalse(first.ok)
        assertFalse(FolderMigration.marker(new, LegacyNames.TOP_FOLDER).exists())
        val second = FolderMigration.copyAndVerify(old, new, LegacyNames.TOP_FOLDER)
        assertTrue(second.ok)
        assertTrue(FolderMigration.marker(new, LegacyNames.TOP_FOLDER).exists())
        assertEquals(4, new.listFiles { f -> f.name.startsWith("f") }!!.size)
    }
}
