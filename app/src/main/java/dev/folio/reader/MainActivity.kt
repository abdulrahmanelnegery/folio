package dev.folio.reader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dev.folio.reader.ui.FolioApp
import dev.folio.reader.ui.FolioTheme

class MainActivity : ComponentActivity() {
    private val model: FolioViewModel by viewModels()

    override fun attachBaseContext(base: android.content.Context) {
        val wrapped = dev.folio.reader.data.AppLanguagePrefs.load(base).wrap(base)
        Strings.res = wrapped.resources
        Strings.arabicDigits = dev.folio.reader.data.AppLanguagePrefs.arabicDigits(base)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val startTheme = dev.folio.reader.data.AppThemePrefs.load(this)
        val systemDark = { (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES }
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { startTheme.isDark(systemDark()) },
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { startTheme.isDark(systemDark()) },
        )
        super.onCreate(savedInstanceState)
        // The window is painted in the chosen theme before Compose draws, so there is no flash of the other one.
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(if (startTheme.isDark(systemDark())) 0xFF0F0F0F.toInt() else 0xFFFFF8F0.toInt()))
        if (savedInstanceState == null) handle(intent)
        setContent {
            val mode by model.appTheme.collectAsState()
            androidx.compose.runtime.SideEffect {
                val dark = { mode.isDark(systemDark()) }
                enableEdgeToEdge(
                    statusBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark() },
                    navigationBarStyle = androidx.activity.SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark() },
                )
            }
            FolioTheme(mode) {
                var splash by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(true) }
                androidx.compose.foundation.layout.Box {
                    FolioApp(model)
                    if (!splash) dev.folio.reader.ui.WelcomeHost(model)
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
