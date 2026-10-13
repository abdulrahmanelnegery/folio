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
import androidx.compose.foundation.layout.Box
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
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            model.messageShown()
        }
    }
    var settings by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    BackHandler(enabled = reading != null) { model.closeBook() }
    BackHandler(enabled = reading == null && settings) { settings = false }
    Box(Modifier.fillMaxSize().let { it }) {
        AnimatedContent(
            targetState = reading ?: if (settings) "settings" else null,
            // A short cross-fade: sliding two heavy screens at once is what made it stutter.
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(110)) },
            label = "screen",
        ) { id ->
            if (id == null) {
                LibraryScreen(model, onOpenPicker = { picker.launch(arrayOf("application/pdf")) }, onSettings = { settings = true })
            } else if (id == "settings") {
                SettingsScreen(model, onBack = { settings = false })
            } else {
                ReaderScreen(model, id)
            }
        }
        SnackbarHost(snackbar, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}
