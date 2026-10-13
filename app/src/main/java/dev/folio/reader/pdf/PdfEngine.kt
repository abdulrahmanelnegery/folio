package dev.folio.reader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.legere.pdfiumandroid.PdfDocument
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** An open PDF. Pdfium is not thread-safe per document, so every call takes the lock. */
class PdfFile private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val document: PdfDocument,
    val pageCount: Int,
    /** Page sizes in points, read once. */
    private val sizes: List<Pair<Float, Float>>,
) : AutoCloseable {
    private val lock = Mutex()

    fun aspect(page: Int): Float {
        val (w, h) = sizes.getOrNull(page) ?: return 1.414f
        return if (w > 0f) h / w else 1.414f
    }

    /** The page drawn at [widthPx] wide (height follows the page's shape), white background. */
    suspend fun render(page: Int, widthPx: Int): Bitmap? = lock.withLock {
        if (page !in 0 until pageCount || widthPx <= 0) return null
        val height = (widthPx * aspect(page)).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFFFFFFF.toInt())
        runCatching {
            document.openPage(page).use { it.renderPageBitmap(bitmap, 0, 0, widthPx, height, false) }
        }.onFailure { bitmap.recycle() }.map { bitmap }.getOrNull()
    }

    override fun close() {
        runCatching { document.close() }
        runCatching { descriptor.close() }
    }

    companion object {
        /** Opens [uri]; null when it is not a readable PDF. */
        fun open(context: Context, uri: Uri): PdfFile? {
            val descriptor = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
            return runCatching {
                val core = PdfiumCore(context)
                val document = core.newDocument(descriptor)
                val count = document.getPageCount()
                val sizes = (0 until count).map { i ->
                    document.openPage(i).use { it.getPageWidthPoint().toFloat() to it.getPageHeightPoint().toFloat() }
                }
                PdfFile(descriptor, document, count, sizes)
            }.getOrElse {
                runCatching { descriptor.close() }
                null
            }
        }
    }
}
