package dev.manhal.reader.ui

import dev.manhal.reader.data.label
import dev.manhal.reader.data.note
import dev.manhal.reader.R
import dev.manhal.reader.s
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.Arrangement
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
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
import dev.manhal.reader.ManhalViewModel
import dev.manhal.reader.data.Backups
import dev.manhal.reader.data.homeTabs
import java.io.File
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: ManhalViewModel, onBack: () -> Unit, onAbout: () -> Unit = {}, initialPage: SettingsPage? = null) {
    var page by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(initialPage) }
    val app by model.settings.state.collectAsState()
    val library by model.repo.state.collectAsState()
    val context = LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    // Coming back from the system's All files access screen, move to the Zimam folder if it was allowed.
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.refreshStorage() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    androidx.activity.compose.BackHandler(enabled = page != null) { page = null }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(page?.let { s(it.titleRes) } ?: s(R.string.settings)) },
                navigationIcon = { IconButton(onClick = { if (page != null) page = null else onBack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, s(R.string.back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            when (page) {
                null -> SettingsHub(app, library, model.storage.shared, version) { if (it == SettingsPage.About) onAbout() else page = it }
                SettingsPage.Appearance -> AppearancePage(app) { change -> model.settings.update(change) }
                SettingsPage.Layout -> LayoutPage(model, library)
                SettingsPage.Language -> LanguagePage(model, app)
                SettingsPage.Reader -> ReaderPage(model, library)
                SettingsPage.Data -> DataPage(model)
                SettingsPage.Privacy -> PrivacyPage(model, library)
                SettingsPage.About -> Unit
            }
        }
    }
}

@Composable
private fun LayoutPage(model: ManhalViewModel, library: dev.manhal.reader.data.LibraryState) {
            Header(s(R.string.home_page_tabs))
            Text(s(R.string.home_is_always_first_hide), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val tabs = library.homeTabs()
            tabs.forEachIndexed { i, cfg ->
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cfg.tab.label, style = MaterialTheme.typography.bodyLarge)
                        Text(cfg.tab.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (cfg.tab != dev.manhal.reader.data.HomeTab.Home) {
                        IconButton(onClick = { model.repo.setHomeTabs(tabs.toMutableList().also { java.util.Collections.swap(it, i, i - 1) }) }, enabled = i > 1) { Icon(Icons.Rounded.ArrowUpward, s(R.string.move_up)) }
                        IconButton(onClick = { model.repo.setHomeTabs(tabs.toMutableList().also { java.util.Collections.swap(it, i, i + 1) }) }, enabled = i < tabs.lastIndex) { Icon(Icons.Rounded.ArrowDownward, s(R.string.move_down)) }
                        androidx.compose.material3.Switch(cfg.visible, { v -> model.repo.setHomeTabs(tabs.map { if (it.tab == cfg.tab) it.copy(visible = v) else it }) })
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().clickable { model.repo.update { it.copy(confirmExit = !it.confirmExit) } }.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s(R.string.ask_before_closing_manhal), style = MaterialTheme.typography.bodyLarge)
                    Text(s(R.string.when_you_press_back_on), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(library.confirmExit, { v -> model.repo.update { it.copy(confirmExit = v) } })
            }

}

@Composable
private fun LanguagePage(model: ManhalViewModel, app: dev.manhal.reader.data.AppSettings) {
            run {
                val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                var language by remember { mutableStateOf(model.settings.value.language) }
                var digits by remember { mutableStateOf(model.settings.value.arabicDigits) }
                Text(s(R.string.language), Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.padding(horizontal = 24.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dev.manhal.reader.data.AppLanguage.entries.forEach { l ->
                        val name = when (l) { dev.manhal.reader.data.AppLanguage.System -> s(R.string.system); dev.manhal.reader.data.AppLanguage.English -> "English"; dev.manhal.reader.data.AppLanguage.Arabic -> "العربية" }
                        androidx.compose.material3.FilterChip(language == l, {
                            if (language != l) {
                                language = l
                                model.settings.update { it.copy(language = l) }
                                activity?.recreate()
                            }
                        }, { Text(name) })
                    }
                }
                Text(s(R.string.language_note), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    Modifier.fillMaxWidth().clickable { digits = !digits; model.settings.update { it.copy(arabicDigits = digits) }; activity?.recreate() }.padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s(R.string.arabic_digits), style = MaterialTheme.typography.bodyLarge)
                        Text(s(R.string.arabic_digits_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(digits, { digits = it; model.settings.update { s -> s.copy(arabicDigits = it) }; activity?.recreate() })
                }
            }
}

@Composable
private fun ReaderPage(model: ManhalViewModel, library: dev.manhal.reader.data.LibraryState) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(s(R.string.reading_theme))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            dev.manhal.reader.data.PageTheme.entries.forEach { t ->
                androidx.compose.material3.FilterChip(library.theme == t, { model.repo.update { it.copy(theme = t) } }, { Text(s(t.labelRes)) })
            }
        }
        Text(s(R.string.reading_theme_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle(s(R.string.epub_text_size))
        androidx.compose.material3.Slider(
            value = library.epubTextPercent.toFloat(),
            onValueChange = { v -> model.repo.update { it.copy(epubTextPercent = (Math.round(v / 10f) * 10).coerceIn(70, 220)) } },
            valueRange = 70f..220f, steps = 14,
            modifier = Modifier.heightIn(min = 48.dp),
        )
        Text("${library.epubTextPercent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SectionTitle(s(R.string.screen_rotation))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            dev.manhal.reader.data.Orientation.entries.forEach { o ->
                androidx.compose.material3.FilterChip(library.orientation == o, { model.repo.update { it.copy(orientation = o) } }, { Text(s(o.labelRes)) })
            }
        }
    }
}

@Composable
private fun PrivacyPage(model: ManhalViewModel, library: dev.manhal.reader.data.LibraryState) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ZimamCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s(R.string.what_is_stored), style = MaterialTheme.typography.titleSmall)
            Text(s(R.string.what_is_stored_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        ZimamCard { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s(R.string.permissions_explained), style = MaterialTheme.typography.titleSmall)
            Text(s(R.string.permissions_explained_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
            SectionTitle(s(R.string.online_lookup))
            var key by remember { mutableStateOf(library.googleBooksKey) }
            androidx.compose.material3.OutlinedTextField(
                key, { key = it; model.repo.update { s -> s.copy(googleBooksKey = it.trim()) } },
                Modifier.fillMaxWidth().padding(horizontal = 20.dp), singleLine = true,
                label = { Text(s(R.string.google_books_key_optional)) },
                supportingText = { Text(s(R.string.free_from_google_without_it)) },
            )

}

@Composable
private fun DataPage(model: ManhalViewModel) {
    var confirm by remember { mutableStateOf<(() -> Unit)?>(null) }
    var ask by remember { mutableStateOf<ConfirmRequest?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var removeOld by remember { mutableStateOf<java.io.File?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::exportBackup) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { confirm = { model.restoreFrom(it) } }
    }
    val tick by model.backupTick.collectAsState()
    // Everything slow is done off the main thread and filled in as it arrives, so the page opens at once:
    // the backup list (and each backup's book count, one at a time) and the check of the old folders (which reads every file).
    val backups: List<File> by produceState(emptyList<File>(), refresh, tick) { value = withContext(Dispatchers.IO) { model.backups.files() } }
    val backupBooks = remember { androidx.compose.runtime.mutableStateMapOf<File, Int>() }
    LaunchedEffect(backups) {
        for (f in backups) if (f !in backupBooks) withContext(Dispatchers.IO) { model.backups.read(f)?.books?.size }?.let { backupBooks[f] = it }
    }
    val settingsScope = rememberCoroutineScope()
    val context = LocalContext.current

            Header(s(R.string.zimam_folder))
            Text(
                if (model.storage.shared) s(R.string.manhal_keeps_its_library_backups, model.storage.root.path)
                else s(R.string.manhal_keeps_its_data_inside),
                Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!model.storage.shared && android.os.Build.VERSION.SDK_INT >= 30) {
                Action(Icons.Rounded.FolderSpecial, s(R.string.use_the_zimam_manhal_folder)) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }.onFailure { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
                }
            }

            model.storage.migrationMessage?.let {
                Text(it, Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            // The old folders stay as a backup after the copy; removing one asks first and checks every file again.
            val oldFolders by produceState(emptyList<File>(), refresh, model.storage.root) { value = withContext(Dispatchers.IO) { model.storage.removableOldFolders() } }
            oldFolders.forEach { old ->
                Text(s(R.string.your_earlier_folder_was_copied, old.path), Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Action(Icons.Rounded.FolderSpecial, s(R.string.remove_the_old_folder, old.name)) { removeOld = old }
            }

            Header(s(R.string.backup))
            Text(
                s(R.string.reading_places_lists_ratings_notes, Backups.KEEP) +
                    s(R.string.save_a_copy_to_a),
                Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Action(Icons.Rounded.Backup, s(R.string.back_up_now)) { model.backupNow() }
            Action(Icons.Rounded.FileUpload, s(R.string.save_a_backup_file)) { export.launch(Backups.exportName()) }
            Action(Icons.Rounded.FileDownload, s(R.string.restore_from_a_backup_file)) { import.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }

            Header(s(R.string.automatic_backups_on_this_phone))
            if (backups.isEmpty()) {
                Text(s(R.string.none_yet_the_first_one), Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            backups.forEach { file ->
                val books = backupBooks[file]
                Row(Modifier.fillMaxWidth().clickable(enabled = books != null) { confirm = { model.restoreFile(file) } }.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f).padding(start = 20.dp)) {
                        Text(Backups.label(file), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            books?.let { s(R.string.n_books_size, it, formatSize(file.length())) } ?: formatSize(file.length()),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (books != null) Text(s(R.string.restore), color = MaterialTheme.colorScheme.primary)
                }
            }

            Header(s(R.string.storage))
            val originals = remember(model.storage.root) { dev.manhal.reader.data.OriginalsFolder(model.originalsFolder) }
            val kept by produceState(0 to 0L, refresh) { value = withContext(Dispatchers.IO) { originals.count to originals.bytes } }
            if (kept.first > 0) {
                Text(s(R.string.originals_kept, kept.first, formatSize(kept.second)), Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Action(Icons.Rounded.CleaningServices, s(R.string.clear_old_originals)) {
                    ask = ConfirmRequest(s(R.string.clear_old_originals_title), s(R.string.clear_old_originals_text), s(R.string.clear)) {
                        settingsScope.launch { val n = withContext(Dispatchers.IO) { originals.clear() }; model.notify(s(R.string.originals_cleared, n)); refresh++ }
                    }
                }
            }
            Action(Icons.Rounded.CleaningServices, s(R.string.clear_saved_covers)) { ask = ConfirmRequest(s(R.string.clear_saved_covers_d613), s(R.string.they_are_drawn_again_from), s(R.string.clear)) { settingsScope.launch { withContext(Dispatchers.IO) { model.covers.clear() }; refresh++ } } }
            Action(Icons.Rounded.CleaningServices, s(R.string.clear_saved_pages_they_are)) { ask = ConfirmRequest(s(R.string.clear_saved_pages), s(R.string.they_are_drawn_again_as), s(R.string.clear)) { settingsScope.launch { withContext(Dispatchers.IO) { model.pageCache.clear() }; refresh++ } } }

    ConfirmDialog(ask) { ask = null }
    removeOld?.let { old ->
        AlertDialog(
            onDismissRequest = { removeOld = null },
            title = { Text(s(R.string.remove_b274, old.name)) },
            text = { Text(s(R.string.manhal_checks_once_more_that, old.path)) },
            confirmButton = { TextButton(onClick = {
                removeOld = null
                settingsScope.launch {
                    val ok = withContext(Dispatchers.IO) { model.storage.removeOldFolder(old) }
                    model.notify(if (ok) s(R.string.removed_x, old.name) else s(R.string.not_removed_a_file_did))
                    refresh++
                }
            }) { Text(s(R.string.remove)) } },
            dismissButton = { TextButton(onClick = { removeOld = null }) { Text(s(R.string.keep_it)) } },
        )
    }
    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(s(R.string.restore_this_backup)) },
            text = { Text(s(R.string.it_replaces_your_current_library)) },
            confirmButton = { TextButton(onClick = { action(); confirm = null }) { Text(s(R.string.restore)) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(s(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Header(text: String) {
    Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Diamond(8.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.padding(start = 20.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
