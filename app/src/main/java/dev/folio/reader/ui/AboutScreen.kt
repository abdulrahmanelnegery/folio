package dev.folio.reader.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** What the app is, who makes it, where its code lives and how it is licensed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Canvas(Modifier.size(104.dp)) {
                val k = size.width / 268f
                fun p(x: Float, y: Float) = Offset((x - 122f) * k, (y - 122f) * k)
                val w = 56f * k
                drawLine(Color(0xFF8A8A92), p(150f, 150f), p(150f, 362f), w, StrokeCap.Round)
                drawLine(Color(0xFF8A8A92), p(150f, 362f), p(362f, 362f), w, StrokeCap.Round)
                val c = p(312f, 200f)
                zimamDot(c, 104f * k, Color(0xFFFF5A1F))
            }
            Text("Rukn", style = MaterialTheme.typography.headlineMedium)
            Text("ركن  ·  a corner, a pillar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Version $version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "A calm reader for PDFs, text and paper books. Everything stays on this phone; the only network use is the lookup you start yourself.",
                Modifier.padding(top = 8.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            )
            Text("by Zimam", Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF5A1F))
            OutlinedButton(onClick = { open("https://github.com/zimamlabs") }, Modifier.fillMaxWidth()) { Text("Zimam on GitHub") }
            OutlinedButton(onClick = { open("https://github.com/zimamlabs/rukn") }, Modifier.fillMaxWidth()) { Text("Source code") }
            OutlinedButton(onClick = { open("https://github.com/zimamlabs/rukn/issues") }, Modifier.fillMaxWidth()) { Text("Report a problem") }
            Text(
                "© 2026 Zimam. Free software under the MIT licence.",
                Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
            )
        }
    }
}
