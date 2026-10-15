package dev.manhal.reader.data

/** Old names, kept only to find and migrate existing data. Nothing here is shown to the user or written for new data. */
object LegacyNames {
    const val HIDDEN_FOLDER = ".folio"
    const val OLDEST_FOLDER = "Folio"
    const val TOP_FOLDER = "Rukn"
    const val ZIMAM_FOLDER = "Zimam/Rukn"

    /** Where earlier versions kept the data, oldest first. */
    val FOLDERS = listOf(HIDDEN_FOLDER, OLDEST_FOLDER, TOP_FOLDER, ZIMAM_FOLDER)

    /** Backup file prefixes of earlier versions; they stay listed and restorable. */
    val BACKUP_PREFIXES = listOf("rukn-", "folio-")

    /** The preferences file that already holds the saved theme and language. */
    const val PREFS_FILE = "rukn-ui"
}
