package dev.folio.reader.edits

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

/**
 * One book's draft: the marks on screen, undo and redo, and the file they are kept in. Every change is written out at
 * once (the latest wins), so closing the app, or having it killed, loses nothing.
 */
class EditSession(private val file: File, scope: CoroutineScope, private val onCount: (Int) -> Unit) {
    private val history = EditHistory(Draft.fromJson(runCatching { file.takeIf { it.exists() }?.readText() }.getOrNull()))

    var marks: List<Mark> by mutableStateOf(history.draft.marks)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    val isEmpty: Boolean get() = marks.isEmpty()

    /** The JSON to write, or null to delete the file; only the newest request is acted on. */
    private val writes = Channel<String?>(Channel.CONFLATED)

    init {
        scope.launch(Dispatchers.IO) {
            for (text in writes) {
                runCatching {
                    if (text == null) file.delete()
                    else {
                        file.parentFile?.mkdirs()
                        val tmp = File(file.parentFile, file.name + ".part")
                        tmp.writeText(text)
                        tmp.renameTo(file)
                    }
                }
            }
        }
    }

    fun nextId() = history.nextId()
    fun draft(): Draft = history.draft

    fun add(mark: Mark) { history.add(mark); changed() }
    fun remove(id: Long) { history.remove(id); changed() }
    fun undo() { if (history.undo()) changed() }
    fun redo() { if (history.redo()) changed() }

    /** Throws the whole draft away, including the file. */
    fun discard() { history.clear(); changed() }

    private fun changed() {
        marks = history.draft.marks
        canUndo = history.canUndo
        canRedo = history.canRedo
        writes.trySend(if (marks.isEmpty()) null else history.draft.toJson())
        onCount(marks.size)
    }
}
