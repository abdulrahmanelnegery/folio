package dev.folio.reader.ui

import dev.folio.reader.R
import dev.folio.reader.s
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.ReleaseNotes

private const val REPO = "https://github.com/zimamlabs/rukn"

/** The About page: the mark on a glow, what Rukn is, the tour and what's new, the privacy promise, the links and who makes it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(model: FolioViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    var news by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s(R.string.about)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, s(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            GlowBox(Modifier.fillMaxWidth(), strength = 0.3f) {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Image(painterResource(dev.folio.reader.R.drawable.zimam_mark), null, Modifier.size(132.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(s(R.string.rukn), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(s(R.string.a_calm_reader_for_pdfs), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Text(s(R.string.version, version), style = MaterialTheme.typography.labelLarge, color = ZimamOrange)
                }
            }
            Text(
                s(R.string.rukn_keeps_your_library_your),
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AboutCard(s(R.string.show_the_tour_again), s(R.string.a_quick_look_at_what)) { model.tourRequested.value = true; onBack() }
            AboutCard(s(R.string.what_is_new), s(R.string.the_changes_in_this_version)) { news = true }
            ZimamCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Diamond(); Spacer(Modifier.width(8.dp)); Text(s(R.string.private_by_design), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                    Text(s(R.string.no_account_and_no_tracking), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ZimamCard {
                Column {
                    LinkRow(s(R.string.source_code), REPO) { open(it) }
                    LinkRow(s(R.string.releases), "$REPO/releases") { open(it) }
                    LinkRow(s(R.string.report_an_issue), "$REPO/issues") { open(it) }
                    LinkRow(s(R.string.licence_mit), "$REPO/blob/main/LICENSE") { open(it) }
                    LinkRow(s(R.string.zimam_labs), "https://github.com/zimamlabs") { open(it) }
                }
            }
            Text(s(R.string.by_zimam_labs), Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium, color = ZimamOrange)
        }
    }
    if (news) AlertDialog(
        onDismissRequest = { news = false },
        title = { Text(s(R.string.new_in_rukn, version)) },
        text = {
            val notes = ReleaseNotes.forVersion(version)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (notes.isEmpty()) Text(s(R.string.nothing_to_list_for_this)) else notes.forEach { Text("\u2022  $it") }
            }
        },
        confirmButton = { TextButton(onClick = { news = false }) { Text(s(R.string.close)) } },
    )
}

@Composable
private fun AboutCard(title: String, text: String, onClick: () -> Unit) {
    ZimamCard(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(CardCorner)).clickable(onClick = onClick)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Diamond(); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkRow(label: String, url: String, open: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text("\u203A", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
