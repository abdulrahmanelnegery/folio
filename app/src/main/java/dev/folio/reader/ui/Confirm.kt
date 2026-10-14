package dev.folio.reader.ui

import dev.folio.reader.R
import dev.folio.reader.s
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** One question before something is cleared, deleted or removed. Every such action goes through this. */
class ConfirmRequest(val title: String, val text: String, val label: String = s(R.string.remove), val action: () -> Unit)

@Composable
fun ConfirmDialog(request: ConfirmRequest?, onDismiss: () -> Unit) {
    if (request == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(request.title) },
        text = { Text(request.text) },
        confirmButton = { TextButton(onClick = { request.action(); onDismiss() }) { Text(request.label) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}
