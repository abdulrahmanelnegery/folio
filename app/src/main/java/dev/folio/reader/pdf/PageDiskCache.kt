package dev.folio.reader.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.folio.reader.data.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Pages drawn before, kept as small JPEGs: reopening a book (or coming back to a page) reads one instead
 * of drawing it again, which is much faster. It stays under a size limit by dropping the least recently used.
 */
class PageDiskCache(private val storage: Storage) {
    private val dir: File get() = storage.dir("pages")
    private var writes = 0

    private fun file(book: String, page: Int, width: Int) = File(dir, "$book-$page-$width.jpg")

    suspend fun get(book: String, page: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        val f = file(book, page, width)
        if (!f.exists()) return@withContext null
        f.setLastModified(System.currentTimeMillis())
        runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull()
    }

    suspend fun put(book: String, page: Int, width: Int, bitmap: Bitmap) = withContext(Dispatchers.IO) {
        runCatching { file(book, page, width).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) } }
        if (++writes % 40 == 0) trim()
    }

    /** Deletes the least recently used pages until the folder is under [MAX_BYTES]. */
    fun trim() {
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (f in files) {
            if (total <= MAX_BYTES) break
            total -= f.length()
            f.delete()
        }
    }

    fun forget(book: String) {
        dir.listFiles { f -> f.name.startsWith("$book-") }?.forEach { it.delete() }
    }

    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val MAX_BYTES = 400L * 1024 * 1024
    }
}
