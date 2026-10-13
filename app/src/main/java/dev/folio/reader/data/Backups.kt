package dev.folio.reader.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local backups of the library and every reading place in it: one copy a day, the newest
 * [KEEP] kept, in the app's private folder. A copy can also be exported to, and restored from,
 * any file the user picks.
 */
class Backups(context: Context) {
    private val dir = File(context.filesDir, "backups").apply { mkdirs() }

    /** Writes today's copy when there is not one yet, and drops the oldest past [KEEP]. */
    fun daily(state: LibraryState) {
        if (state.books.isEmpty()) return
        val today = File(dir, "folio-${stamp(System.currentTimeMillis())}.json")
        if (!today.exists()) runCatching { today.writeText(LibraryRepository.encode(state)) }
        files().drop(KEEP).forEach { it.delete() }
    }

    /** Backups, newest first. */
    fun files(): List<File> = dir.listFiles { f -> f.name.startsWith("folio-") && f.extension == "json" }.orEmpty().sortedByDescending { it.name }

    fun read(file: File): LibraryState? = runCatching { LibraryRepository.decode(file.readText()) }.getOrNull()

    companion object {
        const val KEEP = 14
        fun stamp(millis: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
        fun exportName(): String = "folio-backup-${stamp(System.currentTimeMillis())}.json"
    }
}
