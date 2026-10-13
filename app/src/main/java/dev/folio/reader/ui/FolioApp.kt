package dev.folio.reader.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.folio.reader.FolioViewModel

@Composable
fun FolioApp(model: FolioViewModel) {
    val reading by model.reading.collectAsState()
    val message by model.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) model.importUris(uris, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val library by model.repo.state.collectAsState()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            model.messageShown()
        }
    }
    var settings by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    // The text reader asks about unsaved edits itself; every other reader closes straight away.
    BackHandler(enabled = reading != null && library.books.firstOrNull { it.id == reading }?.kind != dev.folio.reader.data.Kind.Text) { model.closeBook() }
    BackHandler(enabled = reading == null && settings) { settings = false }
    // Back walks out one screen at a time; on the home screen it can ask before closing Folio.
    var askExit by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val activity = androidx.activity.compose.LocalActivity.current
    BackHandler(enabled = reading == null && !settings && library.confirmExit) { askExit = true }
    if (askExit) ExitDialog(
        onStay = { askExit = false },
        onClose = { dontAsk ->
            askExit = false
            if (dontAsk) model.repo.update { it.copy(confirmExit = false) }
            activity?.finish()
        },
    )
    Box(Modifier.fillMaxSize().let { it }) {
        AnimatedContent(
            targetState = reading ?: if (settings) "settings" else null,
            // A short cross-fade: sliding two heavy screens at once is what made it stutter.
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
            label = "screen",
        ) { id ->
            if (id == null) {
                LibraryScreen(model, onOpenPicker = { picker.launch(arrayOf("*/*")) }, onSettings = { settings = true })
            } else if (id == "settings") {
                SettingsScreen(model, onBack = { settings = false })
            } else if (library.books.firstOrNull { it.id == id }?.kind == dev.folio.reader.data.Kind.Text) {
                TextScreen(model, id)
            } else {
                ReaderScreen(model, id)
            }
        }
        SnackbarHost(snackbar, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}

@Composable
private fun ExitDialog(onStay: () -> Unit, onClose: (dontAskAgain: Boolean) -> Unit) {
    var dontAsk by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onStay,
        title = { androidx.compose.material3.Text("Close Folio?") },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text("You are on the home screen. Your reading places are saved.")
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(top = 12.dp).clickable { dontAsk = !dontAsk },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Checkbox(dontAsk, { dontAsk = it })
                    androidx.compose.material3.Text("Do not ask again")
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onClose(dontAsk) }) { androidx.compose.material3.Text("Close") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onStay) { androidx.compose.material3.Text("Stay") } },
    )
}
