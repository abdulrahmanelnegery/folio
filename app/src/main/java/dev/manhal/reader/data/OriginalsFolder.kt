package dev.manhal.reader.data

import java.io.File

/** The copies of PDFs kept when one was overwritten (`originals/`). They are the way back, so they are only ever cleared on request. */
class OriginalsFolder(private val dir: File) {
    private fun files(): List<File> = dir.listFiles { f -> f.isFile }.orEmpty().toList()

    val count: Int get() = files().size
    val bytes: Long get() = files().sumOf { it.length() }

    /** Deletes the kept copies and returns how many went; an empty or missing folder is fine. */
    fun clear(): Int = files().count { it.delete() }
}
