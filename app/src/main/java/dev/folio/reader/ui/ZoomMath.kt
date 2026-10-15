package dev.folio.reader.ui

/**
 * The arithmetic of the reader's zoom, lifted out of ReaderScreen unchanged so it can be tested. The pages are laid out
 * [zoom] times wider than the screen and shifted by `offX` (0 down to `viewW * (1 - zoom)`); a pinch or double tap
 * scales the view for the moment and is then "settled" into the layout.
 */
internal object ZoomMath {
    const val MAX_ZOOM = 5f
    const val DOUBLE_TAP_ZOOM = 2.5f

    /** The layout after a settle: the new zoom, the new sideways offset and the ratio of new to old zoom. */
    data class Settled(val zoom: Float, val offX: Float, val ratio: Float)

    /** Settles a moment's [scale] about the point (ox) keeping it under (cx), as ReaderScreen's `settle` does for the horizontal part. */
    fun settle(zoom: Float, offX: Float, scale: Float, ox: Float, cx: Float, viewW: Float): Settled {
        val newZoom = (zoom * scale).coerceIn(1f, MAX_ZOOM)
        val r = newZoom / zoom
        val newOff = (cx - (ox - offX) * r).coerceIn(viewW * (1f - newZoom), 0f)
        return Settled(newZoom, newOff, r)
    }

    /** How far the list must scroll after a settle, from the scroll offset [inside] the first visible page, so the point at (oy) stays under (cy). */
    fun scrollAfterSettle(inside: Int, oy: Float, ratio: Float, cy: Float): Float = (inside + oy) * ratio - cy

    /** A double tap zooms in unless already zoomed in, in which case it goes back to 1. */
    fun doubleTapTarget(zoom: Float): Float = if (zoom > 1.05f) 1f else DOUBLE_TAP_ZOOM

    /** The live scale during a pinch: never below 1 or above the maximum once settled. */
    fun liveScale(live: Float, change: Float, zoom: Float): Float = (live * change).coerceIn(1f / zoom, MAX_ZOOM / zoom)

    /** A one finger slide on a zoomed page, kept inside the page. */
    fun pan(offX: Float, dx: Float, zoom: Float, viewW: Float): Float = (offX + dx).coerceIn(viewW * (1f - zoom), 0f)

    /** Where inside a page the reader is, as a fraction of its height at any zoom. */
    fun fractionInPage(scrollPx: Int, widthPx: Int, zoom: Float, aspect: Float): Float =
        (scrollPx / (widthPx * zoom * aspect).coerceAtLeast(1f)).coerceIn(0f, 1f)
}
