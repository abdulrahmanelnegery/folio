package dev.folio.reader.ui

import dev.folio.reader.sp
import dev.folio.reader.data.label
import dev.folio.reader.data.note
import androidx.annotation.StringRes
import dev.folio.reader.R
import dev.folio.reader.s
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import dev.folio.reader.data.sessionCount
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.Book
import dev.folio.reader.data.PdfInfo
import dev.folio.reader.data.SortOrder
import dev.folio.reader.data.Status
import dev.folio.reader.data.Tab
import dev.folio.reader.data.ViewMode
import dev.folio.reader.data.depthOf
import dev.folio.reader.data.moveTargets
import dev.folio.reader.data.streak
import dev.folio.reader.data.lastDays
import dev.folio.reader.data.day
import dev.folio.reader.data.isInside
import dev.folio.reader.data.childLists
import dev.folio.reader.data.inTab
import dev.folio.reader.data.nameOf
import dev.folio.reader.data.orderedLists
import dev.folio.reader.data.parentOf
import dev.folio.reader.data.visible
import dev.folio.reader.data.HomeTab
import dev.folio.reader.data.homeTabs
import dev.folio.reader.data.mostRead
import dev.folio.reader.data.sessionsSince
import dev.folio.reader.pdf.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private fun Tab.title(): String = when (this) {
    Tab.Home -> s(R.string.home)
    Tab.ReadingNow -> s(R.string.reading_now)
    Tab.ToRead -> s(R.string.to_read)
    Tab.Finished -> s(R.string.finished)
    Tab.Favorites -> s(R.string.favorites)
    Tab.All -> s(R.string.all_books)
    is Tab.List -> nameOf(path)
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.Home -> Icons.Rounded.Home
    Tab.ReadingNow -> Icons.Rounded.AutoStories
    Tab.ToRead -> Icons.Rounded.BookmarkAdd
    Tab.Finished -> Icons.Rounded.Done
    Tab.Favorites -> Icons.Rounded.Star
    Tab.All -> Icons.Rounded.CollectionsBookmark
    is Tab.List -> Icons.Rounded.Folder
}

