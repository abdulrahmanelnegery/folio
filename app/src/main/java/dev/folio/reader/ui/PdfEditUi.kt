package dev.folio.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.folio.reader.edits.Box as PageBox
import dev.folio.reader.edits.Draft
import dev.folio.reader.edits.EditSession
import dev.folio.reader.edits.Highlight
import dev.folio.reader.edits.hit
import dev.folio.reader.edits.Ink
import dev.folio.reader.edits.Mark
import dev.folio.reader.edits.Note
import dev.folio.reader.edits.PageMapper
import dev.folio.reader.edits.Pt
import dev.folio.reader.edits.Underline

/** What one finger does on a page while editing. [Hand] leaves the page free to scroll. */
enum class EditTool { Hand, Highlight, Pen, Underline, Note, Eraser }

val HighlightColors = listOf(0xFFE066, 0x7BE495, 0xFF8FB1, 0x8EC5FF)
val PenColors = listOf(0xD62828, 0x1D4ED8, 0x111111, 0x2E7D32)

/** Pen widths as a fraction of the page width. */
val PenWidths = listOf(0.0025f, 0.005f, 0.010f)

private fun Int.rgb() = Color(0xFF000000.toInt() or this)

/** Draws the marks of one page over the page image; [live] is the one being drawn right now. */
fun DrawScope.drawMarks(marks: List<Mark>, mapper: PageMapper, live: Mark?) {
    val w = size.width
    val h = size.height
    val unit = w / mapper.visW
    fun px(p: Pt) = Offset(mapper.screenX(p.x, w), mapper.screenY(p.y, h))
    for (m in marks + listOfNotNull(live)) when (m) {
        is Highlight -> {
            val a = px(Pt(m.box.left, m.box.top)); val b = px(Pt(m.box.right, m.box.bottom))
            drawRect(m.color.rgb().copy(alpha = 0.45f), a, Size(b.x - a.x, b.y - a.y))
        }
        is Underline -> {
            val a = px(Pt(m.x1, m.y)); val b = px(Pt(m.x2, m.y))
            drawLine(m.color.rgb(), a, b, strokeWidth = unit * 0.0035f, cap = StrokeCap.Round)
        }
        is Ink -> {
            val width = m.width * unit
            if (m.points.size == 1) drawCircle(m.color.rgb(), width / 2, px(m.points[0]))
            else {
                val path = Path()
                m.points.forEachIndexed { i, p -> val o = px(p); if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y) }
                drawPath(path, m.color.rgb(), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        is Note -> {
            val c = px(m.at)
            val s = unit * 0.045f
            drawRoundRect(Color(0xFFF2B12E), Offset(c.x - s / 2, c.y - s / 2), Size(s, s), androidx.compose.ui.geometry.CornerRadius(s * 0.2f))
            for (i in 0..2) drawLine(Color(0xFF3B2A00), Offset(c.x - s * 0.28f, c.y - s * 0.2f + i * s * 0.2f), Offset(c.x + s * 0.28f, c.y - s * 0.2f + i * s * 0.2f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        }
    }
}

/**
 * Turns a finger on one page into marks for the tool in use. Positions are fractions of the page, so what is drawn
 * does not depend on the zoom. [onLive] shows the mark being drawn; [onAdd] keeps it when the finger lifts.
 */
@Composable
fun Modifier.markInput(
    tool: EditTool, color: Int, width: Float, page: Int, aspect: Float, mapper: PageMapper, session: EditSession,
    onLive: (Mark?) -> Unit, onNote: (Pt, Note?) -> Unit,
): Modifier {
    if (tool == EditTool.Hand) return this
    val liveCb by rememberUpdatedState(onLive)
    val noteCb by rememberUpdatedState(onNote)
    val slack = 0.03f
    val key = listOf(tool, color, width, page, mapper.l, mapper.t, mapper.visW, mapper.visH)
    return when (tool) {
        EditTool.Note -> pointerInput(key) {
            detectTapGestures { pos ->
                val x = mapper.pageX(pos.x, size.width.toFloat()); val y = mapper.pageY(pos.y, size.height.toFloat())
                noteCb(Pt(x, y), session.draft().hit(page, x, y, slack, aspect) as? Note)
            }
        }
        EditTool.Eraser -> this
            .pointerInput(key) {
                detectTapGestures { pos ->
                    val x = mapper.pageX(pos.x, size.width.toFloat()); val y = mapper.pageY(pos.y, size.height.toFloat())
                    session.draft().hit(page, x, y, slack, aspect)?.let { session.remove(it.id) }
                }
            }
            .pointerInput(key) {
                detectDragGestures { change, _ ->
                    val x = mapper.pageX(change.position.x, size.width.toFloat()); val y = mapper.pageY(change.position.y, size.height.toFloat())
                    session.draft().hit(page, x, y, slack, aspect)?.let { session.remove(it.id) }
                    change.consume()
                }
            }
        else -> pointerInput(key) {
            var start = Pt(0f, 0f)
            var end = start
            val points = ArrayList<Pt>()
            fun at(o: Offset) = Pt(mapper.pageX(o.x, size.width.toFloat()), mapper.pageY(o.y, size.height.toFloat()))
            fun live(): Mark? = when (tool) {
                EditTool.Highlight -> Highlight(0, page, PageBox.between(start, end), color)
                EditTool.Underline -> Underline(0, page, minOf(start.x, end.x), maxOf(start.x, end.x), start.y, color)
                else -> Ink(0, page, points.toList(), color, width)
            }
            detectDragGestures(
                onDragStart = { o -> start = at(o); end = start; points.clear(); points += start; liveCb(live()) },
                onDrag = { change, _ ->
                    end = at(change.position)
                    if (tool == EditTool.Pen) points += end
                    liveCb(live()); change.consume()
                },
                onDragEnd = { liveCb(null); keep(session, tool, page, start, end, points, color, width) },
                onDragCancel = { liveCb(null) },
            )
        }
    }
}

/** Keeps a finished stroke as a mark, unless it is too small to be meant. */
private fun keep(session: EditSession, tool: EditTool, page: Int, start: Pt, end: Pt, points: List<Pt>, color: Int, width: Float) {
    when (tool) {
        EditTool.Highlight -> PageBox.between(start, end).let { b ->
            if (b.right - b.left > 0.01f && b.bottom - b.top > 0.004f) session.add(Highlight(session.nextId(), page, b, color))
        }
        EditTool.Underline -> if (kotlin.math.abs(end.x - start.x) > 0.01f) session.add(Underline(session.nextId(), page, minOf(start.x, end.x), maxOf(start.x, end.x), start.y, color))
        EditTool.Pen -> if (points.size > 1) session.add(Ink(session.nextId(), page, thin(points), color, width))
        else -> {}
    }
}

/** Drops points that sit almost on top of the one before, so a long stroke stays small in the draft. */
fun thin(points: List<Pt>, gap: Float = 0.0015f): List<Pt> {
    if (points.size < 3) return points
    val out = ArrayList<Pt>()
    out += points.first()
    for (i in 1 until points.size - 1) {
        val last = out.last()
        if (kotlin.math.abs(points[i].x - last.x) + kotlin.math.abs(points[i].y - last.y) >= gap) out += points[i]
    }
    out += points.last()
    return out
}

/** The tools while editing: what a finger does, undo and redo, and the colour and width of the current tool. */
@Composable
fun EditToolbar(
    tool: EditTool, onTool: (EditTool) -> Unit,
    color: Int, onColor: (Int) -> Unit, width: Float, onWidth: (Float) -> Unit,
    canUndo: Boolean, canRedo: Boolean, onUndo: () -> Unit, onRedo: () -> Unit, onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp, shadowElevation = 8.dp) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (tool == EditTool.Highlight || tool == EditTool.Pen || tool == EditTool.Underline) {
                Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val palette = if (tool == EditTool.Highlight) HighlightColors else PenColors
                    palette.forEach { c ->
                        Box(
                            Modifier.size(28.dp).background(c.rgb(), CircleShape)
                                .border(if (c == color) 3.dp else 1.dp, if (c == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onColor(c) },
                        )
                    }
                    if (tool == EditTool.Pen) {
                        Text("|", color = MaterialTheme.colorScheme.outline)
                        PenWidths.forEachIndexed { i, wv ->
                            Box(Modifier.size(30.dp).clickable { onWidth(wv) }.then(if (wv == width) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                                Box(Modifier.size((4 + i * 4).dp).background(MaterialTheme.colorScheme.onSurface, CircleShape))
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToolButton(EditTool.Hand, tool, onTool, Icons.Rounded.PanTool, "Move and scroll")
                ToolButton(EditTool.Highlight, tool, onTool, Icons.Rounded.Highlight, "Highlight")
                ToolButton(EditTool.Pen, tool, onTool, Icons.Rounded.Create, "Pen")
                ToolButton(EditTool.Underline, tool, onTool, Icons.Rounded.FormatUnderlined, "Underline")
                ToolButton(EditTool.Note, tool, onTool, Icons.Rounded.StickyNote2, "Note")
                ToolButton(EditTool.Eraser, tool, onTool, Icons.Rounded.AutoFixNormal, "Eraser")
                IconButton(onClick = onUndo, enabled = canUndo) { Icon(Icons.AutoMirrored.Rounded.Undo, "Undo") }
                IconButton(onClick = onRedo, enabled = canRedo) { Icon(Icons.AutoMirrored.Rounded.Redo, "Redo") }
                IconButton(onClick = onDone) { Icon(Icons.Rounded.Check, "Done editing", tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun ToolButton(me: EditTool, current: EditTool, onTool: (EditTool) -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    IconButton(
        onClick = { onTool(me) },
        modifier = if (me == current) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier,
    ) { Icon(icon, label, tint = if (me == current) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface) }
}
