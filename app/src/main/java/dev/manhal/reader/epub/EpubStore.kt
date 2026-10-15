package dev.manhal.reader.epub

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipInputStream

/** EPUBs are zip files; each book is unpacked once into its own folder so chapters, styles and images load as plain files. */
object EpubStore {
    private fun root(context: Context, id: String) = File(context.filesDir, "epub/$id")

    /** The folder holding a book's files, unpacking it first if needed; null when the file is not a readable EPUB. */
    fun open(context: Context, uri: Uri, id: String): Pair<EpubPackage, File>? {
        val dir = root(context, id)
        val marker = File(dir, ".unpacked")
        if (!marker.exists()) {
            dir.deleteRecursively(); dir.mkdirs()
            val ok = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> unpack(input, dir) } ?: false
            }.getOrDefault(false)
            if (!ok) { dir.deleteRecursively(); return null }
            marker.writeText("1")
        }
        val container = File(dir, "META-INF/container.xml").takeIf { it.exists() }?.readText() ?: return null
        val opfPath = EpubPackage.packagePath(container) ?: return null
        val opf = File(dir, opfPath).takeIf { it.exists() }?.readText() ?: return null
        val pkg = EpubPackage.parse(opf, opfPath) ?: return null
        return pkg to dir
    }

    /** Unpacks into [dir], refusing any entry that would land outside it and capping the total size. */
    internal fun unpack(input: java.io.InputStream, dir: File, needContainer: Boolean = true): Boolean {
        val base = dir.canonicalFile
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val out = File(base, entry.name).canonicalFile
                if (out.path != base.path && !out.path.startsWith(base.path + File.separator)) return false
                if (entry.isDirectory) { out.mkdirs(); continue }
                out.parentFile?.mkdirs()
                out.outputStream().use { o ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = zip.read(buf); if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) return false
                        o.write(buf, 0, n)
                    }
                }
            }
        }
        return !needContainer || File(base, "META-INF/container.xml").exists()
    }

    fun file(context: Context, id: String, pkg: EpubPackage, relative: String): File = File(root(context, id), (pkg.folder.takeIf { it.isNotEmpty() }?.let { "$it/" }.orEmpty()) + relative)

    fun delete(context: Context, id: String) { root(context, id).deleteRecursively() }

    private const val MAX_BYTES = 600L * 1024 * 1024
}
