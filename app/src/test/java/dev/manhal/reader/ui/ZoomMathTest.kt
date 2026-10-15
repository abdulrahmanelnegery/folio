package dev.manhal.reader.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomMathTest {
    private val viewW = 1000f
    private val eps = 0.001f

    @Test fun `zoom never goes below 1 or above the maximum`() {
        assertEquals(1f, ZoomMath.settle(1f, 0f, 0.2f, 500f, 500f, viewW).zoom, eps)
        assertEquals(ZoomMath.MAX_ZOOM, ZoomMath.settle(4f, -3000f, 3f, 500f, 500f, viewW).zoom, eps)
    }

    @Test fun `the point under the fingers stays put when zooming in`() {
        // At zoom 1, offset 0, a pinch about x = 300 to 2.5x: the content point under x = 300 is still under x = 300.
        val s = ZoomMath.settle(1f, 0f, 2.5f, 300f, 300f, viewW)
        val contentBefore = (300f - 0f) / 1f
        val contentAfter = (300f - s.offX) / s.zoom
        assertEquals(contentBefore, contentAfter, eps)
    }

    @Test fun `the offset keeps the page edges at or beyond the screen edges`() {
        for (focus in listOf(0f, 250f, 500f, 750f, 1000f)) {
            val s = ZoomMath.settle(1f, 0f, 3f, focus, focus, viewW)
            assertTrue("left edge", s.offX <= 0f)
            assertTrue("right edge", s.offX + viewW * s.zoom >= viewW - eps)
        }
    }

    @Test fun `zooming back out to 1 puts the page back at offset 0`() {
        val s = ZoomMath.settle(2.5f, -400f, 1f / 2.5f, 500f, 500f, viewW)
        assertEquals(1f, s.zoom, eps)
        assertEquals(0f, s.offX, eps)
    }

    @Test fun `settling with no change keeps everything`() {
        val s = ZoomMath.settle(2f, -300f, 1f, 400f, 400f, viewW)
        assertEquals(2f, s.zoom, eps)
        assertEquals(-300f, s.offX, eps)
        assertEquals(1f, s.ratio, eps)
    }

    @Test fun `after a settle the vertical scroll keeps the touched point in place`() {
        // A point oy = 200 px below the top of the first visible page, 100 px into it, shown at cy = 200 on screen.
        val before = 100f + 200f
        val r = 2f
        val scroll = ZoomMath.scrollAfterSettle(100, 200f, r, 200f)
        // The page started at scrolled-in 100 and now is r times taller; scrolling by [scroll] brings the point back under cy.
        assertEquals(before * r - 200f, scroll, eps)
    }

    @Test fun `a double tap zooms in from 1 and back out from any zoom`() {
        assertEquals(ZoomMath.DOUBLE_TAP_ZOOM, ZoomMath.doubleTapTarget(1f), eps)
        assertEquals(ZoomMath.DOUBLE_TAP_ZOOM, ZoomMath.doubleTapTarget(1.04f), eps)
        assertEquals(1f, ZoomMath.doubleTapTarget(1.06f), eps)
        assertEquals(1f, ZoomMath.doubleTapTarget(ZoomMath.MAX_ZOOM), eps)
    }

    @Test fun `a pinch cannot take the settled zoom outside 1 to the maximum`() {
        assertEquals(1f / 2f, ZoomMath.liveScale(1f, 0.01f, 2f), eps)
        assertEquals(ZoomMath.MAX_ZOOM / 2f, ZoomMath.liveScale(1f, 100f, 2f), eps)
        assertEquals(1.2f, ZoomMath.liveScale(1f, 1.2f, 2f), eps)
    }

    @Test fun `a one finger slide stays inside the page`() {
        assertEquals(0f, ZoomMath.pan(-10f, 500f, 2f, viewW), eps)
        assertEquals(-1000f, ZoomMath.pan(-990f, -500f, 2f, viewW), eps)
        assertEquals(-60f, ZoomMath.pan(-10f, -50f, 2f, viewW), eps)
    }

    @Test fun `the saved place inside a page does not depend on the zoom`() {
        val atOne = ZoomMath.fractionInPage(400, 1000, 1f, 1.4f)
        val atThree = ZoomMath.fractionInPage(1200, 1000, 3f, 1.4f)
        assertEquals(atOne, atThree, eps)
        assertEquals(1f, ZoomMath.fractionInPage(99999, 1000, 1f, 1.4f), eps)
        assertEquals(0f, ZoomMath.fractionInPage(0, 0, 1f, 0f), eps)
    }
}