private fun ViewMode.icon(): ImageVector = when (this) {
    ViewMode.Grid -> Icons.Rounded.GridView
    ViewMode.Compact -> Icons.Rounded.ViewModule
    ViewMode.Large -> Icons.Rounded.ViewAgenda
    ViewMode.List -> Icons.AutoMirrored.Rounded.ViewList
    ViewMode.Details -> Icons.Rounded.ViewStream
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(model: FolioViewModel, onOpenPicker: () -> Unit, onSettings: () -> Unit, onAbout: () -> Unit, onBrowse: (list: String?, dir: String?) -> Unit) {
    val state by model.repo.state.collectAsState()
    var tab by model::libraryTab
    var query by model::libraryQuery
    var searching by model::librarySearching
    var viewMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var detailsFor by remember { mutableStateOf<String?>(null) }
    var newList by remember { mutableStateOf<NewList?>(null) }
    var manageLists by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var bulkLists by remember { mutableStateOf(false) }
    var statsSheet by remember { mutableStateOf(false) }
    var folderMenu by remember { mutableStateOf<String?>(null) }
    var addBooks by remember { mutableStateOf(false) }
    var addOpen by remember { mutableStateOf(false) }
    // The book editor ("" = a new paper book) and the sitting log.
    var editorFor by remember { mutableStateOf<String?>(null) }
    var logFor by remember { mutableStateOf<String?>(null) }
    var homeIndex by rememberSaveable { mutableStateOf(0) }
    var confirmBulkRemove by remember { mutableStateOf(false) }
    var ask by remember { mutableStateOf<ConfirmRequest?>(null) }
    ConfirmDialog(ask) { ask = null }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val shown = state.visible(tab, query)
    val heroFirst = tab == Tab.ReadingNow && query.isBlank() && shown.isNotEmpty()
    val hero = if (heroFirst) shown.first() else null
    val rest = if (heroFirst) shown.drop(1) else shown
    val shelves = listOf(Tab.Home, Tab.ReadingNow, Tab.ToRead, Tab.Finished, Tab.Favorites, Tab.All)
    val lists = state.orderedLists()
    fun toggle(id: String) { selected = if (id in selected) selected - id else selected + id; if (selected.isEmpty()) selecting = false }
    val open: (Book) -> Unit = { if (selecting) toggle(it.id) else if (it.physical) logFor = it.id else model.openBook(it.id) }
    // Holding a book shows its sheet; while picking, holding just picks it.
    val details: (Book) -> Unit = { if (selecting) toggle(it.id) else detailsFor = it.id }
    val children = (tab as? Tab.List)?.let { state.childLists(it.path) }.orEmpty()
    val showFolders = tab is Tab.List && query.isBlank() && children.isNotEmpty()

    // Back goes up one list at a time, then closes search, then leaves picking.
    androidx.activity.compose.BackHandler(enabled = tab is Tab.List) {
        val parent = parentOf((tab as Tab.List).path)
        tab = if (parent.isEmpty()) Tab.Home else Tab.List(parent)
    }
    // From any other shelf, back returns to Home; the exit question is asked only there.
    androidx.activity.compose.BackHandler(enabled = tab != Tab.Home && tab !is Tab.List) { tab = Tab.Home }
    androidx.activity.compose.BackHandler(enabled = searching) { searching = false; query = "" }
    androidx.activity.compose.BackHandler(enabled = selecting) { selecting = false; selected = emptySet() }
    // Registered last, so it wins: with the side menu open, back only closes the menu.
    androidx.activity.compose.BackHandler(enabled = drawer.currentValue == DrawerValue.Open || drawer.targetValue == DrawerValue.Open) { scope.launch { drawer.close() } }

    // A list that was deleted or renamed while it was open falls back to All.
    LaunchedEffect(lists, tab) { (tab as? Tab.List)?.let { if (it.path !in lists) tab = Tab.All } }

    androidx.compose.runtime.CompositionLocalProvider(LocalSelection provides selected) {
    ModalNavigationDrawer(
        drawerState = drawer,
        // The menu opens from the far left edge only (below), so flicking across the page never opens it; once open,
        // a tap outside or a swipe left closes it.
        gesturesEnabled = drawer.currentValue != DrawerValue.Closed || drawer.targetValue != DrawerValue.Closed,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
                        Text(s(R.string.rukn), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(s(R.string.by_zimam), style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF5A1F))
                    }
                    shelves.forEach { t ->
                        DrawerEntry(t.icon(), t.title(), if (t == Tab.Home) null else state.books.count { it.inTab(t) }, tab == t, 0) { tab = t; scope.launch { drawer.close() } }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(s(R.string.lists), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (lists.isNotEmpty()) TextButton(onClick = { scope.launch { drawer.close() }; manageLists = true }) { Text(s(R.string.manage)) }
                    }
                    // Only the top level here; the lists inside open as folders once a list is.
                    lists.filter { depthOf(it) == 0 }.forEach { path ->
                        val t = Tab.List(path)
                        val inside = (tab as? Tab.List)?.let { isInside(it.path, path) } == true
                        DrawerEntry(Icons.Rounded.Folder, nameOf(path), state.books.count { it.inTab(t, deep = false) }, inside, 0, onLongClick = { scope.launch { drawer.close() }; folderMenu = path }) { tab = t; scope.launch { drawer.close() } }
                    }
                    DrawerEntry(Icons.Rounded.CreateNewFolder, s(R.string.new_list), null, false, 0) { scope.launch { drawer.close() }; newList = NewList("", null) }
                    HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                    Text(s(R.string.files_on_this_phone), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp))
                    DrawerEntry(Icons.Rounded.FolderOpen, s(R.string.browse_files), null, false, 0) { scope.launch { drawer.close() }; onBrowse(null, null) }
                    DrawerEntry(Icons.Rounded.Download, s(R.string.downloads), null, false, 0) { scope.launch { drawer.close() }; onBrowse(null, android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath) }
                    DrawerEntry(Icons.Rounded.Description, s(R.string.documents), null, false, 0) { scope.launch { drawer.close() }; onBrowse(null, android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS).absolutePath) }
                    HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                    DrawerEntry(Icons.Rounded.Settings, s(R.string.settings_and_backup), null, false, 0) { scope.launch { drawer.close() }; onSettings() }
                    DrawerEntry(Icons.Rounded.Info, s(R.string.about_rukn), null, false, 0) { scope.launch { drawer.close() }; onAbout() }
                    DrawerEntry(Icons.Rounded.NewReleases, s(R.string.what_is_new), null, false, 0) { scope.launch { drawer.close() }; model.whatsNewRequested.value = true }
                    DrawerEntry(Icons.Rounded.Explore, s(R.string.show_the_tour_again), null, false, 0) { scope.launch { drawer.close() }; model.tourRequested.value = true }
                    Spacer(Modifier.height(24.dp))
                }
            }
        },
    ) {
      val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
      Box(
        Modifier.fillMaxSize().pointerInput(drawer, rtl) {
            // A drag towards the middle that starts in the 80dp at the menu's edge (left, or right in Arabic) opens the menu. Watching on the Initial pass means nothing
            // underneath (buttons, cards) is blocked: a tap still reaches them, and only a real sideways drag is taken.
            val edge = 80.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                val fromEdge = if (rtl) down.position.x >= size.width - edge else down.position.x <= edge
                if (!fromEdge || drawer.currentValue != DrawerValue.Closed) return@awaitEachGesture
                val dir = if (rtl) -1f else 1f
                var dx = 0f; var dy = 0f
                while (true) {
                    val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    val c = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed) break
                    dx += dir * (c.position.x - c.previousPosition.x); dy += (c.position.y - c.previousPosition.y)
                    if (kotlin.math.abs(dy) > 2 * kotlin.math.abs(dx) && kotlin.math.abs(dy) > 24f) break
                    if (dx > 36f && dx > kotlin.math.abs(dy)) { c.consume(); scope.launch { drawer.open() }; break }
                }
            }
        },
      ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (selecting) {
                    var more by remember { mutableStateOf(false) }
                    TopAppBar(
                        title = { Text(s(R.string.selected, selected.size)) },
                        navigationIcon = { IconButton(onClick = { selecting = false; selected = emptySet() }) { Icon(Icons.Rounded.Close, s(R.string.done)) } },
                        actions = {
                            IconButton(onClick = { selected = shown.map { it.id }.toSet() }) { Icon(Icons.Rounded.SelectAll, s(R.string.select_all)) }
                            IconButton(onClick = { bulkLists = true }, enabled = selected.isNotEmpty()) { Icon(Icons.Rounded.CreateNewFolder, s(R.string.add_to_a_list)) }
                            Box {
                                IconButton(onClick = { more = true }, enabled = selected.isNotEmpty()) { Icon(Icons.Rounded.MoreVert, s(R.string.more)) }
                                DropdownMenu(more, { more = false }) {
                                    (tab as? Tab.List)?.let { open ->
                                        DropdownMenuItem(
                                            text = { Text(s(R.string.remove_from, nameOf(open.path))) },
                                            leadingIcon = { Icon(Icons.Rounded.FolderOff, null) },
                                            onClick = { more = false; ask = ConfirmRequest(s(R.string.remove_books_from, selected.size, nameOf(open.path)), s(R.string.they_stay_in_your_library)) { model.repo.setInList(selected, open.path, false); selected = emptySet(); selecting = false } },
                                        )
                                    }
                                    DropdownMenuItem(text = { Text(s(R.string.add_to_favorites)) }, leadingIcon = { Icon(Icons.Rounded.Star, null) }, onClick = { more = false; model.repo.updateBooks(selected) { it.copy(favorite = true) } })
                                    DropdownMenuItem(text = { Text(s(R.string.mark_as_to_read)) }, leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, null) }, onClick = { more = false; model.repo.updateBooks(selected) { it.copy(status = Status.ToRead) } })
                                    DropdownMenuItem(text = { Text(s(R.string.mark_as_finished)) }, leadingIcon = { Icon(Icons.Rounded.CheckCircle, null) }, onClick = { more = false; model.repo.updateBooks(selected) { it.copy(status = Status.Finished) } })
                                    DropdownMenuItem(text = { Text(s(R.string.remove_from_library)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { more = false; confirmBulkRemove = true })
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    )
                } else TopAppBar(
                    title = {
                        if (searching) {
                            val focus = LocalFocusManager.current
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                placeholder = { Text(if (tab is Tab.List) s(R.string.search_in, tab.title()) else s(R.string.search_your_books)) },
                                shape = RoundedCornerShape(28.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Column {
                                Text(tab.title(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (tab == Tab.Home && query.isBlank()) s(R.string.your_reading_at_a_glance) else "${shown.size} ${if (shown.size == 1) "book" else "books"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, s(R.string.menu)) } },
                    actions = {
                        // Home has no tools of its own; the shelves and lists get search, view, sort and select.
                        if (tab != Tab.Home) {
                        IconButton(onClick = { selecting = true }) { Icon(Icons.Rounded.Checklist, s(R.string.select_books)) }
                        IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                            Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, s(R.string.search))
                        }
                        Box {
                            IconButton(onClick = { viewMenu = true }) { Icon(state.view.icon(), s(R.string.view)) }
                            DropdownMenu(viewMenu, { viewMenu = false }) {
                                ViewMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(mode.icon(), null) },
                                        text = { Text(mode.label, fontWeight = if (mode == state.view) FontWeight.SemiBold else FontWeight.Normal) },
                                        trailingIcon = { if (mode == state.view) Icon(Icons.Rounded.Check, null) },
                                        onClick = { model.repo.update { it.copy(view = mode) }; viewMenu = false },
                                    )
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, s(R.string.sort)) }
                            DropdownMenu(sortMenu, { sortMenu = false }) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.label, fontWeight = if (order == state.sort) FontWeight.SemiBold else FontWeight.Normal) },
                                        trailingIcon = { if (order == state.sort) Icon(Icons.Rounded.Check, null) },
                                        onClick = { model.repo.update { it.copy(sort = order) }; sortMenu = false },
                                    )
                                }
                            }
                        }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (tab == Tab.Home && query.isBlank()) {
                    HomeContent(model, state, lists, onOpen = open, onDetails = details, onOpenList = { tab = Tab.List(it) }, onListMenu = { folderMenu = it }, onShelf = { tab = it }, onStats = { statsSheet = true }, onFiles = { onBrowse(null, null) }, startIndex = homeIndex, onIndex = { homeIndex = it })
                } else if (shown.isEmpty() && children.isEmpty()) {
                    EmptyShelf(tab, onOpenPicker)
                } else if (state.view == ViewMode.List || state.view == ViewMode.Details) {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (tab is Tab.List) item { ListHeader(tab as Tab.List, { tab = Tab.List(it) }, { tab = Tab.All }) }
                        if (showFolders) items(children, key = { "folder:$it" }) { path -> FolderRow(state, path, { tab = Tab.List(path) }, { folderMenu = path }) }
                        if (tab == Tab.ReadingNow && query.isBlank()) item { StatsCard(state) { statsSheet = true } }
                        if (hero != null) {
                            item { HeroCard(model, hero, onOpen = { open(hero) }, onDetails = { details(hero) }) }
                            item { Spacer(Modifier.height(8.dp)) }
                        }
                        items(rest, key = { it.id }) { book ->
                            if (state.view == ViewMode.Details) DetailRow(model, book, { open(book) }, { details(book) })
                            else ListBook(model, book, { open(book) }, { details(book) })
                        }
                    }
                } else {
                    val (minSize, ratio) = when (state.view) {
                        ViewMode.Compact -> 88.dp to 0.7f
                        ViewMode.Large -> 168.dp to 0.7f
                        else -> 112.dp to 0.7f
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (tab is Tab.List) item(span = { GridItemSpan(maxLineSpan) }) { ListHeader(tab as Tab.List, { tab = Tab.List(it) }, { tab = Tab.All }) }
                        if (showFolders) items(children, key = { "folder:$it" }) { path -> FolderTile(state, path, compact = state.view == ViewMode.Compact, { tab = Tab.List(path) }, { folderMenu = path }) }
                        if (tab == Tab.ReadingNow && query.isBlank()) item(span = { GridItemSpan(maxLineSpan) }) { StatsCard(state) { statsSheet = true } }
                        if (hero != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) { HeroCard(model, hero, onOpen = { open(hero) }, onDetails = { details(hero) }) }
                            if (rest.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(s(R.string.also_reading), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        items(rest, key = { it.id }) { book ->
                            GridBook(model, book, ratio, compact = state.view == ViewMode.Compact, { open(book) }, { details(book) })
                        }
                    }
                }
            }
        }
        if (addOpen) Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { addOpen = false } })
        Box(Modifier.align(Alignment.BottomEnd).padding(16.dp).navigationBarsPadding()) {
            AddMenu(
                open = addOpen,
                onToggle = { addOpen = !addOpen },
                listPath = (tab as? Tab.List)?.path,
                onBrowse = { addOpen = false; onBrowse((tab as? Tab.List)?.path, null) },
                onPick = { addOpen = false; onOpenPicker() },
                onFromLibrary = { addOpen = false; addBooks = true },
                onNewList = { addOpen = false; newList = NewList((tab as? Tab.List)?.path.orEmpty(), null) },
                onPaperBook = { addOpen = false; editorFor = "" },
            )
        }
        // The left edge opens the side menu: a swipe that starts in this strip, going right. Android's own back gesture
        // also lives on the edge, so the middle of the strip is excluded from it (the system allows about 200dp), and
        // a small grip shows where to start; tapping the grip opens the menu too.
        // Invisible: keeps Android's own back gesture off the left edge so the menu swipe above can start there.
        Box(Modifier.align(Alignment.CenterStart).width(72.dp).height(200.dp).systemGestureExclusion())
      }
    }

    }

    detailsFor?.let { id ->
        val book = state.books.firstOrNull { it.id == id }
        if (book == null) detailsFor = null else DetailsSheet(model, book, lists, (tab as? Tab.List)?.path, onOpenBook = { detailsFor = null; if (book.physical) logFor = book.id else model.openBook(book.id) }, onEdit = { detailsFor = null; editorFor = book.id }, onNewList = { newList = NewList("", book.id) }, onDismiss = { detailsFor = null })
    }
    newList?.let { request ->
        ListDialog(lists, request.parent, onDismiss = { newList = null }) { name, parent ->
            if (model.repo.addList(name, parent)) {
                val path = if (parent.isEmpty()) name.trim() else "$parent/${name.trim()}"
                request.forBook?.let { model.repo.toggleList(it, path) }
                if (request.forBooks.isNotEmpty()) { model.repo.setInList(request.forBooks, path, true) }
                if (request.forBook == null && request.forBooks.isEmpty()) tab = Tab.List(path)
            }
            newList = null
        }
    }
    editorFor?.let { id ->
        BookEditor(model, state.books.firstOrNull { it.id == id }, onDismiss = { editorFor = null })
    }
    logFor?.let { id ->
        val b = state.books.firstOrNull { it.id == id }
        if (b == null) logFor = null else LogSitting(model, b, onDismiss = { logFor = null })
    }
    folderMenu?.let { path ->
        if (path !in lists) folderMenu = null else FolderSheet(
            model, state, path,
            onOpen = { folderMenu = null; tab = Tab.List(path) },
            onNewInside = { folderMenu = null; newList = NewList(path, null) },
            onDismiss = { folderMenu = null },
        )
    }
    (tab as? Tab.List)?.let { open ->
        if (addBooks) AddBooksSheet(state.books.filter { open.path !in it.lists }, open.path, onAdd = { ids -> model.repo.setInList(ids, open.path, true); addBooks = false }, onDismiss = { addBooks = false })
    }
    if (statsSheet) StatsSheet(state) { statsSheet = false }
    if (bulkLists) BulkListSheet(
        state.books.filter { it.id in selected },
        lists,
        onSet = { list, add -> model.repo.setInList(selected, list, add) },
        onNew = { bulkLists = false; newList = NewList("", null, selected) },
        onDismiss = { bulkLists = false },
    )
    if (confirmBulkRemove) AlertDialog(
        onDismissRequest = { confirmBulkRemove = false },
        title = { Text(s(R.string.remove_books, selected.size)) },
        text = { Text(s(R.string.the_files_stay_where_they)) },
        confirmButton = { TextButton(onClick = { selected.forEach { model.covers.forget(it) }; model.repo.removeBooks(selected); selected = emptySet(); selecting = false; confirmBulkRemove = false }) { Text(s(R.string.remove)) } },
        dismissButton = { TextButton(onClick = { confirmBulkRemove = false }) { Text(s(R.string.cancel)) } },
    )
    if (manageLists) ManageListsSheet(model, lists, onNewIn = { newList = NewList(it, null) }, onDismiss = { manageLists = false })
}

