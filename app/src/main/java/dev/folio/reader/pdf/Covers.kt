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

    /** The cover if it is already in memory, so a card can show it on its first frame. */
    fun peek(book: Book): Bitmap? = memory.get(book.id)

    private val mine: File get() = storage.dir("mycovers")

    suspend fun get(book: Book): Bitmap? {
        memory.get(book.id)?.let { return it }
        if (book.customCover) {
            val custom = withContext(Dispatchers.IO) { File(mine, "${book.id}.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } }
            if (custom != null) { memory.put(book.id, custom); return custom }
        }
        if (book.kind == dev.folio.reader.data.Kind.Epub) return epubCover(book)
        if (book.kind != dev.folio.reader.data.Kind.Pdf || book.physical) return null
        return withContext(Dispatchers.IO) {
            val file = File(dir, "${book.id}.jpg")
            val cached = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
            val bitmap = cached ?: gate.withPermit { draw(book, file) }
            bitmap?.also { memory.put(book.id, it) }
        }
    }

    /** The cover image named in the EPUB's package file, scaled down and kept as a small JPEG like other covers. */
    private suspend fun epubCover(book: Book): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(dir, "${book.id}.jpg")
        val cached = if (file.exists()) BitmapFactory.decodeFile(file.path) else null
        val bitmap = cached ?: run {
            val (pkg, _) = dev.folio.reader.epub.EpubStore.open(context, Uri.parse(book.uri), book.id) ?: return@run null
            val path = pkg.cover ?: return@run null
            val src = dev.folio.reader.epub.EpubStore.file(context, book.id, pkg, path)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.path, bounds)
            if (bounds.outWidth <= 0) return@run null
            val sample = Integer.highestOneBit((bounds.outWidth / COVER_WIDTH).coerceAtLeast(1))
            BitmapFactory.decodeFile(src.path, BitmapFactory.Options().apply { inSampleSize = sample })?.also { bmp ->
                runCatching { file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) } }
            }
        }
        bitmap?.also { memory.put(book.id, it) }
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

    /** Keeps [bytes] (any image Android can decode) as this book's own cover, scaled down to a card's size. */
    suspend fun setCustom(id: String, bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext false
        val sample = Integer.highestOneBit((bounds.outWidth / (COVER_WIDTH * 2)).coerceAtLeast(1))
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return@withContext false
        val scaled = if (bmp.width > COVER_WIDTH * 2) Bitmap.createScaledBitmap(bmp, COVER_WIDTH * 2, bmp.height * COVER_WIDTH * 2 / bmp.width, true) else bmp
        runCatching { File(mine, "$id.jpg").outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) } }.isSuccess.also { memory.remove(id) }
    }

    /** Drops the book's own cover so the one from the file (if any) shows again. */
    fun removeCustom(id: String) {
        memory.remove(id)
        File(mine, "$id.jpg").delete()
    }

    fun forget(id: String) {
        removeCustom(id)
        File(dir, "$id.jpg").delete()
    }

    private companion object {
        const val COVER_WIDTH = 420
    }
}
