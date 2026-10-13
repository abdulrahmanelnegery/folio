package dev.folio.reader.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
    BackHandler(enabled = reading != null) { model.closeBook() }
    Box(Modifier.fillMaxSize().let { it }) {
        AnimatedContent(
            targetState = reading,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith (slideOutHorizontally { it / 4 } + fadeOut())
                }
            },
            label = "screen",
        ) { id ->
            if (id == null) {
                LibraryScreen(model, onOpenPicker = { picker.launch(arrayOf("application/pdf")) })
            } else {
                ReaderScreen(model, id)
            }
        }
        SnackbarHost(snackbar, Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
    }
}