private val LocalSelection = androidx.compose.runtime.compositionLocalOf { emptySet<String>() }

@Composable
private fun cover(model: FolioViewModel, book: Book): ImageBitmap? =
    produceState<ImageBitmap?>(model.covers.peek(book)?.asImageBitmap(), book.id, book.coverRev) { value = model.covers.get(book)?.asImageBitmap() ?: value.takeIf { book.coverRev == 0 } }.value

@Composable
private fun Cover(model: FolioViewModel, book: Book, modifier: Modifier = Modifier, marks: Boolean = true) {
    val image = cover(model, book)
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        if (image != null) {
            Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        } else {
            Box(Modifier.fillMaxSize().background(placeholderColor(book.name)), contentAlignment = Alignment.Center) {
                if (book.kind == dev.folio.reader.data.Kind.Pdf || book.physical) {
                    Text(book.name.take(1).uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text(dev.folio.reader.data.FileTypes.badge(book.fileName.ifEmpty { book.name }, book.kind), color = Color.White.copy(alpha = 0.92f), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        if (book.id in LocalSelection.current) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CheckCircle, s(R.string.selected_label), tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        if (marks && book.favorite) {
            Icon(Icons.Rounded.Star, null, tint = Color(0xFFF2B12E), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
        }
        if (marks && book.draftMarks > 0) {
            Text(s(R.string.draft), Modifier.align(Alignment.BottomEnd).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFF5A1F)).padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        if (marks && book.status == Status.Finished) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(20.dp).background(Color(0xFF2E7D5B), androidx.compose.foundation.shape.CircleShape))
        }
    }
}

private fun placeholderColor(name: String): Color {
    val palette = listOf(0xFFB54A4A, 0xFF4A7FB5, 0xFF4A9E7A, 0xFF9A6BB5, 0xFFC08A3E, 0xFF5B6B8C)
    return Color(palette[(name.hashCode() and Int.MAX_VALUE) % palette.size])
}

/** A thin bar and the percentage read, on every book (empty until it is opened). */
@Composable
private fun ReadingProgress(book: Book, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)))
        Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Stars(rating: Int, size: Int = 14, onRate: ((Int) -> Unit)? = null) {
    Row {
        for (i in 1..5) {
            val filled = i <= rating
            val icon = Icon(
                if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                if (onRate != null) s(R.string.n_stars, i) else null,
                Modifier.size(size.dp).let { if (onRate != null) it.clickable { onRate(if (i == rating) 0 else i) } else it },
                tint = if (filled) Color(0xFFF2B12E) else MaterialTheme.colorScheme.outline,
            )
            icon
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(model: FolioViewModel, book: Book, onOpen: () -> Unit, onDetails: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onDetails),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Cover(model, book, Modifier.width(96.dp).aspectRatio(0.68f), marks = false)
            Column(Modifier.weight(1f).height(140.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(s(R.string.continue_reading), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(book.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(
                        book.placeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (book.rating > 0) Stars(book.rating)
                }
                Column {
                    ReadingProgress(book)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOpen) { Icon(Icons.Rounded.AutoStories, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(s(R.string.continue_action)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridBook(model: FolioViewModel, book: Book, ratio: Float, compact: Boolean, onOpen: () -> Unit, onDetails: () -> Unit) {
    Column(Modifier.combinedClickable(onClick = onOpen, onLongClick = onDetails)) {
        Cover(model, book, Modifier.fillMaxWidth().aspectRatio(ratio))
        Spacer(Modifier.height(8.dp))
        Text(book.name, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!compact) {
            if (book.rating > 0) Stars(book.rating, 12)
            else Text(if (book.status == Status.Finished) s(R.string.finished) else book.countLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        ReadingProgress(book)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBook(model: FolioViewModel, book: Book, onOpen: () -> Unit, onDetails: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onOpen, onLongClick = onDetails).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Cover(model, book, Modifier.width(54.dp).aspectRatio(0.7f))
        Column(Modifier.weight(1f)) {
            Text(book.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                book.placeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            ReadingProgress(book)
        }
        IconButton(onClick = onDetails) { Icon(Icons.Rounded.MoreVert, s(R.string.details)) }
    }
}

/** Everything at a glance: cover, stars, pages, size, last opened and the note. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailRow(model: FolioViewModel, book: Book, onOpen: () -> Unit, onDetails: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onDetails),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Cover(model, book, Modifier.width(72.dp).aspectRatio(0.7f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(book.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Stars(book.rating, 15)
                Text(
                    "${book.countLabel}  ·  ${formatSize(book.size)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (book.lastOpened > 0) s(R.string.opened_date, formatDate(book.lastOpened)) else s(R.string.not_opened_yet),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (book.note.isNotBlank()) Text(book.note, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                ReadingProgress(book)
            }
        }
    }
}

@Composable
private fun EmptyShelf(tab: Tab, onOpen: () -> Unit) {
    val (title, hint) = when (tab) {
        Tab.Home -> s(R.string.welcome) to s(R.string.open_a_file_to_start)
        Tab.ReadingNow -> s(R.string.nothing_here_yet) to s(R.string.books_you_open_show_up)
        Tab.ToRead -> s(R.string.your_to_read_shelf_is) to s(R.string.press_and_hold_a_book)
        Tab.Finished -> s(R.string.no_finished_books) to s(R.string.books_you_reach_the_end)
        Tab.Favorites -> s(R.string.no_favorites_yet) to s(R.string.press_and_hold_a_book_cec3)
        Tab.All -> s(R.string.no_books_yet) to s(R.string.open_a_pdf_to_start)
        is Tab.List -> s(R.string.this_list_is_empty) to s(R.string.press_and_hold_a_book_f1e6)
    }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.AutoStories, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onOpen) { Text(s(R.string.open_a_pdf)) }
    }
}

@Composable
private fun DrawerEntry(icon: ImageVector, label: String, count: Int?, selected: Boolean, depth: Int, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        badge = { if (count != null) Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = (depth * 16).dp)
            .then(if (onLongClick == null) Modifier else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)),
    )
}

// ---- details of one book ------------------------------------------------------------------

private enum class SheetSection(@StringRes val labelRes: Int) { Actions(R.string.actions), Details(R.string.details), Notes(R.string.notes) }

private val SheetSection.label: String get() = s(labelRes)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(model: FolioViewModel, book: Book, lists: List<String>, openList: String?, onOpenBook: () -> Unit, onEdit: () -> Unit, onNewList: () -> Unit, onDismiss: () -> Unit) {
    var section by remember { mutableStateOf(SheetSection.Actions) }
    var confirmRemove by remember { mutableStateOf(false) }
    val sessionsN = model.repo.state.collectAsState().value.sessionCount(book.id)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Cover(model, book, Modifier.width(64.dp).aspectRatio(0.7f))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(book.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    ReadingProgress(book)
                }
            }
            Spacer(Modifier.height(14.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                SheetSection.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = section == s,
                        onClick = { section = s },
                        shape = SegmentedButtonDefaults.itemShape(i, SheetSection.entries.size),
                        label = { Text(s.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(380.dp)) {
                when (section) {
                    SheetSection.Actions -> ActionsSection(model, book, lists, openList, onOpenBook, onEdit, onNewList, onDismiss) { confirmRemove = true }
                    SheetSection.Details -> DetailsSection(book, sessionsN, onEdit)
                    SheetSection.Notes -> NotesSection(model, book)
                }
            }
        }
    }
    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text(s(R.string.remove_from_library_4769)) },
        text = { Text(s(R.string.the_pdf_file_stays_where)) },
        confirmButton = { TextButton(onClick = { model.remove(book.id); confirmRemove = false; onDismiss() }) { Text(s(R.string.remove)) } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(s(R.string.cancel)) } },
    )
}

@Composable
private fun ActionsSection(model: FolioViewModel, book: Book, lists: List<String>, openList: String?, onOpenBook: () -> Unit, onEdit: () -> Unit, onNewList: () -> Unit, onDismiss: () -> Unit, onRemove: () -> Unit) {
    // 0 closed, 1 add to another list, 2 move to another list
    var picker by remember { mutableStateOf(0) }
    var ask by remember { mutableStateOf<ConfirmRequest?>(null) }
    ConfirmDialog(ask) { ask = null }
    val here = openList?.takeIf { it in book.lists }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SheetRow(Icons.Rounded.AutoStories, if (book.physical) s(R.string.log_a_reading_session) else s(R.string.open), onClick = onOpenBook)
        SheetRow(Icons.Rounded.Edit, s(R.string.edit_details_and_cover), onClick = onEdit)
        // Inside a list: take the book out of it, or carry it to another list (it stays in the library).
        if (here != null) {
            SheetRow(Icons.Rounded.FolderOff, s(R.string.remove_from, nameOf(here))) { ask = ConfirmRequest(s(R.string.remove_from_01d2, nameOf(here)), s(R.string.the_book_stays_in_your)) { model.repo.toggleList(book.id, here); onDismiss() } }
            SheetRow(Icons.AutoMirrored.Rounded.DriveFileMove, if (picker == 2) s(R.string.move_to_tap_to_close) else s(R.string.move_to_another_list)) { picker = if (picker == 2) 0 else 2 }
        }
        SheetRow(Icons.Rounded.Folder, if (picker == 1) s(R.string.lists_tap_to_close) else if (here != null) s(R.string.also_add_to_another_list) else s(R.string.add_to_a_list_2f02)) { picker = if (picker == 1) 0 else 1 }
        if (picker != 0) {
            lists.filter { picker == 1 || it != here }.forEach { list ->
                val on = list in book.lists
                SheetRow(if (on) Icons.Rounded.Check else Icons.Rounded.Folder, nameOf(list), highlighted = on, indent = depthOf(list) + 1) {
                    if (picker == 2 && here != null) {
                        model.repo.toggleList(book.id, here)
                        if (!on) model.repo.toggleList(book.id, list)
                        onDismiss()
                    } else model.repo.toggleList(book.id, list)
                }
            }
            SheetRow(Icons.Rounded.CreateNewFolder, s(R.string.new_list_f13d), indent = 1, onClick = onNewList)
        }
        SheetRow(if (book.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (book.favorite) s(R.string.remove_from_favorites) else s(R.string.add_to_favorites)) {
            model.repo.updateBook(book.id) { it.copy(favorite = !it.favorite) }
        }
        SheetRow(Icons.Rounded.BookmarkAdd, if (book.status == Status.ToRead) s(R.string.remove_from_to_read) else s(R.string.to_read)) {
            model.repo.updateBook(book.id) { it.copy(status = if (it.status == Status.ToRead) Status.Reading else Status.ToRead) }
        }
        SheetRow(Icons.Rounded.CheckCircle, if (book.status == Status.Finished) s(R.string.mark_as_reading) else s(R.string.mark_as_finished)) {
            model.repo.updateBook(book.id) { it.copy(status = if (it.status == Status.Finished) Status.Reading else Status.Finished) }
        }
        SheetRow(Icons.Rounded.Delete, s(R.string.remove_from_library), onClick = onRemove)
    }
}

/** The facts, one line each; tap a row to read all of a long value. */
@Composable
private fun DetailsSection(book: Book, sessions: Int, onEdit: () -> Unit) {
    val context = LocalContext.current
    val facts by produceState<List<Pair<String, String>>?>(null, book.id) {
        value = withContext(Dispatchers.IO) {
            val out = ArrayList<Pair<String, String>>()
            if (book.physical) return@withContext out
            val uri = Uri.parse(book.uri)
            PdfFile.open(context, uri)?.use { pdf -> pdf.sizeOf(0)?.let { (w, h) -> out += s(R.string.page_size) to pageSize(w, h) } }
            out += PdfInfo.read(context, uri)
            out
        }
    }
    val rows = buildList {
        // Where you are in the book comes first: the page, the time you have given it and the sittings.
        add((if (book.kind == dev.folio.reader.data.Kind.Epub) s(R.string.reading_chapter) else s(R.string.reading_page)) to if (book.progress > 0f || book.page > 0) book.placeLabel else s(R.string.not_started))
        add(s(R.string.total_time_read) to if (book.readSeconds > 0) minutesLabel(book.readSeconds) else s(R.string.none_yet))
        add(s(R.string.sessions) to sessions.toString())
        if (book.author.isNotBlank()) add(s(R.string.author) to book.author)
        if (book.publisher.isNotBlank()) add(s(R.string.publisher) to book.publisher)
        if (book.year.isNotBlank()) add(s(R.string.year) to book.year)
        if (book.isbn.isNotBlank()) add("ISBN" to book.isbn)
        if (book.description.isNotBlank()) add(s(R.string.description) to book.description)
        if (book.physical) add(s(R.string.type) to s(R.string.paper_book)) else add(s(R.string.file_name) to book.fileName.ifEmpty { book.name })
        add(when (book.kind) { dev.folio.reader.data.Kind.Pdf, dev.folio.reader.data.Kind.Comic -> s(R.string.pages); dev.folio.reader.data.Kind.Epub -> s(R.string.chapters); dev.folio.reader.data.Kind.Text -> s(R.string.lines) } to book.pages.toString())
        if (!book.physical) add(s(R.string.type) to dev.folio.reader.data.FileTypes.badge(book.fileName.ifEmpty { book.name }, book.kind))
        if (!book.physical) add(s(R.string.size) to formatSize(book.size))
        addAll(facts.orEmpty())
        if (book.added > 0) add(s(R.string.added) to formatDate(book.added))
        add(s(R.string.last_opened) to if (book.lastOpened > 0) formatDate(book.lastOpened) else s(R.string.never))
        add(s(R.string.status) to when (book.status) { Status.New -> s(R.string.new_label); Status.Reading -> s(R.string.reading); Status.ToRead -> s(R.string.to_read); Status.Finished -> s(R.string.finished) })
        if (book.lists.isNotEmpty()) add(s(R.string.lists) to book.lists.joinToString(", ") { it.replace("/", " › ") })
        if (!book.physical) add(s(R.string.location) to friendlyLocation(book.uri))
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
        rows.forEach { (k, v) -> FactRow(k, v) }
        TextButton(onClick = onEdit, Modifier.padding(horizontal = 16.dp)) { Text(s(R.string.edit_these_details)) }
        if (facts == null) Text(s(R.string.reading_the_file), Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotesSection(model: FolioViewModel, book: Book) {
    var editing by remember { mutableStateOf<dev.folio.reader.data.PageNote?>(null) }
    var ask by remember { mutableStateOf<ConfirmRequest?>(null) }
    ConfirmDialog(ask) { ask = null }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(s(R.string.your_rating), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stars(book.rating, 34) { model.repo.setRating(book.id, it) }
            if (book.rating > 0) TextButton(onClick = { ask = ConfirmRequest(s(R.string.clear_your_rating), s(R.string.the_stars_on_this_book), s(R.string.clear)) { model.repo.setRating(book.id, 0) } }) { Text(s(R.string.clear)) }
        }
        Text(s(R.string.note_about_this_book), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        var note by remember(book.id) { mutableStateOf(book.note) }
        OutlinedTextField(note, { note = it; model.repo.setNote(book.id, it) }, placeholder = { Text(s(R.string.write_a_note)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
        Text(s(R.string.page_comments, book.comments.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        if (book.comments.isEmpty()) Text(s(R.string.add_them_while_reading_with), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        book.comments.forEach { c ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { editing = c }.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(s(R.string.page, c.page + 1), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(c.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { ask = ConfirmRequest(s(R.string.delete_this_comment), s(R.string.the_comment_on_page_will, c.page + 1), s(R.string.delete)) { model.repo.deleteComment(book.id, c.id) } }) { Icon(Icons.Rounded.Delete, s(R.string.delete_comment)) }
            }
        }
    }
    editing?.let { c ->
        var text by remember(c.id) { mutableStateOf(c.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(s(R.string.comment_on_page, c.page + 1)) },
            text = { OutlinedTextField(text, { text = it }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { model.repo.saveComment(book.id, c.page, text, c.id); editing = null }, enabled = text.isNotBlank()) { Text(s(R.string.save)) } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(s(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 6.dp))
}

@Composable
private fun FactRow(label: String, value: String) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(label, Modifier.width(112.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, highlighted: Boolean = false, indent: Int = 0, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = (24 + indent * 20).dp, end = 24.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(icon, null, tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

private data class NewList(val parent: String, val forBook: String?, val forBooks: Set<String> = emptySet())

// ---- managing lists -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageListsSheet(model: FolioViewModel, lists: List<String>, onNewIn: (String) -> Unit, onDismiss: () -> Unit) {
    var renaming by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var movingList by remember { mutableStateOf<String?>(null) }
    val library by model.repo.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(s(R.string.manage_lists), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text(s(R.string.use_the_arrows_to_change), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            lists.forEach { path ->
                var menu by remember { mutableStateOf(false) }
                val siblings = lists.filter { parentOf(it) == parentOf(path) }
                Row(Modifier.fillMaxWidth().padding(start = (16 + depthOf(path) * 20).dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                    Text(nameOf(path), Modifier.weight(1f).padding(vertical = 14.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { model.repo.moveList(path, true) }, enabled = siblings.firstOrNull() != path) { Icon(Icons.Rounded.ArrowUpward, s(R.string.move_up)) }
                    IconButton(onClick = { model.repo.moveList(path, false) }, enabled = siblings.lastOrNull() != path) { Icon(Icons.Rounded.ArrowDownward, s(R.string.move_down)) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, s(R.string.more)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text(s(R.string.rename)) }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; renaming = path })
                            DropdownMenuItem(text = { Text(s(R.string.move_to)) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, null) }, onClick = { menu = false; movingList = path })
                            DropdownMenuItem(text = { Text(s(R.string.new_list_inside)) }, leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null) }, onClick = { menu = false; onNewIn(path) })
                            DropdownMenuItem(text = { Text(s(R.string.delete)) }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; deleting = path })
                        }
                    }
                }
            }
        }
    }
    movingList?.let { path -> MoveListDialog(library, path, onDismiss = { movingList = null }) { parent -> model.repo.moveListTo(path, parent); movingList = null } }
    renaming?.let { path ->
        NameDialog(s(R.string.rename_list), nameOf(path), s(R.string.rename), onDismiss = { renaming = null }) { name ->
            model.repo.renameList(path, name)
            renaming = null
        }
    }
    deleting?.let { path ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(s(R.string.delete_9cdd, nameOf(path))) },
            text = { Text(s(R.string.the_list_and_any_lists)) },
            confirmButton = { TextButton(onClick = { model.repo.deleteList(path); deleting = null }) { Text(s(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(s(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ListDialog(lists: List<String>, initialParent: String, onDismiss: () -> Unit, onDone: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf(initialParent) }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(R.string.new_list)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.replace("/", "") }, singleLine = true, placeholder = { Text(s(R.string.name)) }, modifier = Modifier.fillMaxWidth())
                Box {
                    TextButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.Folder, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (parent.isEmpty()) s(R.string.top_level) else s(R.string.inside) + parent.replace("/", " › "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text(s(R.string.top_level)) }, onClick = { parent = ""; menu = false })
                        lists.forEach { path ->
                            DropdownMenuItem(text = { Text(nameOf(path), modifier = Modifier.padding(start = (depthOf(path) * 14).dp)) }, onClick = { parent = path; menu = false })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(name, parent) }, enabled = name.isNotBlank()) { Text(s(R.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.replace("/", "") }, singleLine = true, placeholder = { Text(s(R.string.name)) }) },
        confirmButton = { TextButton(onClick = { onDone(name) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

// ---- formatting ---------------------------------------------------------------------------

internal fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> s(R.string.unknown_size)
    bytes < 1024 * 1024 -> "${(bytes / 1024.0).let { "%.0f".format(it) }} KB"
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / 1048576.0)
    else -> "%.2f GB".format(bytes / 1073741824.0)
}

internal fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

/** "210 × 297 mm (A4)" from points. */
private fun pageSize(w: Float, h: Float): String {
    val mmW = w / 72f * 25.4f
    val mmH = h / 72f * 25.4f
    val known = listOf("A3" to (297f to 420f), "A4" to (210f to 297f), "A5" to (148f to 210f), "Letter" to (215.9f to 279.4f), "Legal" to (215.9f to 355.6f))
    val name = known.firstOrNull { (_, s) ->
        val (a, b) = if (mmW <= mmH) s else s.second to s.first
        kotlin.math.abs(a - mmW) < 3f && kotlin.math.abs(b - mmH) < 3f
    }?.first
    return "%.0f × %.0f mm".format(mmW, mmH) + (name?.let { " ($it)" } ?: "")
}

// ---- lists as folders, stats, bulk add ------------------------------------------------------

/** Breadcrumb and the lists inside this list, as folders to step into. */
@Composable
private fun ListHeader(tab: Tab.List, onOpen: (String) -> Unit, onRoot: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        val parts = tab.path.split('/')
        androidx.compose.foundation.lazy.LazyRow(verticalAlignment = Alignment.CenterVertically) {
            item { Text(s(R.string.lists), Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRoot).padding(horizontal = 6.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
            parts.forEachIndexed { i, part ->
                item { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    val path = parts.take(i + 1).joinToString("/")
                    Text(
                        part,
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = i < parts.lastIndex) { onOpen(path) }.padding(horizontal = 6.dp, vertical = 6.dp),
                        color = if (i < parts.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (i == parts.lastIndex) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** "Phone › Download › هوس القراءة.pdf" instead of an encoded address. */
internal fun friendlyLocation(uri: String): String {
    val decoded = Uri.decode(uri)
    return when {
        decoded.startsWith("file://") -> decoded.removePrefix("file://").replace("/storage/emulated/0", s(R.string.phone)).replace("/storage/", "").trim('/').split('/').filter { it.isNotEmpty() }.joinToString(" › ")
        decoded.startsWith("content://com.android.providers.downloads") -> s(R.string.downloads_opened_with_the_file)
        decoded.startsWith("content://com.android.externalstorage") -> s(R.string.phone_storage_opened_with_the)
        decoded.contains("/document/") -> s(R.string.opened_with_the_system_file)
        else -> decoded
    }
}

private fun minutesLabel(seconds: Long): String {
    if (seconds < 60) return s(R.string.n_seconds, seconds)
    val m = seconds / 60
    return if (m < 60) s(R.string.n_minutes, m) else s(R.string.n_hours_minutes, m / 60, m % 60)
}

/** Home: today's minutes and pages, this week, and the streak. */
@Composable
private fun StatsCard(state: dev.folio.reader.data.LibraryState, onClick: () -> Unit) {
    val today = state.day(0)
    val week = state.lastDays(7)
    val streak = state.streak()
    Card(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Stat(s(R.string.today), minutesLabel(today.seconds), sp(R.plurals.pages_n, today.pages), Modifier.weight(1f))
            Stat(s(R.string.this_week), minutesLabel(week.seconds), sp(R.plurals.pages_n, week.pages), Modifier.weight(1f))
            Stat(s(R.string.streak), sp(R.plurals.days_n, streak), if (streak > 0) s(R.string.keep_going) else s(R.string.read_a_minute), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, note: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
        Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
    }
}

/** The last seven days as bars, and the totals. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsSheet(state: dev.folio.reader.data.LibraryState, onDismiss: () -> Unit) {
    val days = (6 downTo 0).map { state.day(it) to it }
    val top = days.maxOf { it.first.seconds }.coerceAtLeast(60)
    val total = state.days.values.fold(0L) { a, d -> a + d.seconds }
    val totalPages = state.days.values.sumOf { it.pages }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(s(R.string.your_reading), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                days.forEach { (d, back) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Text("${d.seconds / 60}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.fillMaxWidth().height((80f * d.seconds / top).coerceAtLeast(3f).dp).clip(RoundedCornerShape(6.dp)).background(if (back == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)))
                        Text(if (back == 0) s(R.string.today) else java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(java.util.Date(System.currentTimeMillis() - back * 86_400_000L)), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Text(s(R.string.minutes_per_day), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FactRow(s(R.string.all_time), minutesLabel(total))
            FactRow(s(R.string.pages_read), totalPages.toString())
            FactRow(s(R.string.books_finished), state.books.count { it.status == Status.Finished }.toString())
            FactRow(s(R.string.longest_streak), sp(R.plurals.days_now, state.streak()))
        }
    }
}

/** Add or remove every picked book from lists in one tap. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkListSheet(picked: List<Book>, lists: List<String>, onSet: (String, Boolean) -> Unit, onNew: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text(s(R.string.add_books_to, picked.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            lists.forEach { list ->
                val have = picked.count { list in it.lists }
                val all = have == picked.size && picked.isNotEmpty()
                SheetRow(
                    if (all) Icons.Rounded.Check else if (have > 0) Icons.Rounded.Remove else Icons.Rounded.Folder,
                    nameOf(list) + if (have > 0 && !all) s(R.string.of, have, picked.size) else "",
                    highlighted = have > 0,
                    indent = depthOf(list),
                ) { onSet(list, !all) }
            }
            SheetRow(Icons.Rounded.CreateNewFolder, s(R.string.new_list_f13d), onClick = onNew)
        }
    }
}

/** A list as a folder tile, the size of a book cover, in the grid. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderTile(state: dev.folio.reader.data.LibraryState, path: String, compact: Boolean, onOpen: () -> Unit, onMenu: () -> Unit) {
    val books = state.books.count { it.inTab(Tab.List(path), deep = false) }
    val inner = state.childLists(path).size
    Column(Modifier.clip(RoundedCornerShape(14.dp)).combinedClickable(onClick = onOpen, onLongClick = onMenu)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Folder, null, Modifier.fillMaxSize(0.5f), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(8.dp))
        Text(nameOf(path), style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!compact) Text(folderSummary(books, inner), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A list as a folder row, in the list views. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(state: dev.folio.reader.data.LibraryState, path: String, onOpen: () -> Unit, onMenu: () -> Unit) {
    val books = state.books.count { it.inTab(Tab.List(path), deep = false) }
    val inner = state.childLists(path).size
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onOpen, onLongClick = onMenu).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.width(54.dp).aspectRatio(0.7f).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(nameOf(path), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(folderSummary(books, inner), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun folderSummary(books: Int, lists: Int): String =
    listOfNotNull(
        if (lists > 0) "$lists ${if (lists == 1) "list" else "lists"}" else null,
        "$books ${if (books == 1) "book" else "books"}",
    ).joinToString("  ·  ")

/** Hold a folder: open it, rename it, move it into another list, add a list inside it, or delete it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderSheet(model: FolioViewModel, state: dev.folio.reader.data.LibraryState, path: String, onOpen: () -> Unit, onNewInside: () -> Unit, onDismiss: () -> Unit) {
    var renaming by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val books = state.books.count { it.inTab(Tab.List(path), deep = false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(nameOf(path), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(folderSummary(books, state.childLists(path).size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (parentOf(path).isNotEmpty()) Text(s(R.string.in_text) + parentOf(path).replace("/", " › "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SheetRow(Icons.Rounded.AutoStories, s(R.string.open), onClick = onOpen)
            SheetRow(Icons.Rounded.Edit, s(R.string.rename)) { renaming = true }
            SheetRow(Icons.AutoMirrored.Rounded.DriveFileMove, s(R.string.move_to_another_list)) { moving = true }
            SheetRow(Icons.Rounded.CreateNewFolder, s(R.string.new_list_inside), onClick = onNewInside)
            SheetRow(Icons.Rounded.Delete, s(R.string.delete_this_list)) { deleting = true }
        }
    }
    if (renaming) NameDialog(s(R.string.rename_list), nameOf(path), s(R.string.rename), onDismiss = { renaming = false }) { name ->
        model.repo.renameList(path, name); renaming = false; onDismiss()
    }
    if (moving) MoveListDialog(state, path, onDismiss = { moving = false }) { parent ->
        model.repo.moveListTo(path, parent); moving = false; onDismiss()
    }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text(s(R.string.delete_9cdd, nameOf(path))) },
        text = { Text(s(R.string.the_list_and_any_lists)) },
        confirmButton = { TextButton(onClick = { model.repo.deleteList(path); deleting = false; onDismiss() }) { Text(s(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { deleting = false }) { Text(s(R.string.cancel)) } },
    )
}

/** Choose where a list goes: the top level or inside any list that is not itself or one of its own. */
@Composable
private fun MoveListDialog(state: dev.folio.reader.data.LibraryState, path: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val targets = state.moveTargets(path)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s(R.string.move_to_ebad, nameOf(path))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (targets.isEmpty()) Text(s(R.string.there_is_nowhere_else_to), color = MaterialTheme.colorScheme.onSurfaceVariant)
                targets.forEach { target ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(target) }.padding(start = (depthOf(target) * 16).dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (target.isEmpty()) s(R.string.top_level) else nameOf(target))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(s(R.string.cancel)) } },
    )
}

/** The round + : inside a list it adds books or a list there, elsewhere it opens a file or creates a list. */
@Composable
private fun AddMenu(open: Boolean, onToggle: () -> Unit, listPath: String?, onBrowse: () -> Unit, onPick: () -> Unit, onFromLibrary: () -> Unit, onNewList: () -> Unit, onPaperBook: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = open) { onToggle() }
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (open) 135f else 0f, androidx.compose.animation.core.tween(220), label = "addTurn",
    )
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        androidx.compose.animation.AnimatedVisibility(
            open,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(220)) { it / 3 } +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.tween(220), initialScale = 0.85f),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(140)) +
                androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(160)) { it / 3 } +
                androidx.compose.animation.scaleOut(androidx.compose.animation.core.tween(160), targetScale = 0.85f),
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (listPath != null) MenuAction(Icons.Rounded.AutoStories, s(R.string.add_books_from_the_library), onFromLibrary)
                MenuAction(Icons.Rounded.FolderOpen, if (listPath != null) s(R.string.browse_files_to_add) else s(R.string.browse_files), onBrowse)
                MenuAction(Icons.Rounded.UploadFile, s(R.string.pick_a_file_with_the), onPick)
                MenuAction(Icons.Rounded.CreateNewFolder, if (listPath != null) s(R.string.new_list_here) else s(R.string.new_list), onNewList)
                MenuAction(Icons.Rounded.AutoStories, s(R.string.add_a_paper_book), onPaperBook)
            }
        }
        androidx.compose.material3.FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) { Icon(Icons.Rounded.Add, if (open) s(R.string.close) else s(R.string.add_label), Modifier.graphicsLayer { rotationZ = turn }) }
    }
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.material3.ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, null) },
        text = { Text(label) },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/** Pick books already in the library to put into the open list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBooksSheet(candidates: List<Book>, listPath: String, onAdd: (Set<String>) -> Unit, onDismiss: () -> Unit) {
    var chosen by remember { mutableStateOf(setOf<String>()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Text(s(R.string.add_to, nameOf(listPath)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            if (candidates.isEmpty()) Text(s(R.string.every_book_in_your_library), Modifier.padding(horizontal = 24.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                candidates.forEach { b ->
                    val on = b.id in chosen
                    Row(
                        Modifier.fillMaxWidth().clickable { chosen = if (on) chosen - b.id else chosen + b.id }.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        androidx.compose.material3.Checkbox(on, { chosen = if (on) chosen - b.id else chosen + b.id })
                        Column(Modifier.weight(1f)) {
                            Text(b.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(b.countLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Button(onClick = { onAdd(chosen) }, enabled = chosen.isNotEmpty(), modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()) { Text(s(R.string.add, chosen.size)) }
        }
    }
}

// ---- the home page ------------------------------------------------------------------------------

/** The home page: a few real tabs under a sticky header; flick sideways to move between them, round and round. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    model: FolioViewModel,
    state: dev.folio.reader.data.LibraryState,
    lists: List<String>,
    onOpen: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    onOpenList: (String) -> Unit,
    onListMenu: (String) -> Unit,
    onShelf: (Tab) -> Unit,
    onStats: () -> Unit,
    onFiles: () -> Unit,
    startIndex: Int,
    onIndex: (Int) -> Unit,
) {
    val tabs = state.homeTabs().filter { it.visible }.map { it.tab }
    val n = tabs.size
    val start = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % n + startIndex.coerceIn(0, n - 1)
    val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = start) { Int.MAX_VALUE }
    val here = pager.currentPage % n
    LaunchedEffect(here) { onIndex(here) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        // The header stays put while the page under it scrolls or flicks.
        androidx.compose.material3.PrimaryTabRow(selectedTabIndex = here, containerColor = MaterialTheme.colorScheme.background) {
            tabs.forEachIndexed { i, t ->
                androidx.compose.material3.Tab(
                    selected = i == here,
                    onClick = {
                        // The nearest copy of that page, so the header never spins the whole way round.
                        val base = pager.currentPage - here
                        val target = listOf(base + i - n, base + i, base + i + n).minBy { kotlin.math.abs(it - pager.currentPage) }
                        scope.launch { pager.animateScrollToPage(target) }
                    },
                    text = { Text(t.label, fontWeight = if (i == here) FontWeight.SemiBold else FontWeight.Normal) },
                )
            }
        }
        androidx.compose.foundation.pager.HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 0) { index ->
            val recent = state.books.filter { it.lastOpened > 0 }.sortedByDescending { it.lastOpened }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (tabs[index % n]) {
                    HomeTab.Home -> {
                        item(key = "greeting") { HomeGreeting(model, state, onShelf, onStats, onFiles) }
                        if (state.books.isEmpty()) item { EmptyShelf(Tab.Home, {}) }
                        recent.firstOrNull { it.status != Status.Finished }?.let { b -> item(key = "continue") { HeroCard(model, b, onOpen = { onOpen(b) }, onDetails = { onDetails(b) }) } }
                        if (recent.isNotEmpty()) item(key = "latest") {
                            Column {
                                HomeTitle(s(R.string.pick_up_where_you_left), s(R.string.reading_now)) { onShelf(Tab.ReadingNow) }
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(recent.take(3), key = { it.id }) { b -> Box(Modifier.width(120.dp)) { GridBook(model, b, 0.7f, compact = false, { onOpen(b) }, { onDetails(b) }) } }
                                }
                            }
                        }
                    }
                    HomeTab.Stats -> {
                        item(key = "stats") { HomeStats(state, onStats) }
                        val top = state.mostRead(7, 4)
                        if (top.isNotEmpty()) item(key = "mostread") {
                            Panel2(s(R.string.most_read_this_week)) {
                                val max = top.first().second.coerceAtLeast(1)
                                top.forEach { (b, seconds) ->
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpen(b) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Cover(model, b, Modifier.width(36.dp).aspectRatio(0.7f), marks = false)
                                        Column(Modifier.weight(1f)) {
                                            Text(b.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(4.dp))
                                            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                                Box(Modifier.fillMaxWidth(seconds.toFloat() / max).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.primary))
                                            }
                                        }
                                        Text(minutesLabel(seconds), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        val sittings = state.sessions.takeLast(6).reversed()
                        if (sittings.isNotEmpty()) item(key = "sittings") {
                            Panel2(s(R.string.recent_sittings)) {
                                sittings.forEach { sess ->
                                    val b = state.books.firstOrNull { it.id == sess.bookId }
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(b?.name ?: s(R.string.a_removed_book), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(formatDate(sess.start), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(minutesLabel(sess.seconds), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                    HomeTab.Library -> {
                        item(key = "shelves") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HomeTitle(s(R.string.shelves), null) {}
                                listOf(Tab.ReadingNow, Tab.ToRead, Tab.Finished, Tab.Favorites, Tab.All).chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        pair.forEach { t ->
                                            Card(Modifier.weight(1f).clickable { onShelf(t) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Icon(t.icon(), null, tint = MaterialTheme.colorScheme.primary)
                                                    Column {
                                                        Text(t.title(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1)
                                                        Text("${state.books.count { it.inTab(t) }}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        val top = lists.filter { depthOf(it) == 0 }
                        if (top.isNotEmpty()) item(key = "lists") {
                            Column {
                                HomeTitle(s(R.string.lists), null) {}
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(top) { path ->
                                        val books = state.books.count { it.inTab(Tab.List(path), deep = false) }
                                        val inner = state.childLists(path).size
                                        Card(Modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = { onOpenList(path) }, onLongClick = { onListMenu(path) }), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                                Column {
                                                    Text(nameOf(path), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                    Text(folderSummary(books, inner), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        val favs = state.books.filter { it.favorite }
                        if (favs.isNotEmpty()) item(key = "favorites") {
                            Column {
                                HomeTitle(s(R.string.favorites), s(R.string.all_favorites)) { onShelf(Tab.Favorites) }
                                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    items(favs.take(12), key = { it.id }) { b -> Box(Modifier.width(110.dp)) { GridBook(model, b, 0.7f, compact = true, { onOpen(b) }, { onDetails(b) }) } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTitle(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (action != null) androidx.compose.material3.TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun Panel2(title: String, content: @Composable ColumnScope.() -> Unit) {
    ZimamCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Diamond(); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
            content()
        }
    }
}

/** The top of Home: a greeting, how today is going, what is new, and shortcuts to the sections. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun HomeGreeting(model: FolioViewModel, state: dev.folio.reader.data.LibraryState, onShelf: (Tab) -> Unit, onStats: () -> Unit, onFiles: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when { hour < 5 -> s(R.string.still_up); hour < 12 -> s(R.string.good_morning); hour < 18 -> s(R.string.good_afternoon); else -> s(R.string.good_evening) }
    val today = state.day(0)
    val streak = state.streak()
    val reading = state.books.count { it.status == Status.Reading }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlowBox(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 18.dp)) {
            Text(greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(
                when {
                    today.seconds > 0 -> s(R.string.read_today, minutesLabel(today.seconds)) + if (streak > 1) s(R.string.a_day_streak, streak) else ""
                    reading > 0 -> sp(R.plurals.books_in_progress, reading)
                    else -> s(R.string.pick_a_book_to_start)
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
        val notes = remember(version) { dev.folio.reader.data.ReleaseNotes.forVersion(version) }
        if (notes.isNotEmpty() && state.hiddenNews != version) {
            Panel2(s(R.string.new_in_rukn, version)) {
                notes.forEach { Text("\u2022  $it", style = MaterialTheme.typography.bodySmall) }
                androidx.compose.material3.TextButton(onClick = { model.repo.update { it.copy(hiddenNews = version) } }) { Text(s(R.string.hide)) }
            }
        }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(s(R.string.reading_now) to { onShelf(Tab.ReadingNow) }, s(R.string.to_read) to { onShelf(Tab.ToRead) }, s(R.string.finished) to { onShelf(Tab.Finished) }, s(R.string.favourites) to { onShelf(Tab.Favorites) }, s(R.string.all_books) to { onShelf(Tab.All) }, s(R.string.stats) to onStats, s(R.string.files) to onFiles).forEach { (label, go) ->
                androidx.compose.material3.AssistChip(onClick = go, label = { Text(label) })
            }
        }
    }
}

/** Today, the week, the streak, the number and length of sittings, and a 7-day chart. */
@Composable
private fun HomeStats(state: dev.folio.reader.data.LibraryState, onClick: () -> Unit) {
    val today = state.day(0)
    val week = state.lastDays(7)
    val sittings = state.sessionsSince(7)
    val sittingsToday = state.sessionsSince(1).size
    val avg = if (sittings.isEmpty()) 0L else sittings.sumOf { it.seconds } / sittings.size
    val streak = state.streak()
    val days = (6 downTo 0).map { state.day(it) }
    val top = days.maxOf { it.seconds }.coerceAtLeast(60)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row {
                Stat(s(R.string.today), minutesLabel(today.seconds), sp(R.plurals.pages_n, today.pages), Modifier.weight(1f))
                Stat(s(R.string.this_week), minutesLabel(week.seconds), sp(R.plurals.pages_n, week.pages), Modifier.weight(1f))
                Stat(s(R.string.streak), sp(R.plurals.days_n, streak), if (streak > 0) s(R.string.keep_going) else s(R.string.read_a_minute), Modifier.weight(1f))
            }
            Row {
                Stat(s(R.string.sittings), "${sittings.size}", s(R.string.n_today, sittingsToday), Modifier.weight(1f))
                Stat(s(R.string.average_sitting), minutesLabel(avg), s(R.string.this_week_lower), Modifier.weight(1f))
                Stat(s(R.string.books), "${state.books.count { it.status == Status.Reading }}", s(R.string.being_read), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().height(54.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                days.forEachIndexed { i, d ->
                    Box(Modifier.weight(1f).height((46f * d.seconds / top).coerceAtLeast(3f).dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (i == 6) 0.9f else 0.4f)))
                }
            }
        }
    }
}
