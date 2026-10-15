package dev.manhal.reader.edits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SafeWriteTest {
    @get:Rule val tmp = TemporaryFolder()

    /** A stand-in for the PDF code: the "PDF" is a text file whose first line is its page count. */
    private class Fake(var failWrite: Boolean = false, var pagesAfter: Int? = null, var crashWriteMidway: Boolean = false, var outOfMemory: Boolean = false) : PdfWork {
        override fun write(source: File, out: File) {
            if (outOfMemory) throw OutOfMemoryError("huge pdf")
            if (failWrite) throw IllegalStateException("cannot write")
            out.writeText("${pagesAfter ?: pageCount(source)}\nedited")
            if (crashWriteMidway) throw java.io.IOException("disk full")
        }
        override fun pageCount(file: File): Int? = runCatching { file.readLines().first().toInt() }.getOrNull()
    }

    private fun original(text: String = "3\noriginal") = tmp.newFile("book.pdf").apply { writeText(text) }

    @Test fun `save as new writes a checked copy and leaves the original alone`() {
        val o = original()
        val target = File(tmp.root, "book (edited).pdf")
        val r = SafeWrite.saveAsNew(o, target, Fake())
        assertTrue(r is SaveResult.Saved)
        assertEquals("3\nedited", target.readText())
        assertEquals("3\noriginal", o.readText())
        assertFalse(File(tmp.root, ".book (edited).pdf.part").exists())
    }

    @Test fun `save as new never replaces a file that is already there`() {
        val o = original()
        val target = tmp.newFile("book (edited).pdf").apply { writeText("keep me") }
        assertTrue(SafeWrite.saveAsNew(o, target, Fake()) is SaveResult.Failed)
        assertEquals("keep me", target.readText())
    }

    @Test fun `save as new leaves nothing behind when writing fails`() {
        val o = original()
        val target = File(tmp.root, "x.pdf")
        assertTrue(SafeWrite.saveAsNew(o, target, Fake(failWrite = true)) is SaveResult.Failed)
        assertFalse(target.exists())
        assertEquals(listOf("book.pdf"), tmp.root.list()!!.sorted())
    }

    @Test fun `save as new refuses a copy with a different page count`() {
        val o = original()
        val target = File(tmp.root, "x.pdf")
        assertTrue(SafeWrite.saveAsNew(o, target, Fake(pagesAfter = 2)) is SaveResult.Failed)
        assertFalse(target.exists())
    }

    @Test fun `overwrite keeps a backup and swaps in the edited file`() {
        val o = original()
        val backup = File(tmp.root, "backups/book.orig")
        assertTrue(SafeWrite.overwrite(o, backup, Fake()) is SaveResult.Saved)
        assertEquals("3\nedited", o.readText())
        assertEquals("3\noriginal", backup.readText())
        assertFalse(File(tmp.root, ".book.pdf.part").exists())
    }

    @Test fun `overwrite leaves the original untouched when writing fails or stops midway`() {
        for (fake in listOf(Fake(failWrite = true), Fake(crashWriteMidway = true))) {
            val o = original()
            val r = SafeWrite.overwrite(o, File(tmp.root, "b.orig"), fake)
            assertTrue(r is SaveResult.Failed)
            assertEquals("3\noriginal", o.readText())
            assertFalse(File(tmp.root, ".book.pdf.part").exists())
            o.delete()
        }
    }

    @Test fun `overwrite leaves the original untouched when the result does not check out`() {
        val o = original()
        assertTrue(SafeWrite.overwrite(o, File(tmp.root, "b.orig"), Fake(pagesAfter = 7)) is SaveResult.Failed)
        assertEquals("3\noriginal", o.readText())
    }

    @Test fun `overwrite refuses a file that is not a readable PDF`() {
        val o = original("not a pdf")
        assertTrue(SafeWrite.overwrite(o, File(tmp.root, "b.orig"), Fake()) is SaveResult.Failed)
        assertEquals("not a pdf", o.readText())
    }

    @Test fun `overwrite refuses a read only file`() {
        val o = original()
        o.setWritable(false)
        org.junit.Assume.assumeFalse("running as a user that ignores the read only flag", o.canWrite())
        val r = SafeWrite.overwrite(o, File(tmp.root, "b.orig"), Fake())
        o.setWritable(true)
        assertTrue(r is SaveResult.Failed)
        assertEquals("3\noriginal", o.readText())
    }

    @Test fun `the edited name avoids taking another file's name`() {
        val o = File("/books/Great Book.pdf")
        assertEquals("Great Book (edited).pdf", SafeWrite.editedName(o) { false })
        assertEquals("Great Book (edited 2).pdf", SafeWrite.editedName(o) { it == "Great Book (edited).pdf" })
        assertEquals("Great Book (edited 3).pdf", SafeWrite.editedName(o) { it.endsWith("(edited).pdf") || it.endsWith("(edited 2).pdf") })
    }

    @Test fun `running out of memory while saving is a failure, not a crash, and the original is untouched`() {
        val o = original()
        val target = File(tmp.root, "x.pdf")
        assertTrue(SafeWrite.saveAsNew(o, target, Fake(outOfMemory = true)) is SaveResult.Failed)
        assertFalse(target.exists())
        assertTrue(SafeWrite.overwrite(o, File(tmp.root, "b.orig"), Fake(outOfMemory = true)) is SaveResult.Failed)
        assertEquals("3\noriginal", o.readText())
        assertFalse(File(tmp.root, ".book.pdf.part").exists())
    }
}
