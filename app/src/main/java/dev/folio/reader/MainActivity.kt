package dev.folio.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.folio.reader.ui.FolioApp
import dev.folio.reader.ui.FolioTheme

class MainActivity : ComponentActivity() {
    private val model: FolioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            FolioTheme {
                var splash by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
                androidx.compose.foundation.layout.Box {
                    FolioApp(model)
                    androidx.compose.animation.AnimatedVisibility(splash, exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(260))) {
                        dev.folio.reader.ui.Splash("Rukn") { splash = false }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onStop() {
        super.onStop()
        model.commitReading()
        model.repo.flush()
        model.persistDrafts()
    }

    private fun handle(intent: Intent) {
        if (intent.action != Intent.ACTION_VIEW) return
        intent.data?.let { model.importUris(listOf(it), intent.flags) }
    }
}
