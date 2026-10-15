package dev.manhal.reader.ui

import dev.manhal.reader.R
import dev.manhal.reader.s
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import dev.manhal.reader.ManhalViewModel

@Composable
fun ManhalApp(model: ManhalViewModel) {
    val reduceMotion = LocalReduceMotion.current
    val reading by model.reading.collectAsState()
    val message by model.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val migrating by model.migrating.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) model.importUris(uris, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    // Only what this screen needs: the whole library changes every few seconds while reading.
    val kindOfReading by remember(reading) { model.repo.state.map { st -> st.books.firstOrNull { it.id == reading }?.kind }.distinctUntilChanged() }.collectAsState(null)
    val confirmExit by remember { model.repo.state.map { it.confirmExit }.distinctUntilChanged() }.collectAsState(true)
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            model.messageShown()
        }
    }
    var settings by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var about by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var explorer by remember { androidx.compose.runtime.mutableStateOf<ExplorerRequest?>(null) }
    // The text reader asks about unsaved edits itself; every other reader closes straight away.
    BackHandler(enabled = reading != null && kindOfReading != dev.manhal.reader.data.Kind.Text) { model.closeBook() }
    BackHandler(enabled = reading == null && settings) { settings = false }
    BackHandler(enabled = reading == null && about) { about = false }
    // Back walks out one screen at a time; on the home screen it can ask before closing Manhal.
    var askExit by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val activity = androidx.activity.compose.LocalActivity.current
    BackHandler(enabled = reading == null && !settings && !about && explorer == null && confirmExit) { askExit = true }
    if (askExit) ExitDialog(
        onStay = { askExit = false },
        onClose = { dontAsk ->
            askExit = false
            if (dontAsk) model.repo.update { it.copy(confirmExit = false) }
            activity?.finish()
        },
    )
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope { while (true) { awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial); model.touched() } }
        },
    ) {
        AnimatedContent(
            targetState = reading ?: if (explorer != null) "explorer" else if (settings) "settings" else if (about) "about" else null,
            // Short and soft: a book opens by growing and fading in from just under full size while the library gives way, and
            // closes by easing out a touch larger while the library appears beneath; other screens simply cross-fade.
            transitionSpec = {
                if (reduceMotion) return@AnimatedContent androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                fun isBook(id: String?) = id != null && id != "settings" && id != "explorer" && id != "about"
                when {
                    !isBook(initialState) && isBook(targetState) ->
                        (fadeIn(tween(190, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                            androidx.compose.animation.scaleIn(initialScale = 0.94f, animationSpec = tween(230, easing = androidx.compose.animation.core.FastOutSlowInEasing))) togetherWith
                            fadeOut(tween(90))
                    // Closing is a small, quick move: the book slips a little to the right and fades while the library settles in from just under full size.
                    isBook(initialState) && !isBook(targetState) ->
                        (fadeIn(tween(200, delayMillis = 30)) + androidx.compose.animation.scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing))) togetherWith
                            (fadeOut(tween(130)) + androidx.compose.animation.slideOutHorizontally(tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 16 })
                    else -> fadeIn(tween(150)) togetherWith fadeOut(tween(110))
                }
            },
            label = s(R.string.screen),
        ) { id ->
            if (id == null) {
                LibraryScreen(model, onOpenPicker = { picker.launch(arrayOf("*/*")) }, onSettings = { settings = true }, onAbout = { about = true }, onBrowse = { list, dir -> explorer = ExplorerRequest(list, dir) })
            } else if (id == "explorer") {
                explorer?.let { ExplorerScreen(model, it, onClose = { explorer = null }) }
            } else if (id == "about") {
                AboutScreen(model, onBack = { about = false })
            } else if (id == "settings") {
                SettingsScreen(model, onBack = { settings = false }, onAbout = { settings = false; about = true })
            } else {
                // The book is built only once the opening glide has finished, so the glide itself is not competing with
                // opening the file; the page then fades in over the same paper-coloured sheet.
                val settled = transition.currentState == androidx.compose.animation.EnterExitState.Visible &&
                    transition.targetState == androidx.compose.animation.EnterExitState.Visible
                var built by remember(id) { androidx.compose.runtime.mutableStateOf(false) }
                LaunchedEffect(settled) { if (settled) built = true }
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    androidx.compose.animation.AnimatedVisibility(built, enter = fadeIn(tween(140))) {
                        when (kindOfReading) {
                            dev.manhal.reader.data.Kind.Text -> TextScreen(model, id)
                            dev.manhal.reader.data.Kind.Epub -> EpubScreen(model, id)
                            dev.manhal.reader.data.Kind.Comic -> ComicScreen(model, id)
                            else -> ReaderScreen(model, id)
                        }
                    }
                }
            }
        }
        if (migrating) {
            androidx.compose.material3.Surface(
                Modifier.align(androidx.compose.ui.Alignment.TopCenter).statusBarsPadding().padding(top = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp,
            ) {
                androidx.compose.foundation.layout.Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
                    androidx.compose.material3.Text(s(R.string.moving_your_library_to_zimam), style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                }
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
        title = { androidx.compose.material3.Text(s(R.string.close_manhal)) },
        text = {
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(s(R.string.you_are_on_the_home))
                androidx.compose.foundation.layout.Row(
                    Modifier.padding(top = 12.dp).clickable { dontAsk = !dontAsk },
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    androidx.compose.material3.Checkbox(dontAsk, { dontAsk = it })
                    androidx.compose.material3.Text(s(R.string.do_not_ask_again))
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onClose(dontAsk) }) { androidx.compose.material3.Text(s(R.string.close)) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onStay) { androidx.compose.material3.Text(s(R.string.stay)) } },
    )
}

/** Counts reading time for [id] while the screen is shown, the app is in front and the reader is touching or scrolling. */
@Composable
fun ReadingClock(model: ManhalViewModel, id: String) {
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.LaunchedEffect(id) {
        var last = android.os.SystemClock.elapsedRealtime()
        while (true) {
            kotlinx.coroutines.delay(1000)
            val now = android.os.SystemClock.elapsedRealtime()
            if (owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) model.readingTick(id, now - last)
            last = now
        }
    }
}
