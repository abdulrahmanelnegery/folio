package dev.manhal.reader.data

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dev.manhal.reader.epub.EpubPackage
import dev.manhal.reader.epub.EpubStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import java.io.File

/** Where a book's text comes from for each kind; each returns the search as a flow of progress that stops when the collector goes away. */
object BookSearchSources {
    private fun run(query: String, total: Int, units: (ensureActive: () -> Unit) -> Sequence<Pair<Int, String>>): Flow<SearchProgress> = channelFlow {
        val job = currentCoroutineContext()
        BookSearch.search(units { job.ensureActive() }, total, query) { trySendBlocking(it) }
    }.conflate().flowOn(Dispatchers.IO)

    /** A text or Markdown file already in memory: one unit. */
    fun text(text: String, query: String): Flow<SearchProgress> = run(query, 1) { _ -> sequenceOf(0 to text) }

    /** An unpacked EPUB: one unit per chapter of the reading order. */
    fun epub(context: Context, id: String, pkg: EpubPackage, query: String): Flow<SearchProgress> = run(query, pkg.spine.size) { check ->
        sequence {
            pkg.spine.forEachIndexed { i, rel ->
                check()
                yield(i to (runCatching { BookSearch.htmlToText(EpubStore.file(context, id, pkg, rel).readText()) }.getOrDefault("")))
            }
        }
    }

    /** A PDF on disk: the text layer of one page at a time; a scanned PDF has none and reports that. */
    fun pdf(context: Context, file: File, pageCount: Int, query: String): Flow<SearchProgress> = run(query, pageCount) { check ->
        sequence {
            PDFBoxResourceLoader.init(context.applicationContext)
            val doc = runCatching { PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly()) }.getOrNull()
            try {
                val stripper = PDFTextStripper()
                for (page in 1..pageCount) {
                    check()
                    val text = if (doc == null) "" else runCatching { stripper.startPage = page; stripper.endPage = page; stripper.getText(doc) }.getOrDefault("")
                    yield((page - 1) to text.replace(Regex("\\s+"), " ").trim())
                }
            } finally {
                doc?.close()
            }
        }
    }
}
