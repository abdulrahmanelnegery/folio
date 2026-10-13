package dev.folio.reader.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.FileTypes
import dev.folio.reader.data.Kind
import dev.folio.reader.data.nameOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

/** Where the explorer opens and, when set, the list the picked files are added to. */
data class ExplorerRequest(val targetList: String? = null, val startDir: String? = null)

private enum class FileSort(val label: String) { Name("Name"), Newest("Newest first"), Oldest("Oldest first"), Largest("Largest first") }
private enum class FileFilter(val label: String) { All("All"), Books("Books"), Folders("Folders") }

private class Entry(val file: File, val isDir: Boolean, val size: Long, val modified: Long, val kind: Kind?)

private fun allFilesAllowed(): Boolean = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

private fun volumes(): List<Triple<String, ImageVector, File>> {
    val out = ArrayList<Triple<String, ImageVector, File>>()
    val internal = Environment.getExternalStorageDirectory()
    out += Triple("Phone", Icons.Rounded.Smartphone, internal)
    File(internal, Environment.DIRECTORY_DOWNLOADS).takeIf { it.isDirectory }?.let { out += Triple("Downloads", Icons.Rounded.Download, it) }
    File(internal, Environment.DIRECTORY_DOCUMENTS).takeIf { it.isDirectory }?.let { out += Triple("Documents", Icons.Rounded.Description, it) }
    File("/storage").listFiles()?.filter { it.isDirectory && it.name != "emulated" && it.name != "self" && it.canRead() }?.forEach { out += Triple("SD card ${it.name}", Icons.Rounded.SdCard, it) }
    return out
}

private fun iconFor(e: Entry): Pair<ImageVector, Color> = when {
    e.isDir -> Icons.Rounded.Folder to Color(0xFFE0A93B)
    e.kind == Kind.Pdf -> Icons.Rounded.PictureAsPdf to Color(0xFFD64545)
    e.kind == Kind.Epub -> Icons.Rounded.MenuBook to Color(0xFF3F8F6B)
    e.kind == Kind.Comic -> Icons.Rounded.MenuBook to Color(0xFF8A5BC4)
    e.kind == Kind.Text -> Icons.Rounded.Description to Color(0xFF4A7FB5)
    else -> Icons.AutoMirrored.Rounded.InsertDriveFile to Color(0xFF8A8F98)
}

private fun readable(size: Long): String = when {
    size < 1024 -> "$size B"
    size < 1024 * 1024 -> "${size / 1024} KB"
    size < 1024L * 1024 * 1024 -> "%.1f MB".format(size / 1048576.0)
    else -> "%.2f GB".format(size / 1073741824.0)
}

