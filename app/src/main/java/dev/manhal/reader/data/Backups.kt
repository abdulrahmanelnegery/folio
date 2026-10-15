package dev.manhal.reader.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local backups of the library and every reading place in it, in the `backups` folder next to the
 * library: one when Manhal starts if the last is over [GAP_MS] old, one whenever a book is closed
 * after that gap, and one on demand. The newest [KEEP] are kept. A copy can also be exported to,
 * and restored from, any file the user picks.
 */
class Backups(private val storage: DataFolder) {
    private val dir: File get() = storage.dir(Storage.BACKUPS)

    /** Writes a copy when the newest is older than [GAP_MS]. Returns true when it wrote one. */
    fun auto(state: LibraryState): Boolean {
        if (state.books.isEmpty()) return false
        val newest = files().firstOrNull()?.lastModified() ?: 0L
        if (System.currentTimeMillis() - newest < GAP_MS) return false
        return now(state) != null
    }

    /** Writes a copy now. */
    fun now(state: LibraryState): File? {
        val file = File(dir, "manhal-${stamp(System.currentTimeMillis())}.json")
        val ok = AtomicFile.write(file, LibraryRepository.encode(state).toByteArray())
        // Old copies go only once the new one is safely written, so a failing disk never empties the folder.
        if (ok) files().drop(KEEP).forEach { it.delete() }
        return file.takeIf { ok }
    }

    /** Backups, newest first. */
    fun files(): List<File> = dir.listFiles { f -> (listOf("manhal-") + LegacyNames.BACKUP_PREFIXES).any { f.name.startsWith(it) } && f.extension == "json" }.orEmpty().sortedByDescending { it.name }

    /** The newest backup that opens and holds something, with its state; null when there is none. */
    fun newestValid(): Pair<File, LibraryState>? = files().firstNotNullOfOrNull { f -> read(f)?.takeIf { !it.isBlank() }?.let { f to it } }

    fun read(file: File): LibraryState? = runCatching { LibraryRepository.decode(file.readText()) }.getOrNull()

    companion object {
        const val KEEP = 40
        private const val GAP_MS = 3L * 60 * 60 * 1000

        fun stamp(millis: Long): String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(millis))
        fun exportName(): String = "manhal-backup-${stamp(System.currentTimeMillis())}.json"

        /** "2026-10-13 18:40" from a backup file name. */
        fun label(file: File): String = file.nameWithoutExtension.removePrefix("manhal-").let { n -> LegacyNames.BACKUP_PREFIXES.fold(n) { acc, p -> acc.removePrefix(p) } }.replace('_', ' ').let {
            val (date, time) = it.split(' ').let { p -> p[0] to p.getOrElse(1) { "" } }
            if (time.isEmpty()) date else "$date ${time.substring(0, 5).replace('-', ':')}"
        }
    }
}
