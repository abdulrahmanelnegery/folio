package dev.folio.reader.data

import java.io.File

/** A [DataFolder] on a plain directory, for tests. */
class TempData(val root: File) : DataFolder {
    override fun file(name: String) = File(root, name)
    override fun dir(name: String) = File(root, name).apply { mkdirs() }
}
