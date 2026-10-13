package dev.folio.reader.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewList
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
import dev.folio.reader.data.inTab
import dev.folio.reader.data.nameOf
import dev.folio.reader.data.orderedLists
import dev.folio.reader.data.parentOf
import dev.folio.reader.data.visible
import dev.folio.reader.pdf.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private fun Tab.title(): String = when (this) {
    Tab.ReadingNow -> "Reading now"
    Tab.ToRead -> "To read"
    Tab.Finished -> "Finished"
    Tab.Favorites -> "Favorites"
    Tab.All -> "All books"
    is Tab.List -> nameOf(path)
}

private fun Tab.icon(): ImageVector = when (this) {
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
    ViewMode.List -> Icons.Rounded.ViewList
    ViewMode.Details -> Icons.Rounded.ViewStream
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(model: FolioViewModel, onOpenPicker: () -> Unit, onSettings: () -> Unit) {
    val state by model.repo.state.collectAsState()
    var tab by remember { mutableStateOf<Tab>(Tab.ReadingNow) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var viewMenu by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var detailsFor by remember { mutableStateOf<String?>(null) }
    var newList by remember { mutableStateOf<NewList?>(null) }
    var manageLists by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val shown = state.visible(tab, query)
    val heroFirst = tab == Tab.ReadingNow && query.isBlank() && shown.isNotEmpty()
    val hero = if (heroFirst) shown.first() else null
    val rest = if (heroFirst) shown.drop(1) else shown
    val shelves = listOf(Tab.ReadingNow, Tab.ToRead, Tab.Finished, Tab.Favorites, Tab.All)
    val lists = state.orderedLists()
    val open: (Book) -> Unit = { model.openBook(it.id) }
    val details: (Book) -> Unit = { detailsFor = it.id }

    // A list that was deleted or renamed while it was open falls back to All.
    LaunchedEffect(lists, tab) { (tab as? Tab.List)?.let { if (it.path !in lists) tab = Tab.All } }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Folio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp))
                    shelves.forEach { t ->
                        DrawerEntry(t.icon(), t.title(), state.books.count { it.inTab(t) }, tab == t, 0) { tab = t; scope.launch { drawer.close() } }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Lists", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (lists.isNotEmpty()) TextButton(onClick = { scope.launch { drawer.close() }; manageLists = true }) { Text("Manage") }
                    }
                    lists.forEach { path ->
                        val t = Tab.List(path)
                        DrawerEntry(Icons.Rounded.Folder, nameOf(path), state.books.count { it.inTab(t) }, tab == t, depthOf(path)) { tab = t; scope.launch { drawer.close() } }
                    }
                    DrawerEntry(Icons.Rounded.CreateNewFolder, "New list", null, false, 0) { scope.launch { drawer.close() }; newList = NewList("", null) }
                    HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                    DrawerEntry(Icons.Rounded.Settings, "Settings and backup", null, false, 0) { scope.launch { drawer.close() }; onSettings() }
                    Spacer(Modifier.height(24.dp))
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        if (searching) {
                            val focus = LocalFocusManager.current
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                placeholder = { Text("Search your books") },
                                shape = RoundedCornerShape(28.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Column {
                                Text(tab.title(), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${shown.size} ${if (shown.size == 1) "book" else "books"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "Menu") } },
                    actions = {
                        IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                            Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, "Search")
                        }
                        Box {
                            IconButton(onClick = { viewMenu = true }) { Icon(state.view.icon(), "View") }
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
                            IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onOpenPicker,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Open PDF") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (shown.isEmpty()) {
                    EmptyShelf(tab, onOpenPicker)
                } else if (state.view == ViewMode.List || state.view == ViewMode.Details) {
                    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        if (hero != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) { HeroCard(model, hero, onOpen = { open(hero) }, onDetails = { details(hero) }) }
                            if (rest.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("Also reading", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        items(rest, key = { it.id }) { book ->
                            GridBook(model, book, ratio, compact = state.view == ViewMode.Compact, { open(book) }, { details(book) })
                        }
                    }
                }
            }
        }
    }

    detailsFor?.let { id ->
        val book = state.books.firstOrNull { it.id == id }
        if (book == null) detailsFor = null else DetailsSheet(model, book, lists, onNewList = { newList = NewList("", book.id) }, onDismiss = { detailsFor = null })
    }
    newList?.let { request ->
        ListDialog(lists, request.parent, onDismiss = { newList = null }) { name, parent ->
            if (model.repo.addList(name, parent)) {
                val path = if (parent.isEmpty()) name.trim() else "$parent/${name.trim()}"
                request.forBook?.let { model.repo.toggleList(it, path) }
                if (request.forBook == null) tab = Tab.List(path)
            }
            newList = null
        }
    }
    if (manageLists) ManageListsSheet(model, lists, onNewIn = { newList = NewList(it, null) }, onDismiss = { manageLists = false })
}

