package dev.manhal.reader.data

import java.io.File

/** Where the app's own files live; [Storage] on the phone, a temporary folder in tests. */
interface DataFolder {
    fun file(name: String): File
    fun dir(name: String): File
}