/** Our own file browser: breadcrumb, search, filters, sort, multi-select, and straight into the library. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun ExplorerScreen(model: FolioViewModel, request: ExplorerRequest, onClose: () -> Unit) {
    val context = LocalContext.current
    val library by model.repo.state.collectAsState()
    var allowed by remember { mutableStateOf(allFilesAllowed()) }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) allowed = allFilesAllowed() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val roots = remember(allowed) { if (allowed) volumes() else emptyList() }
    val home = Environment.getExternalStorageDirectory()
    var dir by remember {
        mutableStateOf(
            request.startDir?.let(::File)?.takeIf { it.isDirectory } ?: library.explorerDir.takeIf { it.isNotEmpty() }?.let(::File)?.takeIf { it.isDirectory } ?: home,
        )
    }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(FileSort.Name) }
    var filter by remember { mutableStateOf(FileFilter.All) }
    var sortMenu by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    val picking = picked.isNotEmpty() || request.targetList != null

    // The folder's contents, or a search through it and everything below it.
    LaunchedEffect(dir, query, hidden, allowed) {
        if (!allowed) return@LaunchedEffect
        busy = true
        if (query.isNotBlank()) delay(300)
        entries = withContext(Dispatchers.IO) {
            val q = query.trim().lowercase()
            val found = ArrayList<Entry>()
            fun entryOf(f: File) = Entry(f, f.isDirectory, if (f.isFile) f.length() else 0L, f.lastModified(), if (f.isFile) FileTypes.kindOf(f.name) else null)
            if (q.isEmpty()) {
                dir.listFiles()?.forEach { f -> if (hidden || !f.name.startsWith(".")) found += entryOf(f) }
            } else {
                dir.walkTopDown().maxDepth(6).onEnter { hidden || !it.name.startsWith(".") }.forEach { f ->
                    if (!isActive || found.size >= 300) return@forEach
                    if (f != dir && f.name.lowercase().contains(q) && (hidden || !f.name.startsWith("."))) found += entryOf(f)
                }
            }
            found
        }
        busy = false
        model.repo.update { it.copy(explorerDir = dir.absolutePath) }
    }
    val shown = remember(entries, sort, filter) {
        entries.filter { e ->
            when (filter) {
                FileFilter.All -> true
                FileFilter.Books -> !e.isDir && e.kind != null
                FileFilter.Folders -> e.isDir
            }
        }.sortedWith(
            compareBy<Entry> { !it.isDir }.let { base ->
                when (sort) {
                    FileSort.Name -> base.thenBy { it.file.name.lowercase() }
                    FileSort.Newest -> base.thenByDescending { it.modified }
                    FileSort.Oldest -> base.thenBy { it.modified }
                    FileSort.Largest -> base.thenByDescending { it.size }
                }
            },
        )
    }
    val inLibrary = remember(library.books) { library.books.map { it.uri }.toSet() }

    fun up() {
        val parent = dir.parentFile
        if (query.isNotBlank()) { query = ""; searching = false }
        else if (dir != home && parent != null && parent.canRead()) dir = parent
        else onClose()
    }
    BackHandler { if (picked.isNotEmpty()) picked = emptySet() else up() }

    fun addPicked(open: Boolean) {
        val uris = picked.map { Uri.fromFile(File(it)) }
        model.importUris(uris, open = open && request.targetList == null, addToList = request.targetList)
        picked = emptySet()
        if (request.targetList != null) onClose()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (searching) OutlinedTextField(
                        query, { query = it }, singleLine = true, placeholder = { Text("Search in ${dir.name.ifEmpty { "phone" }}") },
                        shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth(),
                    ) else Column {
                        Text(if (picked.isNotEmpty()) "${picked.size} selected" else request.targetList?.let { "Add to “${nameOf(it)}”" } ?: "Browse files", fontWeight = FontWeight.SemiBold)
                        Text(dir.name.ifEmpty { "Phone" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                },
                navigationIcon = { IconButton(onClick = { if (picked.isNotEmpty()) picked = emptySet() else up() }) { Icon(if (picked.isNotEmpty()) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) { Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, "Search") }
                    IconButton(onClick = { hidden = !hidden }) { Icon(if (hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, if (hidden) "Hide hidden files" else "Show hidden files") }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            FileSort.entries.forEach { s ->
                                DropdownMenuItem(text = { Text(s.label) }, trailingIcon = { if (s == sort) Icon(Icons.Rounded.Check, null) }, onClick = { sort = s; sortMenu = false })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            AnimatedVisibility(picked.isNotEmpty()) {
                Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { addPicked(open = false) }, Modifier.weight(1f)) {
                            Text(if (request.targetList != null) "Add ${picked.size} to “${nameOf(request.targetList)}”" else "Add ${picked.size} to library")
                        }
                        if (request.targetList == null && picked.size == 1) OutlinedButton(onClick = { addPicked(open = true) }) { Text("Open") }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!allowed) {
                NeedsAccess(onGrant = {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }.onFailure { context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
                }, onClose = onClose)
                return@Column
            }
            // Where to start: the phone, Downloads, Documents, an SD card.
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(roots) { (label, icon, root) ->
                    FilterChip(
                        selected = dir.absolutePath.startsWith(root.absolutePath) && root.absolutePath.length >= (roots.filter { dir.absolutePath.startsWith(it.third.absolutePath) }.maxOfOrNull { it.third.absolutePath.length } ?: 0),
                        onClick = { dir = root; query = ""; searching = false },
                        label = { Text(label) },
                        leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) },
                    )
                }
            }
            Breadcrumb(dir, home) { dir = it; query = ""; searching = false }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FileFilter.entries.forEach { f -> FilterChip(filter == f, { filter = f }, { Text(f.label) }) }
                Spacer(Modifier.weight(1f))
                Text(if (busy) "Looking…" else "${shown.size} items", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (shown.isEmpty() && !busy) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Folder, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Text(if (query.isNotBlank()) "Nothing matches “$query”" else "This folder is empty", style = MaterialTheme.typography.titleMedium)
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp)) {
                items(shown, key = { it.file.absolutePath }) { e ->
                    val (icon, tint) = iconFor(e)
                    val selected = e.file.absolutePath in picked
                    val known = Uri.fromFile(e.file).toString() in inLibrary
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .combinedClickable(
                                onClick = {
                                    when {
                                        e.isDir -> { dir = e.file; query = ""; searching = false }
                                        e.kind == null -> {}
                                        picking -> picked = if (selected) picked - e.file.absolutePath else picked + e.file.absolutePath
                                        else -> model.importUris(listOf(Uri.fromFile(e.file)))
                                    }
                                },
                                onLongClick = { if (!e.isDir && e.kind != null) picked = if (selected) picked - e.file.absolutePath else picked + e.file.absolutePath },
                            ).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = tint)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(e.file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (e.isDir || e.kind != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                            val where = if (query.isNotBlank()) e.file.parentFile?.name.orEmpty() + "  ·  " else ""
                            Text(
                                where + (if (e.isDir) "Folder" else readable(e.size)) + "  ·  " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(e.modified)),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        when {
                            selected -> Icon(Icons.Rounded.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
                            known -> Text("In library", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            e.isDir -> Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** The path as tappable pieces from the phone's root down to here. */
@Composable
private fun Breadcrumb(dir: File, home: File, onGo: (File) -> Unit) {
    val parts = remember(dir) {
        val list = ArrayList<File>()
        var f: File? = dir
        while (f != null && f.absolutePath.length >= home.absolutePath.length) { list += f; f = f.parentFile }
        list.reversed()
    }
    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        items(parts) { f ->
            val last = f == parts.last()
            Text(
                if (f == home) "Phone" else f.name,
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = !last) { onGo(f) }.padding(horizontal = 6.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = if (last) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                fontWeight = if (last) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (!last) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NeedsAccess(onGrant: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Folder, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Browse the files on your phone", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Allow access to all files and Rukn can show your folders, search them and open a book straight from where it is. Nothing is uploaded; it only reads the names and the files you open.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onGrant) { Text("Allow access") }
        OutlinedButton(onClick = onClose, Modifier.padding(top = 8.dp)) { Text("Not now") }
    }
}
