package dev.folio.reader.data

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Where Folio keeps its own data: the library (reading places, lists, ratings, notes), backups
 * and cover thumbnails. With "All files access" it is a `.folio` folder on the phone, which
 * survives reinstalling the app; without it, the app's private folder.
 */
class Storage(private val context: Context) {
    @Volatile
    var root: File = context.filesDir
        private set

    val shared: Boolean get() = root != context.filesDir

    val sharedFolder: File get() = File(Environment.getExternalStorageDirectory(), ".folio")

    fun canUseShared(): Boolean = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()

    fun file(name: String) = File(root, name)

    fun dir(name: String) = File(root, name).apply { mkdirs() }

    /**
     * Picks the folder to use now (call again after the permission changes). Returns true when it
     * changed, so callers reload. Data moves to the shared folder when it has nothing newer.
     */
    fun refresh(): Boolean {
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
        const val LIBRARY = "library.json"
        const val BACKUPS = "backups"
    }
}
