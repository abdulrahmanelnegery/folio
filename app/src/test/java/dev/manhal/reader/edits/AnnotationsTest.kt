package dev.manhal.reader.edits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationsTest {
    private val box = Highlight(1, 0, Box(0.1f, 0.2f, 0.5f, 0.3f), 0xFFE066)
    private val ink = Ink(2, 0, listOf(Pt(0.1f, 0.5f), Pt(0.4f, 0.5f)), 0xD62828, 0.004f)
    private val note = Note(3, 1, Pt(0.7f, 0.1f), "check this مرحبا")
    private val under = Underline(4, 0, 0.2f, 0.6f, 0.8f, 0x1D4ED8)

    @Test fun `a draft survives writing and reading`() {
        val d = Draft(listOf(box, ink, note, under))
        assertEquals(d, Draft.fromJson(d.toJson()))
    }

    @Test fun `a damaged draft reads as empty`() {
        assertTrue(Draft.fromJson("{not json").isEmpty)
        assertTrue(Draft.fromJson(null).isEmpty)
        assertTrue(Draft.fromJson("").isEmpty)
    }

    @Test fun `a box between two corners is the same whichever way it was dragged`() {
        assertEquals(Box.between(Pt(0.5f, 0.6f), Pt(0.1f, 0.2f)), Box.between(Pt(0.1f, 0.2f), Pt(0.5f, 0.6f)))
        assertEquals(Box(0f, 0f, 1f, 1f), Box.between(Pt(-1f, -1f), Pt(2f, 2f)))
    }

    @Test fun `undo and redo step through adds and removals`() {
        val h = EditHistory()
        h.add(box); h.add(ink)
        assertEquals(2, h.draft.marks.size)
        h.remove(1)
        assertEquals(listOf<Mark>(ink), h.draft.marks)
        assertTrue(h.undo())
        assertEquals(listOf<Mark>(box, ink), h.draft.marks)
        assertTrue(h.undo()); assertTrue(h.undo())
        assertTrue(h.draft.isEmpty)
        assertFalse(h.undo())
        assertTrue(h.redo()); assertTrue(h.redo()); assertTrue(h.redo())
        assertEquals(listOf<Mark>(ink), h.draft.marks)
        assertFalse(h.redo())
    }

    @Test fun `a new edit forgets what could have been redone`() {
        val h = EditHistory()
        h.add(box); h.undo()
        assertTrue(h.canRedo)
        h.add(ink)
        assertFalse(h.canRedo)
    }

    @Test fun `removing a mark that is not there changes nothing`() {
        val h = EditHistory(Draft(listOf(box)))
        h.remove(99)
        assertFalse(h.canUndo)
        assertEquals(1, h.draft.marks.size)
    }

    @Test fun `ids keep growing even after a mark is erased`() {
        val h = EditHistory(Draft(listOf(box, ink)))
        val a = h.nextId(); h.remove(2); val b = h.nextId()
        assertTrue(a > 2); assertTrue(b > a)
    }

    @Test fun `the eraser finds the mark under the finger and the top one first`() {
        val over = Highlight(9, 0, Box(0.1f, 0.2f, 0.5f, 0.3f), 0)
        val d = Draft(listOf(box, over, ink, under, note))
        assertEquals(9L, d.hit(0, 0.3f, 0.25f, 0.03f, 1.4f)?.id)
        assertEquals(2L, d.hit(0, 0.25f, 0.5f, 0.03f, 1.4f)?.id)
        assertEquals(4L, d.hit(0, 0.4f, 0.8f, 0.03f, 1.4f)?.id)
        assertEquals(3L, d.hit(1, 0.7f, 0.1f, 0.03f, 1.4f)?.id)
        assertNull(d.hit(0, 0.9f, 0.9f, 0.03f, 1.4f))
        assertNull(d.hit(2, 0.3f, 0.25f, 0.03f, 1.4f))
        assertNotNull(d.hit(0, 0.1f, 0.2f, 0.03f, 1.4f))
    }

    @Test fun `screen and page positions map both ways with and without cropped margins`() {
        val plain = PageMapper()
        assertEquals(50f, plain.screenX(0.5f, 100f), 0.001f)
        assertEquals(0.25f, plain.pageY(50f, 200f), 0.001f)
        val cropped = PageMapper(l = 0.1f, t = 0.2f, visW = 0.8f, visH = 0.6f)
        assertEquals(0f, cropped.screenX(0.1f, 400f), 0.001f)
        assertEquals(400f, cropped.screenX(0.9f, 400f), 0.001f)
        assertEquals(0.5f, cropped.pageX(cropped.screenX(0.5f, 321f), 321f), 0.001f)
        assertEquals(0.2f, cropped.pageY(0f, 300f), 0.001f)
    }

    @Test fun `positions turn into PDF space for every page rotation`() {
        // A 200 wide, 100 tall page box. The top-left of what is shown is (0,0).
        fun at(u: Float, v: Float, r: Int) = toPdfSpace(u, v, 200f, 100f, r).let { it.first to it.second }
        assertEquals(0f to 100f, at(0f, 0f, 0))
        assertEquals(200f to 0f, at(1f, 1f, 0))
        // Turned a quarter clockwise, the shown top-left is the box's bottom-left, and shown top-right is its top-left.
        assertEquals(0f to 0f, at(0f, 0f, 90))
        assertEquals(0f to 100f, at(1f, 0f, 90))
        assertEquals(200f to 0f, at(0f, 1f, 90))
        assertEquals(200f to 0f, at(0f, 0f, 180))
        assertEquals(0f to 100f, at(1f, 1f, 180))
        assertEquals(200f to 100f, at(0f, 0f, 270))
        assertEquals(0f to 0f, at(1f, 1f, 270))
        assertEquals(at(0.3f, 0.4f, 0), at(0.3f, 0.4f, 360))
        assertEquals(at(0.3f, 0.4f, 270), at(0.3f, 0.4f, -90))
    }

    @Test fun `a quarter turn swaps the shown shape`() {
        assertEquals(0.5f, shownAspect(200f, 100f, 0), 0.001f)
        assertEquals(2f, shownAspect(200f, 100f, 90), 0.001f)
        assertEquals(0.5f, shownAspect(200f, 100f, 180), 0.001f)
        assertEquals(2f, shownAspect(200f, 100f, 270), 0.001f)
    }

    @Test fun `a long stroke is thinned but keeps its ends and its shape`() {
        val line = (0..1000).map { Pt(it / 1000f, 0.5f) }
        val thin = dev.manhal.reader.ui.thin(line)
        assertTrue(thin.size < 700)
        assertEquals(line.first(), thin.first())
        assertEquals(line.last(), thin.last())
        assertEquals(line.take(2), dev.manhal.reader.ui.thin(line.take(2)))
    }
}
