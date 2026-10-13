package dev.folio.reader.data

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Where Rukn keeps its own data: the library (reading places, lists, ratings, notes), backups
 * and cover thumbnails. With "All files access" it is a visible `Rukn` folder on the phone, which
 * survives reinstalling the app; without it, the app's private folder. The old hidden `.folio` folder is
 * moved into it the first time.
 */
class Storage(private val context: Context) {
    @Volatile
    var root: File = context.filesDir
        private set

    val shared: Boolean get() = root != context.filesDir

    val sharedFolder: File get() = File(Environment.getExternalStorageDirectory(), "Rukn")

    /** Where earlier versions kept it, hidden. */
    /** Where earlier versions kept it: the hidden `.folio`, then the visible `Folio` (the app was called Folio before Rukn). */
    val legacyFolders: List<File> get() = listOf(".folio", "Folio").map { File(Environment.getExternalStorageDirectory(), it) }

    fun canUseShared(): Boolean = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()

    fun file(name: String) = File(root, name)

    fun dir(name: String) = File(root, name).apply { mkdirs() }

    /**
     * Picks the folder to use now (call again after the permission changes). Returns true when it
     * changed, so callers reload. Data moves to the shared folder when it has nothing newer.
     */
    fun refresh(): Boolean {
        if (canUseShared()) legacyFolders.forEach { migrateLegacyFolder(it, sharedFolder) }
        val target = if (canUseShared()) sharedFolder.also { it.mkdirs() }.takeIf { it.isDirectory && it.canWrite() } ?: context.filesDir else context.filesDir
        if (target == root) return false
        val sourceLibrary = File(root, LIBRARY)
        val targetLibrary = File(target, LIBRARY)
        // A newer library already in the target (a reinstall) wins; otherwise bring ours across.
        if (sourceLibrary.exists() && (!targetLibrary.exists() || sourceLibrary.lastModified() > targetLibrary.lastModified())) {
            runCatching { sourceLibrary.copyTo(targetLibrary, overwrite = true) }
        }
        runCatching {
            File(root, BACKUPS).listFiles()?.forEach { f ->
                val to = File(File(target, BACKUPS).apply { mkdirs() }, f.name)
                if (!to.exists()) f.copyTo(to)
            }
        }
        root = target
        return true
    }

    companion object {
        /**
         * Moves the old hidden folder into the visible one. On the same volume that is one rename; if the visible
         * folder already exists (or the rename fails) whatever is missing or newer is copied over first. The old
         * folder is deleted only after everything was copied, so a failure loses nothing. True when it moved.
         */
        fun migrateLegacyFolder(legacy: File, target: File): Boolean {
            if (!legacy.isDirectory || legacy.absolutePath == target.absolutePath) return false
            if (!target.exists() && legacy.renameTo(target)) return true
            return runCatching {
                target.mkdirs()
                legacy.walkTopDown().forEach { f ->
                    val dest = File(target, f.relativeTo(legacy).path)
                    if (f.isDirectory) dest.mkdirs()
                    else if (!dest.exists() || f.lastModified() > dest.lastModified()) {
                        dest.parentFile?.mkdirs()
                        f.copyTo(dest, overwrite = true)
                    }
                }
                legacy.deleteRecursively()
            }.getOrDefault(false)
        }

        const val LIBRARY = "library.json"
        const val BACKUPS = "backups"
    }
}
