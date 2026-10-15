package dev.manhal.reader.edits

import dev.manhal.reader.R
import dev.manhal.reader.s
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The two things only the real PDF code can do, so the safety steps around them can be tested without it. */
interface PdfWork {
    /** Writes [source] with the draft applied to [out]. Throws when it cannot. */
    fun write(source: File, out: File)

    /** The page count of [file] when it opens as a PDF, otherwise null. */
    fun pageCount(file: File): Int?
}

sealed interface SaveResult {
    data class Saved(val file: File) : SaveResult
    /** Nothing was changed: the original is as it was. */
    data class Failed(val reason: String) : SaveResult
}

/**
 * The write paths. Neither ever opens the original for writing: the new content goes to a temporary file next to the
 * target, is checked to open as a PDF with the same number of pages, and only then replaces the target in one step.
 */
object SafeWrite {
    /** Writes the edited copy to [target], which must not exist; it is created only once the copy has checked out. */
    fun saveAsNew(source: File, target: File, work: PdfWork): SaveResult {
        if (target.exists()) return SaveResult.Failed(s(R.string.a_file_named_is_already, target.name))
        val tmp = File(target.parentFile, ".${target.name}.part")
        return try {
            val expected = work.pageCount(source) ?: return SaveResult.Failed(s(R.string.the_original_could_not_be))
            tmp.parentFile?.mkdirs()
            work.write(source, tmp)
            if (work.pageCount(tmp) != expected) return SaveResult.Failed(s(R.string.the_edited_copy_did_not))
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            SaveResult.Saved(target)
        } catch (e: OutOfMemoryError) {
            SaveResult.Failed(s(R.string.the_file_is_too_big_to_edit))
        } catch (e: Exception) {
            SaveResult.Failed(e.message ?: s(R.string.the_copy_could_not_be))
        } finally {
            tmp.delete()
        }
    }

    /**
     * Replaces [original] with its edited version. The original is first copied to [backup] (kept), the edit is written
     * and checked beside it, and the swap is a single rename. Any failure before the swap leaves the original untouched;
     * if the swap itself fails the backup is put back.
     */
    fun overwrite(original: File, backup: File, work: PdfWork): SaveResult {
        if (!original.canWrite()) return SaveResult.Failed(s(R.string.this_file_is_read_only))
        val tmp = File(original.parentFile, ".${original.name}.part")
        return try {
            val expected = work.pageCount(original) ?: return SaveResult.Failed(s(R.string.the_original_could_not_be))
            backup.parentFile?.mkdirs()
            original.copyTo(backup, overwrite = true)
            if (backup.length() != original.length()) return SaveResult.Failed(s(R.string.the_backup_copy_was_incomplete))
            work.write(original, tmp)
            if (work.pageCount(tmp) != expected) return SaveResult.Failed(s(R.string.the_edited_file_did_not))
            try {
                Files.move(tmp.toPath(), original.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                if (!original.exists() || original.length() == 0L) backup.copyTo(original, overwrite = true)
                return SaveResult.Failed(s(R.string.the_file_could_not_be_1ab0))
            }
            SaveResult.Saved(original)
        } catch (e: OutOfMemoryError) {
            SaveResult.Failed(s(R.string.the_file_is_too_big_to_edit))
        } catch (e: Exception) {
            SaveResult.Failed(e.message ?: s(R.string.the_file_could_not_be_564c))
        } finally {
            tmp.delete()
        }
    }

    /** "name (edited).pdf" beside [original], or "name (edited 2).pdf" when that is taken. */
    fun editedName(original: File, taken: (String) -> Boolean = { File(original.parentFile, it).exists() }): String {
        val stem = original.nameWithoutExtension
        val ext = original.extension.ifEmpty { "pdf" }
        var n = 1
        while (true) {
            val name = "$stem (edited${if (n > 1) " $n" else ""}).$ext"
            if (!taken(name)) return name
            n++
        }
    }
}
