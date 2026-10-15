package dev.manhal.reader.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** Writes an edited text file back without ever leaving it truncated or half written with no way back. */
object TextSave {
    /** A real file: temp file beside it, then rename. A failure leaves the original byte-identical. */
    fun toFile(file: File, bytes: ByteArray): Boolean = AtomicFile.write(file, bytes)

    /**
     * A file behind another app's provider cannot be renamed into place, so the new text is first kept whole in [spare]
     * (the draft), then streamed in and read back. On any failure [spare] stays so nothing the reader typed is lost, and
     * false is returned; on success [spare] is deleted.
     */
    fun toProvider(bytes: ByteArray, spare: File, openOut: () -> OutputStream?, openIn: () -> InputStream?): Boolean {
        if (!AtomicFile.write(spare, bytes)) return false
        val ok = runCatching {
            val out = openOut() ?: return@runCatching false
            out.use { it.write(bytes) }
            val back = openIn()?.use { it.readBytes() } ?: return@runCatching false
            back.contentEquals(bytes)
        }.getOrDefault(false)
        if (ok) spare.delete()
        return ok
    }
}
