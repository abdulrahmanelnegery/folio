package dev.folio.reader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.Backups
import dev.folio.reader.data.homeTabs
import java.io.File
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: FolioViewModel, onBack: () -> Unit) {
    var confirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::exportBackup) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { confirm = { model.restoreFrom(it) } }
    }
    val tick by model.backupTick.collectAsState()
    val backups: List<File> = remember(refresh, tick) { model.backups.files() }
    val context = LocalContext.current
    // Coming back from the system's All files access screen, move to the Folio folder if it was allowed.
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.refreshStorage() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings and backup") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            val library by model.repo.state.collectAsState()
            Header("Home page tabs")
            Text("Home is always first. Hide the others or use the arrows to put them in the order you like.", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val tabs = library.homeTabs()
            tabs.forEachIndexed { i, cfg ->
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cfg.tab.label, style = MaterialTheme.typography.bodyLarge)
                        Text(cfg.tab.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (cfg.tab != dev.folio.reader.data.HomeTab.Home) {
                        IconButton(onClick = { model.repo.setHomeTabs(tabs.toMutableList().also { java.util.Collections.swap(it, i, i - 1) }) }, enabled = i > 1) { Icon(Icons.Rounded.ArrowUpward, "Move up") }
                        IconButton(onClick = { model.repo.setHomeTabs(tabs.toMutableList().also { java.util.Collections.swap(it, i, i + 1) }) }, enabled = i < tabs.lastIndex) { Icon(Icons.Rounded.ArrowDownward, "Move down") }
                        androidx.compose.material3.Switch(cfg.visible, { v -> model.repo.setHomeTabs(tabs.map { if (it.tab == cfg.tab) it.copy(visible = v) else it }) })
                    }
                }
            }

            Header("General")
            Row(
                Modifier.fillMaxWidth().clickable { model.repo.update { it.copy(confirmExit = !it.confirmExit) } }.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Ask before closing Folio", style = MaterialTheme.typography.bodyLarge)
                    Text("When you press back on the home screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(library.confirmExit, { v -> model.repo.update { it.copy(confirmExit = v) } })
            }

            Header("Folio folder")
            Text(
                if (model.storage.shared) "Folio keeps its library, backups and covers in ${model.storage.root.path}. It survives reinstalling the app."
                else "Folio keeps its data inside the app. Allow access to all files to use a visible Folio folder on the phone instead, so your reading places and backups survive reinstalling the app, like other readers do.",
                Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!model.storage.shared && android.os.Build.VERSION.SDK_INT >= 30) {
                Action(Icons.Rounded.FolderSpecial, "Use the Folio folder") {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }.onFailure { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
                }
            }

            Header("Backup")
            Text(
                "Reading places, lists, ratings, notes and bookmarks are copied automatically when you close a book (at most every few hours), and the last ${Backups.KEEP} copies are kept. " +
                    "Save a copy to a file to keep it elsewhere or move to another phone.",
                Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Action(Icons.Rounded.Backup, "Back up now") { model.backupNow() }
            Action(Icons.Rounded.FileUpload, "Save a backup file") { export.launch(Backups.exportName()) }
            Action(Icons.Rounded.FileDownload, "Restore from a backup file") { import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }

            Header("Automatic backups on this phone")
            if (backups.isEmpty()) {
                Text("None yet. The first one is made when the library has a book in it.", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            backups.forEach { file ->
                val state = remember(file) { model.backups.read(file) }
                Row(Modifier.fillMaxWidth().clickable(enabled = state != null) { confirm = { model.restoreFile(file) } }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f).padding(start = 20.dp)) {
                        Text(Backups.label(file), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            state?.let { "${it.books.size} books  ·  ${formatSize(file.length())}" } ?: "Unreadable",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state != null) Text("Restore", color = MaterialTheme.colorScheme.primary)
                }
            }

            Header("Online lookup")
            var key by remember { mutableStateOf(library.googleBooksKey) }
            androidx.compose.material3.OutlinedTextField(
                key, { key = it; model.repo.update { s -> s.copy(googleBooksKey = it.trim()) } },
                Modifier.fillMaxWidth().padding(horizontal = 24.dp), singleLine = true,
                label = { Text("Google Books key (optional)") },
                supportingText = { Text("Free from Google. Without it the shared quota is often used up, and Arabic books are found less.") },
            )

            Header("Storage")
            Action(Icons.Rounded.CleaningServices, "Clear saved covers") { model.covers.clear(); refresh++ }
            Action(Icons.Rounded.CleaningServices, "Clear saved pages (they are drawn again as you read)") { model.pageCache.clear(); refresh++ }

            Header("About")
            Text("Folio 0.7.5", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
            Text("A calm PDF reader. Everything stays on this phone.", Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Restore this backup?") },
            text = { Text("It replaces your current library, reading places, lists and notes. Books are found again by their files, so the PDFs must still be on the phone.") },
            confirmButton = { TextButton(onClick = { action(); confirm = null }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp))
}

@Composable
private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.padding(start = 20.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
