package dev.folio.reader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An open PDF, drawn by Android's own renderer (the system's PDFium). It allows one open page at a
 * time, so every call takes the lock.
 */
class PdfFile private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    /** Page sizes in points; pages not read yet share the first page's shape. */
    private val sizes: Array<Pair<Float, Float>>,
) : AutoCloseable {
    private val lock = Mutex()
    val pageCount: Int = sizes.size

    /** Reads the real size of every page after the book is already on screen; true if any differ from the first. */
    suspend fun readAllSizes(): Boolean = lock.withLock {
        var changed = false
        for (i in 1 until pageCount) {
            val size = runCatching { renderer.openPage(i).use { it.width.toFloat() to it.height.toFloat() } }.getOrNull() ?: continue
            if (size != sizes[i]) { sizes[i] = size; changed = true }
        }
        changed
    }

    /** Width and height of [page] in points. */
    fun sizeOf(page: Int): Pair<Float, Float>? = sizes.getOrNull(page)

    /** Height over width of [page]. */
    fun aspect(page: Int): Float {
        val (w, h) = sizes.getOrNull(page) ?: return 1.414f
        return if (w > 0f && h > 0f) h / w else 1.414f
    }

    /** The page drawn at [widthPx] wide (height follows the page's shape), on white. */
    suspend fun render(page: Int, widthPx: Int): Bitmap? = lock.withLock {
        if (page !in 0 until pageCount || widthPx <= 0) return null
        val height = (widthPx * aspect(page)).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xFFFFFFFF.toInt())
        try {
            renderer.openPage(page).use { it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
            bitmap
        } catch (e: Exception) {
            Log.w("Folio", "page $page did not render", e)
            bitmap.recycle()
            null
        }
    }

    override fun close() {
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }

    companion object {
        /** Opens [uri]; null when it is not a readable PDF (or needs a password). */
        fun open(context: Context, uri: Uri): PdfFile? {
            val descriptor = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
            return try {
                val renderer = PdfRenderer(descriptor)
                // Only the first page is measured now; the rest follow in the background.
                val first = renderer.openPage(0).use { it.width.toFloat() to it.height.toFloat() }
                PdfFile(descriptor, renderer, Array(renderer.pageCount) { first })
            } catch (e: Exception) {
                Log.w("Folio", "could not open $uri", e)
                runCatching { descriptor.close() }
                null
            }
        }
    }
}
