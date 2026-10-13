package dev.folio.reader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import dev.folio.reader.data.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/** First-page covers: drawn once, kept as small JPEGs in the cache folder. */
class Covers(private val context: Context, private val storage: dev.folio.reader.data.Storage) {
    private val memory = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val dir: File get() = storage.dir("covers")
    private val gate = Semaphore(2)

    suspend fun get(book: Book): Bitmap? {
        memory.get(book.id)?.let { return it }
        return withContext(Dispatchers.IO) {
            val file = File(dir, "${book.id}.jpg")
            val cached = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
            val bitmap = cached ?: gate.withPermit { draw(book, file) }
            bitmap?.also { memory.put(book.id, it) }
        }
    }

    private suspend fun draw(book: Book, file: File): Bitmap? {
        val pdf = PdfFile.open(context, Uri.parse(book.uri)) ?: return null
        return try {
            pdf.render(0, COVER_WIDTH)?.also { bmp ->
                runCatching { file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 82, it) } }
            }
        } finally {
            pdf.close()
        }
    }

    /** Deletes every saved cover; they are drawn again when needed. */
    fun clear() {
        memory.evictAll()
        dir.listFiles()?.forEach { it.delete() }
    }

    fun forget(id: String) {
        memory.remove(id)
        File(dir, "$id.jpg").delete()
    }

    private companion object {
        const val COVER_WIDTH = 420
    }
}
