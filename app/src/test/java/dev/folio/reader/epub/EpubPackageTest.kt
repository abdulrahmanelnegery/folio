package dev.folio.reader.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubPackageTest {
    @get:Rule val tmp = TemporaryFolder()

    private val container = """<?xml version="1.0"?><container><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>"""
    private val opf = """<package><metadata><dc:title>Dune &amp; Sons</dc:title><dc:creator>Frank Herbert</dc:creator><meta name="cover" content="cov"/></metadata>
        <manifest><item id="cov" href="images/cover.jpg" media-type="image/jpeg"/><item id="c1" href="ch%201.xhtml" media-type="application/xhtml+xml"/>
        <item id="c2" href="ch2.xhtml#top" media-type="application/xhtml+xml"/><item id="css" href="s.css" media-type="text/css"/></manifest>
        <spine><itemref idref="c1"/><itemref idref="css"/><itemref idref="c2"/></spine></package>"""

    @Test fun `the package file is found from the container`() {
        assertEquals("OEBPS/content.opf", EpubPackage.packagePath(container))
        assertNull(EpubPackage.packagePath("<container/>"))
    }

    @Test fun `title, author, reading order and cover are read`() {
        val p = EpubPackage.parse(opf, "OEBPS/content.opf")!!
        assertEquals("Dune & Sons", p.title)
        assertEquals("Frank Herbert", p.author)
        assertEquals(listOf("ch 1.xhtml", "ch2.xhtml"), p.spine)
        assertEquals("images/cover.jpg", p.cover)
        assertEquals("OEBPS", p.folder)
    }

    @Test fun `a book with no readable chapters is refused`() {
        assertNull(EpubPackage.parse("<package><manifest/><spine/></package>", "a.opf"))
    }

    @Test fun `unpacking refuses an entry that escapes the folder`() {
        val bytes = ByteArrayOutputStream().also { b -> ZipOutputStream(b).use { z -> z.putNextEntry(ZipEntry("../evil.txt")); z.write(1); z.closeEntry() } }.toByteArray()
        val dir = tmp.newFolder("book")
        assertFalse(EpubStore.unpack(bytes.inputStream(), dir))
        assertFalse(java.io.File(dir.parentFile, "evil.txt").exists())
    }

    @Test fun `a normal book unpacks`() {
        val bytes = ByteArrayOutputStream().also { b -> ZipOutputStream(b).use { z ->
            z.putNextEntry(ZipEntry("META-INF/container.xml")); z.write(container.toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("OEBPS/ch1.xhtml")); z.write("<p>hi</p>".toByteArray()); z.closeEntry()
        } }.toByteArray()
        val dir = tmp.newFolder("ok")
        assertTrue(EpubStore.unpack(bytes.inputStream(), dir))
        assertTrue(java.io.File(dir, "OEBPS/ch1.xhtml").exists())
    }
}

class ComicOrderTest {
    @Test fun `pages are ordered by number, not by letters`() {
        val names = listOf("p10.jpg", "p2.jpg", "p1.jpg", "p01b.jpg").sortedWith { a, b -> ComicStore.compareNatural(a, b) }
        assertEquals(listOf("p1.jpg", "p01b.jpg", "p2.jpg", "p10.jpg"), names)
    }
}
