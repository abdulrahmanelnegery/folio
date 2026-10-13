package dev.folio.reader.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.sqrt

/**
 * All PDF work happens on this one thread: Android's renderer is not thread-safe, it allows one open
 * page at a time, and drawing a page is far too slow for the main thread. Being a single thread it also
 * keeps the work in order without any locking.
 */
private val Worker = Executors.newSingleThreadExecutor { r -> Thread(r, "folio-pdf").apply { priority = Thread.NORM_PRIORITY - 1 } }.asCoroutineDispatcher()

/** A page is never drawn bigger than this many pixels, so a zoomed page cannot exhaust memory. */
private const val MAX_PIXELS = 5_500_000
private const val MAX_SIDE = 4096

/**
 * An open PDF, drawn by Android's own renderer (the system's PDFium).
 */
class PdfFile private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    /** Page sizes in points; a page not drawn yet shares the first page's shape until it is learned. */
    private val sizes: Array<Pair<Float, Float>>,
) : AutoCloseable {
    @Volatile
    private var closed = false
    val pageCount: Int = sizes.size

    /** Called from the worker when a page turns out to have a different shape than assumed, so the layout can follow. */
    @Volatile
    var onShapeLearned: (() -> Unit)? = null

    /** Width and height of [page] in points. */
    fun sizeOf(page: Int): Pair<Float, Float>? = sizes.getOrNull(page)

    /** Height over width of [page]. */
    fun aspect(page: Int): Float {
        val (w, h) = sizes.getOrNull(page) ?: return 1.414f
        return if (w > 0f && h > 0f) h / w else 1.414f
    }

    /**
     * The page drawn [widthPx] wide (height follows the page's shape), on white. It runs off the main
     * thread, gives up early when its caller is cancelled (the page scrolled away), and keeps within
     * the pixel budget.
     */
    suspend fun render(page: Int, widthPx: Int): Bitmap? = withContext(Worker) {
        ensureActive()
        if (closed || page !in 0 until pageCount || widthPx <= 0) return@withContext null
        try {
            renderer.openPage(page).use { p ->
                val shape = p.width.toFloat() to p.height.toFloat()
                if (shape.first > 0f && shape.second > 0f && !sameShape(shape, sizes[page])) {
                    sizes[page] = shape
                    onShapeLearned?.invoke()
                }
                var w = widthPx.coerceAtMost(MAX_SIDE)
                var h = (w * p.height.toFloat() / p.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                if (h > MAX_SIDE || w.toLong() * h > MAX_PIXELS) {
                    val k = minOf(MAX_SIDE.toFloat() / h, sqrt(MAX_PIXELS.toFloat() / (w.toFloat() * h))).coerceAtMost(1f)
                    w = (w * k).toInt().coerceAtLeast(1); h = (h * k).toInt().coerceAtLeast(1)
                }
                ensureActive()
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(0xFFFFFFFF.toInt())
                try {
                    p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                } catch (e: Exception) {
                    bitmap.recycle()
                    throw e
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Log.w("Folio", "out of memory drawing page $page", e)
            null
        } catch (e: Exception) {
            Log.w("Folio", "page $page did not render", e)
            null
        }
    }

    private fun sameShape(a: Pair<Float, Float>, b: Pair<Float, Float>): Boolean {
        val ra = a.second / a.first
        val rb = b.second / b.first
        return kotlin.math.abs(ra - rb) < 0.005f * rb
    }

    /**
     * Finds the white margins common to the book: a few pages spread through it are drawn small and the
     * median blank border on each side is used for all pages, so every page crops the same way and the
     * layout does not jump. Returns left, top, right, bottom as fractions of the page, or null when the
     * pages are too empty to tell.
     */
    suspend fun detectMargins(): List<Float>? = withContext(Worker) {
        val count = pageCount
        val samples = if (count <= 9) (0 until count).toList() else (1..9).map { (count * it / 10).coerceIn(0, count - 1) }.distinct()
        val boxes = ArrayList<FloatArray>()
        for (page in samples) {
            ensureActive()
            if (closed) return@withContext null
            runCatching {
                renderer.openPage(page).use { p ->
                    val w = 320
                    val h = (w * p.height.toFloat() / p.width.coerceAtLeast(1)).toInt().coerceIn(1, 1200)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(0xFFFFFFFF.toInt())
                    p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    contentBox(bmp)?.let { boxes += it }
                    bmp.recycle()
                }
            }
        }
        if (boxes.size < 2) return@withContext null
        fun median(side: Int) = boxes.map { it[side] }.sorted().let { it[it.size / 2] }
        // A little breathing room so no letter is cut.
        listOf(median(0), median(1), median(2), median(3)).map { (it - 0.012f).coerceIn(0f, 0.4f) }
    }

    /** The blank border of a drawn page as fractions (left, top, right, bottom), or null when it is empty. */
    private fun contentBox(bmp: Bitmap): FloatArray? {
        val w = bmp.width
        val h = bmp.height
        val row = IntArray(w)
        var minX = w; var maxX = -1; var minY = h; var maxY = -1
        for (y in 0 until h) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            var ink = 0
            for (x in 0 until w) {
                val c = row[x]
                val lum = ((c shr 16 and 0xFF) * 3 + (c shr 8 and 0xFF) * 6 + (c and 0xFF)) / 10
                if (lum < 235) { ink++; if (x < minX) minX = x; if (x > maxX) maxX = x }
            }
            // A line counts as content only with a few dark pixels, so a speck of dust does not move the border.
            if (ink >= 2) { if (y < minY) minY = y; if (y > maxY) maxY = y }
        }
        if (maxX < 0 || maxY < 0) return null
        return floatArrayOf(minX.toFloat() / w, minY.toFloat() / h, 1f - (maxX + 1f) / w, 1f - (maxY + 1f) / h)
    }

    /** Closes once the worker is free, off the calling thread. */
    fun closeInBackground() {
        CoroutineScope(Worker).launch { close() }
    }

    override fun close() {
        closed = true
        runCatching { renderer.close() }
        runCatching { descriptor.close() }
    }

    companion object {
        /** Opens [uri]; null when it is not a readable PDF (or needs a password). */
        fun open(context: Context, uri: Uri): PdfFile? {
            val descriptor = runCatching { context.contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
            return try {
                val renderer = PdfRenderer(descriptor)
                // Only the first page is measured now; the others are learned as they are drawn.
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
