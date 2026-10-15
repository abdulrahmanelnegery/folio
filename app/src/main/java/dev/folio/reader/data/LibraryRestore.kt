package dev.folio.reader.data

/** Replaces the library with a restored one, but only a real one, and only after the current library has a safety copy. */
class LibraryRestore(private val repo: LibraryRepository, private val backups: Backups) {
    enum class Result { Restored, NotABackup, NoSafetyCopy }

    /** [restored] is the decoded file, or null when it did not decode. `{}` and other people's JSON decode as blank, so they are refused. */
    fun apply(restored: LibraryState?): Result {
        if (restored == null || restored.isBlank()) return Result.NotABackup
        val current = repo.state.value
        if (!current.isBlank() && backups.now(current) == null) return Result.NoSafetyCopy
        repo.replace(restored)
        repo.flush()
        return Result.Restored
    }
}
