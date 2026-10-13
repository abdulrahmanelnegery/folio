package dev.folio.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Code(val text: String) : Block
    data class Quote(val text: String) : Block
    data class Item(val marker: String, val text: String, val depth: Int) : Block
    data object Rule : Block
    data class Table(val rows: List<List<String>>) : Block
}

/** Splits Markdown into blocks: headings, paragraphs, lists, quotes, fenced code, tables and rules. */
private fun parse(text: String): List<Block> {
    val out = ArrayList<Block>()
    val lines = text.lines()
    var i = 0
    val paragraph = StringBuilder()
    fun flush() {
        if (paragraph.isNotEmpty()) { out += Block.Paragraph(paragraph.toString().trim()); paragraph.clear() }
    }
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") || trimmed.startsWith("~~~") -> {
                flush()
                val fence = trimmed.take(3)
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(fence)) { code.appendLine(lines[i]); i++ }
                out += Block.Code(code.toString().trimEnd())
            }
            trimmed.isEmpty() -> flush()
            Regex("^(-{3,}|\\*{3,}|_{3,})$").matches(trimmed) -> { flush(); out += Block.Rule }
            trimmed.startsWith("#") && Regex("^#{1,6}\\s").containsMatchIn(trimmed) -> {
                flush()
                val level = trimmed.takeWhile { it == '#' }.length
                out += Block.Heading(level, trimmed.drop(level).trim().trimEnd('#').trim())
            }
            trimmed.startsWith(">") -> {
                flush()
                val quote = StringBuilder(trimmed.removePrefix(">").trim())
                while (i + 1 < lines.size && lines[i + 1].trim().startsWith(">")) { i++; quote.append(' ').append(lines[i].trim().removePrefix(">").trim()) }
                out += Block.Quote(quote.toString())
            }
            Regex("^\\s*([-*+]|\\d+[.)])\\s+").containsMatchIn(line) -> {
                flush()
                val m = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$").find(line)!!
                val marker = if (m.groupValues[2][0].isDigit()) m.groupValues[2] else "•"
                out += Block.Item(marker, m.groupValues[3], m.groupValues[1].length / 2)
            }
            trimmed.startsWith("|") && i + 1 < lines.size && Regex("^\\|?[\\s:|-]+\\|[\\s:|-]*$").matches(lines[i + 1].trim()) -> {
                flush()
                val rows = ArrayList<List<String>>()
                rows += cells(trimmed)
                i += 2
                while (i < lines.size && lines[i].trim().startsWith("|")) { rows += cells(lines[i].trim()); i++ }
                i--
                out += Block.Table(rows)
            }
            else -> { if (paragraph.isNotEmpty()) paragraph.append(' '); paragraph.append(trimmed) }
        }
        i++
    }
    flush()
    return out
}

private fun cells(row: String) = row.trim().trim('|').split('|').map { it.trim() }

/** Bold, italic, code, strike-through and links inside a line. */
private fun inline(text: String, ink: Color, link: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
    val token = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|\\*(.+?)\\*|_(.+?)_|`([^`]+)`|~~(.+?)~~|!?\\[([^\\]]*)]\\(([^)\\s]*)[^)]*\\)")
    var last = 0
    for (m in token.findAll(text)) {
        append(text.substring(last, m.range.first))
        val g = m.groupValues
        when {
            g[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[1]) }
            g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(g[2]) }
            g[3].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[3]) }
            g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[4]) }
            g[5].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(g[5]) }
            g[6].isNotEmpty() -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(g[6]) }
            else -> {
                val label = g[7].ifEmpty { g[8] }
                pushStringAnnotation("url", g[8])
                withStyle(SpanStyle(color = link, textDecoration = TextDecoration.Underline)) { append(label) }
                pop()
            }
        }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

/** Renders Markdown in the reading colours; [fontSize] scales everything. */
@Composable
fun MarkdownView(text: String, ink: Color, paper: Color, fontSize: Float, listState: LazyListState, modifier: Modifier = Modifier) {
    val blocks = remember(text) { parse(text) }
    val link = if (paper.red + paper.green + paper.blue < 1.5f) Color(0xFF8AB4FF) else Color(0xFF1558B0)
    val codeBg = if (paper.red + paper.green + paper.blue < 1.5f) Color(0x33FFFFFF) else Color(0x14000000)
    val uri = LocalUriHandler.current
    LazyColumn(state = listState, modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(blocks.size) { index ->
            when (val b = blocks[index]) {
                is Block.Heading -> {
                    val size = fontSize * when (b.level) { 1 -> 1.9f; 2 -> 1.55f; 3 -> 1.3f; 4 -> 1.15f; else -> 1.05f }
                    Text(inline(b.text, ink, link, codeBg), color = ink, fontSize = size.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (b.level <= 2) 10.dp else 4.dp))
                }
                is Block.Paragraph -> Linked(inline(b.text, ink, link, codeBg), ink, fontSize, uri::openUri)
                is Block.Quote -> Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.width(4.dp).height(24.dp).background(link.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                    Box(Modifier.padding(start = 12.dp)) { Linked(inline(b.text, ink, link, codeBg), ink.copy(alpha = 0.85f), fontSize, uri::openUri, italic = true) }
                }
                is Block.Item -> Row(Modifier.padding(start = (b.depth * 18).dp)) {
                    Text(b.marker, color = ink, fontSize = fontSize.sp, modifier = Modifier.width(26.dp))
                    Linked(inline(b.text, ink, link, codeBg), ink, fontSize, uri::openUri)
                }
                is Block.Code -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(codeBg).horizontalScroll(rememberScrollState()).padding(12.dp)) {
                    Text(b.text, color = ink, fontFamily = FontFamily.Monospace, fontSize = (fontSize * 0.88f).sp, softWrap = false)
                }
                Block.Rule -> Box(Modifier.fillMaxWidth().height(1.dp).background(ink.copy(alpha = 0.25f)))
                is Block.Table -> Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    b.rows.forEachIndexed { r, row ->
                        Row(Modifier.background(if (r == 0) codeBg else Color.Transparent)) {
                            row.forEach { cell ->
                                Text(inline(cell, ink, link, codeBg), color = ink, fontSize = (fontSize * 0.92f).sp, fontWeight = if (r == 0) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.width(140.dp).padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun Linked(text: AnnotatedString, color: Color, fontSize: Float, open: (String) -> Unit, italic: Boolean = false) {
    ClickableText(
        text,
        style = TextStyle(color = color, fontSize = fontSize.sp, lineHeight = (fontSize * 1.55f).sp, fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal),
        onClick = { offset -> text.getStringAnnotations("url", offset, offset).firstOrNull()?.let { runCatching { open(it.item) } } },
    )
}
