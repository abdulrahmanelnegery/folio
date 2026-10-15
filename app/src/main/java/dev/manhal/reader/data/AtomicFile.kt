package dev.manhal.reader.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Writes a file so a crash or a full disk never leaves it half written: temp file beside it, flushed to disk, then renamed over it. */
object AtomicFile {
    /** True when [file] now holds [bytes]; on false the old content is untouched. */
    fun write(file: File, bytes: ByteArray): Boolean {
        val tmp = File(file.parentFile, ".${file.name}.tmp")
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(tmp).use { it.write(bytes); it.fd.sync() }
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            true
        } catch (e: Exception) {
            tmp.delete()
            false
        }
    }
}
