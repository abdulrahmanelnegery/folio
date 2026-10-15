package dev.manhal.reader.edits

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Edits to a PDF kept as a draft, apart from the file. Every position is a fraction of the page as it is
 * shown (0..1 from the left and from the top), so a draft does not depend on zoom, cropping or screen size.
 */
@Serializable
data class Pt(val x: Float, val y: Float)

@Serializable
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(x: Float, y: Float, slack: Float = 0f) = x in (left - slack)..(right + slack) && y in (top - slack)..(bottom + slack)

    companion object {
        /** The box between two corners, whichever way the finger went. */
        fun between(a: Pt, b: Pt) = Box(minOf(a.x, b.x).coerceIn(0f, 1f), minOf(a.y, b.y).coerceIn(0f, 1f), maxOf(a.x, b.x).coerceIn(0f, 1f), maxOf(a.y, b.y).coerceIn(0f, 1f))
    }
}

@Serializable
sealed interface Mark {
    val id: Long
    val page: Int
}

/** A coloured band over part of a page; the colour is 0xRRGGBB. */
@Serializable
data class Highlight(override val id: Long, override val page: Int, val box: Box, val color: Int) : Mark

/** A line under text, from [x1] to [x2] at height [y]. */
@Serializable
data class Underline(override val id: Long, override val page: Int, val x1: Float, val x2: Float, val y: Float, val color: Int) : Mark

/** A freehand stroke; [width] is a fraction of the page width. */
@Serializable
data class Ink(override val id: Long, override val page: Int, val points: List<Pt>, val color: Int, val width: Float) : Mark

/** A sticky note: an icon at [at] that carries [text]. */
@Serializable
data class Note(override val id: Long, override val page: Int, val at: Pt, val text: String) : Mark

@Serializable
data class Draft(val marks: List<Mark> = emptyList()) {
    val isEmpty: Boolean get() = marks.isEmpty()
    fun onPage(page: Int) = marks.filter { it.page == page }

    fun toJson(): String = codec.encodeToString(serializer(), this)

    companion object {
        private val codec = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        /** A damaged or empty file reads as no draft rather than failing. */
        fun fromJson(text: String?): Draft = runCatching { codec.decodeFromString(serializer(), text.orEmpty()) }.getOrDefault(Draft())
    }
}

/** The page's own edits with undo and redo; each step adds or removes one mark. */
class EditHistory(start: Draft = Draft()) {
    var draft: Draft = start
        private set
    private val undone = ArrayDeque<Step>()
    private val done = ArrayDeque<Step>()

    private sealed interface Step { val mark: Mark }
    private data class Added(override val mark: Mark) : Step
    private data class Removed(override val mark: Mark, val index: Int) : Step

    val canUndo get() = done.isNotEmpty()
    val canRedo get() = undone.isNotEmpty()

    fun add(mark: Mark) {
        draft = Draft(draft.marks + mark)
        done.addLast(Added(mark)); undone.clear()
    }

    fun remove(id: Long) {
        val i = draft.marks.indexOfFirst { it.id == id }
        if (i < 0) return
        val mark = draft.marks[i]
        draft = Draft(draft.marks.filterIndexed { j, _ -> j != i })
        done.addLast(Removed(mark, i)); undone.clear()
    }

    fun undo(): Boolean {
        val step = done.removeLastOrNull() ?: return false
        draft = when (step) {
            is Added -> Draft(draft.marks.filter { it.id != step.mark.id })
            is Removed -> Draft(draft.marks.toMutableList().also { it.add(step.index.coerceAtMost(it.size), step.mark) })
        }
        undone.addLast(step)
        return true
    }

    fun redo(): Boolean {
        val step = undone.removeLastOrNull() ?: return false
        draft = when (step) {
            is Added -> Draft(draft.marks + step.mark)
            is Removed -> Draft(draft.marks.filter { it.id != step.mark.id })
        }
        done.addLast(step)
        return true
    }

    fun clear() { draft = Draft(); done.clear(); undone.clear() }

    /** The id for the next mark: later than every one used so far (even erased ones), so ids never repeat. */
    fun nextId(): Long = (++lastId)

    private var lastId = start.marks.maxOfOrNull { it.id } ?: 0L
}

/**
 * The mark under a touch, nearest the top (last drawn) first; [slack] is the reach of a finger as a fraction of the
 * page width. Used by the eraser.
 */
fun Draft.hit(page: Int, x: Float, y: Float, slack: Float, aspect: Float): Mark? = onPage(page).asReversed().firstOrNull { m ->
    when (m) {
        is Highlight -> m.box.contains(x, y, slack / 2)
        is Underline -> abs(y - m.y) * aspect <= slack && x in (minOf(m.x1, m.x2) - slack)..(maxOf(m.x1, m.x2) + slack)
        is Ink -> m.points.zipWithNext().any { (a, b) -> distance(x, y * aspect, a.x, a.y * aspect, b.x, b.y * aspect) <= slack + m.width / 2 } ||
            (m.points.size == 1 && hypot(x - m.points[0].x, (y - m.points[0].y) * aspect) <= slack)
        is Note -> hypot(x - m.at.x, (y - m.at.y) * aspect) <= slack * 1.6f
    }
}

private fun distance(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = bx - ax; val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
    return hypot(px - (ax + t * dx), py - (ay + t * dy))
}

/**
 * Maps between the page's fractions and the part of it on screen. When margins are cropped only the middle of the
 * page shows; [l], [t] are the cropped fractions and [visW], [visH] what is left.
 */
class PageMapper(val l: Float = 0f, val t: Float = 0f, val visW: Float = 1f, val visH: Float = 1f) {
    fun screenX(x: Float, width: Float) = (x - l) / visW * width
    fun screenY(y: Float, height: Float) = (y - t) / visH * height
    fun pageX(px: Float, width: Float) = (px / width * visW + l).coerceIn(0f, 1f)
    fun pageY(py: Float, height: Float) = (py / height * visH + t).coerceIn(0f, 1f)
}

/**
 * Turns a fraction of the shown page (from the left, from the top) into PDF user space, which counts up from the
 * bottom-left of the page box and ignores the page's rotation. [w] and [h] are the box size, [rotation] the page's
 * /Rotate (0, 90, 180 or 270, clockwise). The result is the point inside the box.
 */
fun toPdfSpace(u: Float, v: Float, w: Float, h: Float, rotation: Int): Pair<Float, Float> = when (((rotation % 360) + 360) % 360) {
    90 -> (v * w) to (u * h)
    180 -> ((1f - u) * w) to (v * h)
    270 -> ((1f - v) * w) to ((1f - u) * h)
    else -> (u * w) to ((1f - v) * h)
}

/** The page's shape as shown: a quarter turn swaps its width and height. */
fun shownAspect(w: Float, h: Float, rotation: Int): Float = if ((((rotation % 360) + 360) % 360) % 180 == 90) w / h else h / w
