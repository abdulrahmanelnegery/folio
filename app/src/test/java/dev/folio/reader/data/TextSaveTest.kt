package dev.folio.reader.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class TextSaveTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `a file is replaced whole and no temp file is left`() {
        val f = tmp.newFile("note.md").apply { writeText("old") }
        assertTrue(TextSave.toFile(f, "new text".toByteArray()))
        assertEquals("new text", f.readText())
        assertEquals(listOf("note.md"), tmp.root.list()!!.sorted())
    }

    @Test fun `a failed file save leaves the original byte identical`() {
        val f = tmp.newFile("note.md").apply { writeText("old") }
        tmp.root.setWritable(false)
        try {
            assumeFalse("running as a user that ignores permissions", tmp.root.canWrite())
            assertFalse(TextSave.toFile(f, "new".toByteArray()))
        } finally {
            tmp.root.setWritable(true)
        }
        assertEquals("old", f.readText())
    }

    private class Provider(var content: ByteArray, val failMidway: Boolean = false) {
        fun out(): OutputStream = object : OutputStream() {
            val buf = ByteArrayOutputStream()
            override fun write(b: Int) { buf.write(b) }
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (failMidway) { content = b.copyOfRange(off, off + len / 2); throw IOException("provider died") }
                buf.write(b, off, len)
            }
            override fun close() { if (!failMidway) content = buf.toByteArray() }
        }
        fun input() = ByteArrayInputStream(content)
    }

    @Test fun `a provider save is read back and the spare copy is dropped`() {
        val spare = tmp.newFile("draft")
        val p = Provider("old".toByteArray())
        assertTrue(TextSave.toProvider("new".toByteArray(), spare, p::out, p::input))
        assertEquals("new", String(p.content))
        assertFalse(spare.exists())
    }

    @Test fun `a provider that dies midway fails and the whole new text stays in the spare copy`() {
        val spare = tmp.newFile("draft")
        val p = Provider("old".toByteArray(), failMidway = true)
        assertFalse(TextSave.toProvider("new text".toByteArray(), spare, p::out, p::input))
        assertArrayEquals("new text".toByteArray(), spare.readBytes())
    }

    @Test fun `a provider that stores something different fails and keeps the spare copy`() {
        val spare = tmp.newFile("draft")
        val p = Provider(ByteArray(0))
        val liar = object { fun out() = object : OutputStream() { override fun write(b: Int) {} } }
        assertFalse(TextSave.toProvider("new".toByteArray(), spare, liar::out, p::input))
        assertTrue(spare.exists())
    }

    @Test fun `a provider that cannot be opened fails and keeps the spare copy`() {
        val spare = tmp.newFile("draft")
        assertFalse(TextSave.toProvider("new".toByteArray(), spare, { null }, { null }))
        assertEquals("new", spare.readText())
    }
}
