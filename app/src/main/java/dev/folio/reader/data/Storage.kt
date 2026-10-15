package dev.folio.reader.data

import dev.folio.reader.R
import dev.folio.reader.s
import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Where Manhal keeps its own data: the library (reading places, lists, ratings, notes), backups
 * and cover thumbnails. With "All files access" it is a visible `Zimam/Manhal` folder on the phone, which
 * survives reinstalling the app; without it, the app's private folder. Older folders are copied into it, verified
 * and left in place as a backup.
 */
class Storage(private val env: StorageEnv, private val unverifiedMessage: (old: String, target: String) -> String) : DataFolder {
    constructor(context: Context) : this(AndroidStorageEnv(context), { old, target -> s(R.string.rukn_could_not_verify_the, old, target) })

    @Volatile
    var root: File = initialRoot()
        private set

    /**
     * The folder to start from, without touching any file contents: the Zimam folder only once every old folder has a
     * verified-copy marker, otherwise the old folder that holds the library, so the app opens at once and the real check runs in the background.
     */
    private fun initialRoot(): File {
        if (!canUseShared()) return env.privateDir
        val olds = oldFolders()
        if (olds.all { FolderMigration.marker(sharedFolder, labelOf(it)).isFile }) return sharedFolder.takeIf { it.isDirectory && it.canWrite() } ?: env.privateDir
        return olds.firstOrNull { File(it, LIBRARY).isFile } ?: env.privateDir
    }

    val shared: Boolean get() = root != env.privateDir

    val sharedFolder: File get() = File(env.externalRoot, "Zimam/Manhal")

    /** Where earlier versions kept it: the hidden `.folio`, the visible `Folio` (the app was called Folio before Rukn), then the visible `Rukn` and `Zimam/Rukn` of the app's earlier name, which sat beside the other apps' folders. */
    val legacyFolders: List<File> get() = listOf(".folio", "Folio", "Rukn", "Zimam/Rukn").map { File(env.externalRoot, it) }

    fun canUseShared(): Boolean = env.canUseShared()

    override fun file(name: String) = File(root, name)

    override fun dir(name: String) = File(root, name).apply { mkdirs() }

    /** The old folders that still exist (kept as a backup after the copy). */
    fun oldFolders(): List<File> = legacyFolders.filter { it.isDirectory }

    /** Set to stop a copy between files (the next refresh resumes it); a half-written file is only ever a `.part` file. */
    @Volatile
    var cancelled: Boolean = false

    /** Set when the last switch could not be verified, so the screen can say why the old folder is still in use. */
    @Volatile
    var migrationMessage: String? = null
        private set

    /** Set when the switch found a different library in the previous location: its book count. It was kept as a backup you can restore. */
    @Volatile
    var otherLibraryBooks: Int? = null
        private set

    /** Old folders whose every file is verified in the Zimam folder right now, so removing them loses nothing. */
    fun removableOldFolders(): List<File> = if (root == sharedFolder) oldFolders().filter { FolderMigration.safeToRemove(it, sharedFolder, labelOf(it)) } else emptyList()

    /** Removes one old folder, only after checking again that everything in it is in the Zimam folder. */
    fun removeOldFolder(folder: File): Boolean {
        if (folder !in removableOldFolders() || !FolderMigration.safeToRemove(folder, sharedFolder, labelOf(folder))) return false
        return folder.deleteRecursively()
    }

    /**
     * Picks the folder to use now (call again after the permission changes). Returns true when it changed, so
     * callers reload. Old folders are copied into the Zimam folder and verified; they are never deleted here. If any
     * copy does not verify, the old location stays in use, [migrationMessage] says so, and the next call retries.
     */
    @Synchronized
    fun refresh(): Boolean {
        migrationMessage = null
        otherLibraryBooks = null
        var target = if (canUseShared()) sharedFolder.also { it.mkdirs() }.takeIf { it.isDirectory && it.canWrite() } ?: env.privateDir else env.privateDir
        if (target == sharedFolder) {
            for (old in oldFolders()) {
                if (FolderMigration.upToDate(old, sharedFolder, labelOf(old))) continue
                val r = FolderMigration.copyAndVerify(old, sharedFolder, labelOf(old), cancelled = { cancelled })
                if (!r.ok) {
                    migrationMessage = unverifiedMessage(old.path, sharedFolder.path)
                    target = old
                    break
                }
            }
        }
        if (target == root) return false
        FolderMigration.backupState(target)
        val sourceLibrary = File(root, LIBRARY)
        val targetLibrary = File(target, LIBRARY)
        // Never overwrite the target's own library: bring ours across only when it has none, otherwise keep ours beside it
        // and in the backup list, so the reader can restore it if it is the one they want.
        if (sourceLibrary.exists() && sourceLibrary != targetLibrary) {
            runCatching {
                if (!targetLibrary.exists()) sourceLibrary.copyTo(targetLibrary)
                else if (sourceLibrary.length() != targetLibrary.length() || FolderMigration.sha256(sourceLibrary) != FolderMigration.sha256(targetLibrary)) {
                    File(target, "migration-backup").apply { mkdirs() }.let { sourceLibrary.copyTo(File(it, "library-from-previous-location-${System.currentTimeMillis()}.json")) }
                    val other = LibraryRepository.decode(sourceLibrary.readText())
                    if (!other.isBlank()) {
                        File(target, BACKUPS).apply { mkdirs() }.let { sourceLibrary.copyTo(File(it, "manhal-${Backups.stamp(System.currentTimeMillis())}.json"), overwrite = false) }
                        otherLibraryBooks = other.books.size
                    }
                }
            }
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
        /** The name that marks a copied old folder; the old `Zimam/Rukn` and the old `Rukn` beside it must not share one. */
        fun labelOf(folder: File): String = if (folder.parentFile?.name == "Zimam") "Zimam-${folder.name}" else folder.name

        const val LIBRARY = "library.json"
        const val BACKUPS = "backups"
    }
}

/** What [Storage] needs from the phone, so the folder switch can be tested with plain directories. */
interface StorageEnv {
    val privateDir: File
    val externalRoot: File
    fun canUseShared(): Boolean
}

class AndroidStorageEnv(private val context: Context) : StorageEnv {
    override val privateDir: File get() = context.filesDir
    override val externalRoot: File get() = Environment.getExternalStorageDirectory()
    override fun canUseShared(): Boolean = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
}
