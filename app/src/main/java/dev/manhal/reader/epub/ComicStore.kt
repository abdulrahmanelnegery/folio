package dev.manhal.reader.epub

import android.content.Context
import android.net.Uri
import java.io.File

/** Comic books in a `.cbz` (a zip of page images): unpacked once, pages ordered the way people number them (page2 before page10). */
object ComicStore {
    private val images = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    private fun root(context: Context, id: String) = File(context.filesDir, "comic/$id")

    /** The page images of a book in reading order; empty when the file is not a readable comic. */
    fun open(context: Context, uri: Uri, id: String): List<File> {
        val dir = root(context, id)
        val marker = File(dir, ".unpacked")
        if (!marker.exists()) {
            dir.deleteRecursively(); dir.mkdirs()
            val ok = runCatching { context.contentResolver.openInputStream(uri)?.use { EpubStore.unpack(it, dir, needContainer = false) } ?: false }.getOrDefault(false)
            if (!ok) { dir.deleteRecursively(); return emptyList() }
            marker.writeText("1")
        }
        return pages(dir)
    }

    internal fun pages(dir: File): List<File> =
        dir.walkTopDown().filter { it.isFile && it.extension.lowercase() in images && !it.name.startsWith(".") && !it.path.contains("__MACOSX") }.toList().sortedWith(naturalOrder())

    internal fun naturalOrder(): Comparator<File> = Comparator { a, b -> compareNatural(a.path.lowercase(), b.path.lowercase()) }

    internal fun compareNatural(a: String, b: String): Int {
        var i = 0; var j = 0
        while (i < a.length && j < b.length) {
            if (a[i].isDigit() && b[j].isDigit()) {
                var x = i; while (x < a.length && a[x].isDigit()) x++
                var y = j; while (y < b.length && b[y].isDigit()) y++
                val na = a.substring(i, x).trimStart('0'); val nb = b.substring(j, y).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb); if (c != 0) return c
                i = x; j = y
            } else {
                if (a[i] != b[j]) return a[i] - b[j]
                i++; j++
            }
        }
        return (a.length - i) - (b.length - j)
    }

    fun delete(context: Context, id: String) { root(context, id).deleteRecursively() }
}
