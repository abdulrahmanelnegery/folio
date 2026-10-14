package dev.folio.reader.data

import dev.folio.reader.R
import dev.folio.reader.s
import java.io.File
import java.security.MessageDigest

/**
 * Copies an old data folder into the new one without ever losing anything: every file is copied through a `.part`
 * file and renamed, a file already in the destination is never overwritten (when it differs, the old copy is kept
 * beside it as `name (from Old).ext`), and the result is checked by file count, size and SHA-256. The old folder is
 * never touched here; it stays as a backup until the user asks to remove it, and removal re-checks first.
 */
object FolderMigration {
    /** One marker per old folder, written only after its copy verified; its date says when. */
    fun marker(target: File, label: String) = File(target, ".migrated-$label")

    data class Result(val ok: Boolean, val copied: Int, val kept: Int, val problems: List<String>)

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun files(root: File) = root.walkTopDown().filter { it.isFile && !it.name.endsWith(".part") }.toList()

    /** Where a clashing old file is kept: `library.json` becomes `library (from Rukn).json`, then `(from Rukn 2)` if that is taken too. */
    internal fun clashName(dest: File, label: String): File {
        val stem = dest.nameWithoutExtension
        val ext = dest.extension.let { if (it.isEmpty()) "" else ".$it" }
        var n = 1
        while (true) {
            val candidate = File(dest.parentFile, s(R.string.from, stem, label, if (n > 1) " $n" else "", ext))
            if (!candidate.exists()) return candidate
            n++
        }
    }

    /** Finds where [source]'s file ended up under [target]: the same path, or a kept clash copy next to it. */
    private fun landing(source: File, file: File, target: File, label: String): File? {
        val dest = File(target, file.relativeTo(source).path)
        val hash = sha256(file)
        if (dest.isFile && dest.length() == file.length() && sha256(dest) == hash) return dest
        val stem = dest.nameWithoutExtension
        val ext = dest.extension.let { if (it.isEmpty()) "" else ".$it" }
        return dest.parentFile?.listFiles()?.firstOrNull {
            it.isFile && it.name.startsWith("$stem (from $label") && it.name.endsWith(ext) && it.length() == file.length() && sha256(it) == hash
        }
    }

    /** What is wrong, if anything: a file with no verified counterpart in [target]. Empty means the copy is complete. */
    fun verify(source: File, target: File, label: String = source.name): List<String> =
        files(source).mapNotNull { f -> if (landing(source, f, target, label) == null) "missing or different: ${f.relativeTo(source).path}" else null }

    fun copyAndVerify(source: File, target: File, label: String = source.name, cancelled: () -> Boolean = { false }): Result {
        if (!source.isDirectory || source.absolutePath == target.absolutePath) return Result(true, 0, 0, emptyList())
        var copied = 0; var kept = 0
        val problems = mutableListOf<String>()
        try {
            target.mkdirs()
            source.walkTopDown().filter { it.isDirectory }.forEach { File(target, it.relativeTo(source).path).mkdirs() }
            for (f in files(source)) {
                if (cancelled()) { problems += "copy cancelled"; break }
                if (landing(source, f, target, label) != null) continue
                var dest = File(target, f.relativeTo(source).path)
                if (dest.exists()) { dest = clashName(dest, label); kept++ }
                val part = File(dest.parentFile, dest.name + ".part")
                dest.parentFile?.mkdirs()
                f.copyTo(part, overwrite = true)
                if (!part.renameTo(dest)) { part.delete(); problems += "could not place ${dest.name}"; continue }
                copied++
            }
        } catch (e: Exception) {
            problems += "copy stopped: ${e.message ?: e.javaClass.simpleName}"
        }
        problems += verify(source, target, label)
        if (problems.isEmpty()) runCatching { marker(target, label).appendText(s(R.string.copied_from_files_kept_beside, label, copied, kept)) }
        return Result(problems.isEmpty(), copied, kept, problems)
    }

    /** True when [old] was verified before and nothing in it has changed since, so the copy can be skipped. */
    fun upToDate(old: File, target: File, label: String = old.name): Boolean {
        val m = marker(target, label)
        return m.isFile && files(old).none { it.lastModified() > m.lastModified() }
    }

    /** True only when every file of [old] is verified in [target] right now. */
    fun safeToRemove(old: File, target: File, label: String = old.name): Boolean =
        old.isDirectory && target.isDirectory && verify(old, target, label).isEmpty()

    /** Saves the state files once before anything writes to a freshly migrated folder. */
    fun backupState(target: File, stamp: Long = System.currentTimeMillis()) {
        val lib = File(target, Storage.LIBRARY)
        val dir = File(target, "migration-backup")
        if (lib.isFile && !dir.exists()) runCatching { dir.mkdirs(); lib.copyTo(File(dir, "library-$stamp.json")) }
    }
}