@Composable
private fun cover(model: FolioViewModel, book: Book): ImageBitmap? =
    produceState<ImageBitmap?>(null, book.id) { value = model.covers.get(book)?.asImageBitmap() }.value

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
                Text(book.name.take(1).uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (marks && book.favorite) {
            Icon(Icons.Rounded.Star, null, tint = Color(0xFFF2B12E), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
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
                if (onRate != null) "$i stars" else null,
                Modifier.size(size.dp).let { if (onRate != null) it.clickable { onRate(i) } else it },
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
                    Text("CONTINUE READING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(book.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (book.page > 0) "Page ${book.page + 1} of ${book.pages}" else "${book.pages} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (book.rating > 0) Stars(book.rating)
                }
                Column {
                    ReadingProgress(book)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOpen) { Icon(Icons.Rounded.AutoStories, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Continue") }
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
        Text(book.name, style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!compact) {
            if (book.rating > 0) Stars(book.rating, 12)
            else Text(if (book.status == Status.Finished) "Finished" else "${book.pages} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                if (book.page > 0 && book.status != Status.Finished) "Page ${book.page + 1} of ${book.pages}" else "${book.pages} pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            ReadingProgress(book)
        }
        IconButton(onClick = onDetails) { Icon(Icons.Rounded.MoreVert, "Details") }
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
                    "${book.pages} pages  ·  ${formatSize(book.size)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (book.lastOpened > 0) "Opened ${formatDate(book.lastOpened)}" else "Not opened yet",
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
        Tab.ReadingNow -> "Nothing here yet" to "Books you open show up here, so you can pick up where you stopped."
        Tab.ToRead -> "Your to-read shelf is empty" to "Press and hold a book, then choose To read."
        Tab.Finished -> "No finished books" to "Books you reach the end of land here."
        Tab.Favorites -> "No favorites yet" to "Press and hold a book, then choose Add to favorites."
        Tab.All -> "No books yet" to "Open a PDF to start your library."
        is Tab.List -> "This list is empty" to "Press and hold a book, then choose Lists."
    }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.AutoStories, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onOpen) { Text("Open a PDF") }
    }
}

@Composable
private fun DrawerEntry(icon: ImageVector, label: String, count: Int?, selected: Boolean, depth: Int, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        badge = { if (count != null) Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = (depth * 16).dp),
    )
}

// ---- details of one book ------------------------------------------------------------------

private enum class SheetSection(val label: String) { Actions("Actions"), Details("Details"), Notes("Notes") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(model: FolioViewModel, book: Book, lists: List<String>, onNewList: () -> Unit, onDismiss: () -> Unit) {
    var section by remember { mutableStateOf(SheetSection.Actions) }
    var confirmRemove by remember { mutableStateOf(false) }
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
                    SheetSection.Actions -> ActionsSection(model, book, lists, onNewList) { confirmRemove = true }
                    SheetSection.Details -> DetailsSection(book)
                    SheetSection.Notes -> NotesSection(model, book)
                }
            }
        }
    }
    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text("Remove from library?") },
        text = { Text("The PDF file stays where it is. Your reading place, notes and bookmarks for it are removed.") },
        confirmButton = { TextButton(onClick = { model.remove(book.id); confirmRemove = false; onDismiss() }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ActionsSection(model: FolioViewModel, book: Book, lists: List<String>, onNewList: () -> Unit, onRemove: () -> Unit) {
    var listPicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SheetRow(if (book.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (book.favorite) "Remove from favorites" else "Add to favorites") {
            model.repo.updateBook(book.id) { it.copy(favorite = !it.favorite) }
        }
        SheetRow(Icons.Rounded.BookmarkAdd, if (book.status == Status.ToRead) "Remove from To read" else "To read") {
            model.repo.updateBook(book.id) { it.copy(status = if (it.status == Status.ToRead) Status.Reading else Status.ToRead) }
        }
        SheetRow(Icons.Rounded.CheckCircle, if (book.status == Status.Finished) "Mark as reading" else "Mark as finished") {
            model.repo.updateBook(book.id) { it.copy(status = if (it.status == Status.Finished) Status.Reading else Status.Finished) }
        }
        SheetRow(Icons.Rounded.Folder, if (listPicker) "Lists (tap to close)" else "Add to a list…") { listPicker = !listPicker }
        if (listPicker) {
            lists.forEach { list ->
                val on = list in book.lists
                SheetRow(if (on) Icons.Rounded.Check else Icons.Rounded.Folder, nameOf(list), highlighted = on, indent = depthOf(list) + 1) { model.repo.toggleList(book.id, list) }
            }
            SheetRow(Icons.Rounded.CreateNewFolder, "New list…", indent = 1, onClick = onNewList)
        }
        SheetRow(Icons.Rounded.Delete, "Remove from library", onClick = onRemove)
    }
}

/** The facts, one line each; tap a row to read all of a long value. */
@Composable
private fun DetailsSection(book: Book) {
    val context = LocalContext.current
    val facts by produceState<List<Pair<String, String>>?>(null, book.id) {
        value = withContext(Dispatchers.IO) {
            val uri = Uri.parse(book.uri)
            val out = ArrayList<Pair<String, String>>()
            PdfFile.open(context, uri)?.use { pdf -> pdf.sizeOf(0)?.let { (w, h) -> out += "Page size" to pageSize(w, h) } }
            out += PdfInfo.read(context, uri)
            out
        }
    }
    val rows = buildList {
        add("File name" to book.fileName.ifEmpty { book.name })
        add("Pages" to book.pages.toString())
        add("Size" to formatSize(book.size))
        addAll(facts.orEmpty())
        if (book.added > 0) add("Added" to formatDate(book.added))
        add("Last opened" to if (book.lastOpened > 0) formatDate(book.lastOpened) else "Never")
        add("Reading place" to if (book.page > 0) "Page ${book.page + 1} of ${book.pages}" else "Start")
        add("Status" to when (book.status) { Status.New -> "New"; Status.Reading -> "Reading"; Status.ToRead -> "To read"; Status.Finished -> "Finished" })
        if (book.lists.isNotEmpty()) add("Lists" to book.lists.joinToString(", ") { it.replace("/", " › ") })
        add("Location" to book.uri)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
        rows.forEach { (k, v) -> FactRow(k, v) }
        if (facts == null) Text("Reading the file…", Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NotesSection(model: FolioViewModel, book: Book) {
    var editing by remember { mutableStateOf<dev.folio.reader.data.PageNote?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Your rating", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Stars(book.rating, 34) { model.repo.setRating(book.id, it) }
        Text("Note about this book", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        var note by remember(book.id) { mutableStateOf(book.note) }
        OutlinedTextField(note, { note = it; model.repo.setNote(book.id, it) }, placeholder = { Text("Write a note") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
        Text("Page comments (${book.comments.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
        if (book.comments.isEmpty()) Text("Add them while reading with the comment icon.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        book.comments.forEach { c ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { editing = c }.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("Page ${c.page + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(c.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { model.repo.deleteComment(book.id, c.id) }) { Icon(Icons.Rounded.Delete, "Delete comment") }
            }
        }
    }
    editing?.let { c ->
        var text by remember(c.id) { mutableStateOf(c.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Comment on page ${c.page + 1}") },
            text = { OutlinedTextField(text, { text = it }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { model.repo.saveComment(book.id, c.page, text, c.id); editing = null }, enabled = text.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
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

private data class NewList(val parent: String, val forBook: String?)

// ---- managing lists -----------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageListsSheet(model: FolioViewModel, lists: List<String>, onNewIn: (String) -> Unit, onDismiss: () -> Unit) {
    var renaming by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Text("Manage lists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text("Use the arrows to change the order inside a list. Lists can hold other lists.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
            lists.forEach { path ->
                var menu by remember { mutableStateOf(false) }
                val siblings = lists.filter { parentOf(it) == parentOf(path) }
                Row(Modifier.fillMaxWidth().padding(start = (16 + depthOf(path) * 20).dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                    Text(nameOf(path), Modifier.weight(1f).padding(vertical = 14.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    IconButton(onClick = { model.repo.moveList(path, true) }, enabled = siblings.firstOrNull() != path) { Icon(Icons.Rounded.ArrowUpward, "Move up") }
                    IconButton(onClick = { model.repo.moveList(path, false) }, enabled = siblings.lastOrNull() != path) { Icon(Icons.Rounded.ArrowDownward, "Move down") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; renaming = path })
                            DropdownMenuItem(text = { Text("New list inside") }, leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, null) }, onClick = { menu = false; onNewIn(path) })
                            DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Rounded.Delete, null) }, onClick = { menu = false; deleting = path })
                        }
                    }
                }
            }
        }
    }
    renaming?.let { path ->
        NameDialog("Rename list", nameOf(path), "Rename", onDismiss = { renaming = null }) { name ->
            model.repo.renameList(path, name)
            renaming = null
        }
    }
    deleting?.let { path ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${nameOf(path)}”?") },
            text = { Text("The list and any lists inside it are removed. The books stay in your library.") },
            confirmButton = { TextButton(onClick = { model.repo.deleteList(path); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
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
        title = { Text("New list") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.replace("/", "") }, singleLine = true, placeholder = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Box {
                    TextButton(onClick = { menu = true }) {
                        Icon(Icons.Rounded.Folder, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (parent.isEmpty()) "Top level" else "Inside " + parent.replace("/", " › "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(text = { Text("Top level") }, onClick = { parent = ""; menu = false })
                        lists.forEach { path ->
                            DropdownMenuItem(text = { Text(nameOf(path), modifier = Modifier.padding(start = (depthOf(path) * 14).dp)) }, onClick = { parent = path; menu = false })
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(name, parent) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it.replace("/", "") }, singleLine = true, placeholder = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onDone(name) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ---- formatting ---------------------------------------------------------------------------

internal fun formatSize(bytes: Long): String = when {
    bytes <= 0 -> "Unknown size"
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
