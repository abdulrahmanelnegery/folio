package dev.folio.reader.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import dev.folio.reader.edits.Box
import dev.folio.reader.edits.Draft
import dev.folio.reader.edits.Highlight
import dev.folio.reader.edits.Ink
import dev.folio.reader.edits.Note
import dev.folio.reader.edits.Pt
import dev.folio.reader.edits.Underline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PdfEditWriterTest {
    private fun pdf(rotations: List<Int>): ByteArray {
        val doc = PDDocument()
        rotations.forEach { r -> doc.addPage(PDPage(PDRectangle(200f, 100f)).apply { rotation = r }) }
        return ByteArrayOutputStream().also { doc.save(it); doc.close() }.toByteArray()
    }

    private val draft = Draft(listOf(
        Highlight(1, 0, Box(0.1f, 0.2f, 0.5f, 0.3f), 0xFFE066),
        Underline(2, 0, 0.1f, 0.6f, 0.8f, 0x1D4ED8),
        Ink(3, 0, listOf(Pt(0.1f, 0.5f), Pt(0.4f, 0.6f), Pt(0.7f, 0.5f)), 0xD62828, 0.005f),
        Ink(4, 0, listOf(Pt(0.5f, 0.5f)), 0x111111, 0.01f),
        Note(5, 1, Pt(0.7f, 0.1f), "check مرحبا"),
    ))

    private fun edited(rotations: List<Int>): PDDocument {
        val doc = PDDocument.load(ByteArrayInputStream(pdf(rotations)))
        applyDraft(doc, draft)
        val bytes = ByteArrayOutputStream().also { doc.save(it); doc.close() }.toByteArray()
        return PDDocument.load(ByteArrayInputStream(bytes))
    }

    @Test fun `marks are painted into the page and the page count is kept`() {
        val doc = edited(listOf(0, 0))
        assertEquals(2, doc.numberOfPages)
        val content = doc.getPage(0).contents.readBytes().toString(Charsets.ISO_8859_1)
        assertTrue("fills the highlight", content.contains(" re") && content.contains("\nf"))
        assertTrue("strokes the underline and drawings", content.split("\nS").size - 1 >= 3)
        doc.close()
    }

    @Test fun `a note becomes a comment that keeps its text, even in another script`() {
        val doc = edited(listOf(0, 90))
        val notes = doc.getPage(1).annotations.filterIsInstance<PDAnnotationText>()
        assertEquals(1, notes.size)
        assertEquals("check مرحبا", notes[0].contents)
        assertTrue("the comment sits inside the page box", notes[0].rectangle.lowerLeftX >= 0f && notes[0].rectangle.upperRightX <= 200f)
        assertTrue(doc.getPage(0).annotations.isEmpty())
        doc.close()
    }

    @Test fun `a rotated page puts the note where it is shown`() {
        // On a page turned a quarter clockwise, the shown top-right (u=1, v=0) is the box's top-left.
        val doc = PDDocument.load(ByteArrayInputStream(pdf(listOf(90))))
        applyDraft(doc, Draft(listOf(Note(1, 0, Pt(1f, 0f), "x"))))
        val r = doc.getPage(0).annotations.filterIsInstance<PDAnnotationText>().single().rectangle
        assertTrue("near the left edge", r.lowerLeftX < 20f)
        assertTrue("near the top edge", r.upperRightY > 80f)
        doc.close()
    }

    @Test fun `marks for a page that is not there are ignored`() {
        val doc = PDDocument.load(ByteArrayInputStream(pdf(listOf(0))))
        applyDraft(doc, Draft(listOf(Highlight(1, 5, Box(0f, 0f, 1f, 1f), 0))))
        assertEquals(1, doc.numberOfPages)
        doc.close()
    }
}
