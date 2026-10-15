package dev.manhal.reader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OriginalsFolderTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `counts and sizes the kept copies`() {
        File(tmp.root, "a.pdf").writeBytes(ByteArray(100))
        File(tmp.root, "b.pdf").writeBytes(ByteArray(250))
        val o = OriginalsFolder(tmp.root)
        assertEquals(2, o.count)
        assertEquals(350L, o.bytes)
    }

    @Test fun `clearing removes the copies and reports how many`() {
        File(tmp.root, "a.pdf").writeText("x")
        File(tmp.root, "b.pdf").writeText("y")
        assertEquals(2, OriginalsFolder(tmp.root).clear())
        assertTrue(tmp.root.listFiles()!!.isEmpty())
    }

    @Test fun `a missing folder is empty and clearing it does nothing`() {
        val o = OriginalsFolder(File(tmp.root, "nope"))
        assertEquals(0, o.count)
        assertEquals(0L, o.bytes)
        assertEquals(0, o.clear())
    }
}
