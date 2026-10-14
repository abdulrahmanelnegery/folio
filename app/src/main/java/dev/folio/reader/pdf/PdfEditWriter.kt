package dev.folio.reader.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.pdmodel.graphics.blend.BlendMode
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationText
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.pdmodel.PDResources
import dev.folio.reader.edits.Draft
import dev.folio.reader.edits.Highlight
import dev.folio.reader.edits.Ink
import dev.folio.reader.edits.Note
import dev.folio.reader.edits.PdfWork
import dev.folio.reader.edits.Underline
import dev.folio.reader.edits.toPdfSpace
import java.io.File

/**
 * Applies a [Draft] to a copy of a PDF. Highlights, underlines and drawings are painted into the page itself, so every
 * reader shows them; a note is a real PDF comment (with its text) that also has its icon drawn, so readers that skip
 * comments still show where it is. Reading and drawing pages stays with Android's own renderer.
 */
class PdfEditWriter(context: Context, private val draft: Draft) : PdfWork {
    init { PDFBoxResourceLoader.init(context.applicationContext) }

    override fun write(source: File, out: File) {
        // Big files are paged through a temporary file instead of being held in memory.
        PDDocument.load(source, MemoryUsageSetting.setupTempFileOnly()).use { doc ->
            applyDraft(doc, draft)
            doc.save(out)
        }
    }

    override fun pageCount(file: File): Int? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r -> r.pageCount.also { if (it > 0) r.openPage(0).close() } }
        }
    }.getOrNull()
}

/** Puts a draft's marks into an open document (no Android needed, so it can be tested on its own). */
fun applyDraft(doc: PDDocument, draft: Draft) {
    PdfMarkPainter(doc).apply(draft)
}

private class PdfMarkPainter(private val doc: PDDocument) {
    fun apply(draft: Draft) {
        if (doc.isEncrypted && !doc.currentAccessPermission.canModify() && !doc.currentAccessPermission.canModifyAnnotations()) {
            throw IllegalStateException("This PDF is protected against changes, so it cannot be edited.")
        }
        for ((index, marks) in draft.marks.groupBy { it.page }) {
            if (index !in 0 until doc.numberOfPages) continue
            paint(doc, doc.getPage(index), marks)
        }
    }

    private fun paint(doc: PDDocument, page: PDPage, marks: List<dev.folio.reader.edits.Mark>) {
        val box = page.cropBox ?: page.mediaBox
        val w = box.width; val h = box.height
        val rotation = page.rotation
        val shownW = if ((((rotation % 360) + 360) % 360) % 180 == 90) h else w
        fun at(x: Float, y: Float) = toPdfSpace(x, y, w, h, rotation).let { (px, py) -> (box.lowerLeftX + px) to (box.lowerLeftY + py) }

        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            for (m in marks) when (m) {
                is Highlight -> {
                    val (x1, y1) = at(m.box.left, m.box.top); val (x2, y2) = at(m.box.right, m.box.bottom)
                    cs.saveGraphicsState()
                    cs.setGraphicsStateParameters(PDExtendedGraphicsState().apply { nonStrokingAlphaConstant = 0.45f; blendMode = BlendMode.MULTIPLY })
                    cs.setNonStrokingColor((m.color shr 16) and 0xFF, (m.color shr 8) and 0xFF, m.color and 0xFF)
                    cs.addRect(minOf(x1, x2), minOf(y1, y2), kotlin.math.abs(x2 - x1), kotlin.math.abs(y2 - y1))
                    cs.fill()
                    cs.restoreGraphicsState()
                }
                is Underline -> {
                    val (x1, y1) = at(m.x1, m.y); val (x2, y2) = at(m.x2, m.y)
                    stroke(cs, m.color, shownW * 0.0035f) { cs.moveTo(x1, y1); cs.lineTo(x2, y2) }
                }
                is Ink -> stroke(cs, m.color, (m.width * shownW).coerceAtLeast(0.3f)) {
                    m.points.forEachIndexed { i, p ->
                        val (x, y) = at(p.x, p.y)
                        if (i == 0) { cs.moveTo(x, y); if (m.points.size == 1) cs.lineTo(x, y) } else cs.lineTo(x, y)
                    }
                }
                is Note -> {
                    val size = shownW * 0.045f
                    val (cx, cy) = at(m.at.x, m.at.y)
                    drawNoteIcon(cs, cx - size / 2, cy - size / 2, size)
                }
            }
        }
        // The notes' text: a real comment under each icon.
        val notes = marks.filterIsInstance<Note>()
        if (notes.isNotEmpty()) {
            val annotations = page.annotations
            for (m in notes) {
                val size = shownW * 0.045f
                val (cx, cy) = at(m.at.x, m.at.y)
                val rect = PDRectangle(cx - size / 2, cy - size / 2, size, size)
                val annotation = PDAnnotationText().apply {
                    rectangle = rect
                    contents = m.text
                    annotationName = "rukn-note-${m.id}"
                    color = PDColor(floatArrayOf(0.95f, 0.69f, 0.18f), PDDeviceRGB.INSTANCE)
                    setNoZoom(true); setNoRotate(true)
                }
                val form = PDAppearanceStream(doc).apply { bBox = PDRectangle(size, size); resources = PDResources() }
                PDPageContentStream(doc, form, form.contentStream.createOutputStream(com.tom_roush.pdfbox.cos.COSName.FLATE_DECODE)).use { drawNoteIcon(it, 0f, 0f, size) }
                annotation.appearance = PDAppearanceDictionary().apply { normalAppearance = com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceEntry(form.cosObject) }
                annotations.add(annotation)
            }
            page.annotations = annotations
        }
    }

    private fun stroke(cs: PDPageContentStream, color: Int, width: Float, path: () -> Unit) {
        cs.saveGraphicsState()
        cs.setStrokingColor((color shr 16) and 0xFF, (color shr 8) and 0xFF, color and 0xFF)
        cs.setLineWidth(width)
        cs.setLineCapStyle(1); cs.setLineJoinStyle(1)
        path()
        cs.stroke()
        cs.restoreGraphicsState()
    }

    private fun drawNoteIcon(cs: PDPageContentStream, x: Float, y: Float, size: Float) {
        cs.saveGraphicsState()
        cs.setNonStrokingColor(0xF2, 0xB1, 0x2E)
        cs.addRect(x, y, size, size); cs.fill()
        cs.setStrokingColor(0x3B, 0x2A, 0x00)
        cs.setLineWidth(size * 0.07f); cs.setLineCapStyle(1)
        for (i in 0..2) { val yy = y + size * (0.7f - i * 0.2f); cs.moveTo(x + size * 0.22f, yy); cs.lineTo(x + size * 0.78f, yy); cs.stroke() }
        cs.restoreGraphicsState()
    }
}
