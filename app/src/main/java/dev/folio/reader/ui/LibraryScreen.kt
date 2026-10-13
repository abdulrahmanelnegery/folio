package dev.folio.reader.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.Book
import dev.folio.reader.data.SortOrder
import dev.folio.reader.data.Status
import dev.folio.reader.data.Tab
import dev.folio.reader.data.visible

private fun Tab.title(): String = when (this) {
    Tab.ReadingNow -> "Reading now"
    Tab.ToRead -> "To read"
    Tab.Finished -> "Finished"
    Tab.Favorites -> "Favorites"
    Tab.All -> "All"
    is Tab.List -> name
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(model: FolioViewModel, onOpenPicker: () -> Unit) {
    val state by model.repo.state.collectAsState()
    var tab by remember { mutableStateOf<Tab>(Tab.ReadingNow) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var newList by remember { mutableStateOf(false) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val shown = state.visible(tab, query)
    val heroFirst = tab == Tab.ReadingNow && query.isBlank() && shown.isNotEmpty()
    val hero = if (heroFirst) shown.first() else null
    val rest = if (heroFirst) shown.drop(1) else shown
    val shelves = listOf(Tab.ReadingNow, Tab.ToRead, Tab.Finished, Tab.Favorites, Tab.All)

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Text("Folio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp))
                shelves.forEach { t ->
                    DrawerEntry(t.icon(), t.title(), state.books.count { it.inTabInternal(t) }, tab == t) { tab = t; scope.launch { drawer.close() } }
                }
                HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
                Text("Lists", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp))
                state.lists.forEach { name ->
                    val t = Tab.List(name)
                    DrawerEntry(Icons.Rounded.Folder, name, state.books.count { it.inTabInternal(t) }, tab == t) { tab = t; scope.launch { drawer.close() } }
                }
                DrawerEntry(Icons.Rounded.CreateNewFolder, "New list", null, false) { scope.launch { drawer.close() }; newList = true }
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
                        Text(tab.title(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Rounded.Menu, "Menu") }
                },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, "Search")
                    }
                    IconButton(onClick = { model.repo.update { it.copy(grid = !it.grid) } }) {
                        Icon(if (state.grid) Icons.Rounded.ViewList else Icons.Rounded.GridView, if (state.grid) "List view" else "Grid view")
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.AutoMirrored.Rounded.Sort, "Sort") }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label, fontWeight = if (order == state.sort) FontWeight.SemiBold else FontWeight.Normal) },
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
            Spacer(Modifier.height(8.dp))
            if (shown.isEmpty()) {
                EmptyShelf(tab, onOpenPicker)
            } else if (state.grid) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (hero != null) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            HeroCard(model, hero, onOpen = { model.openBook(hero.id) }, onMenu = { menuFor = hero.id })
                        }
                        if (rest.isNotEmpty()) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text("Also reading", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                    items(rest, key = { it.id }) { book ->
                        GridBook(model, book, onOpen = { model.openBook(book.id) }, onMenu = { menuFor = book.id })
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (hero != null) {
                        item { HeroCard(model, hero, onOpen = { model.openBook(hero.id) }, onMenu = { menuFor = hero.id }) }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    items(rest, key = { it.id }) { book ->
                        ListBook(model, book, onOpen = { model.openBook(book.id) }, onMenu = { menuFor = book.id })
                    }
                }
            }
        }
    }
    }

    menuFor?.let { id ->
        val book = state.books.firstOrNull { it.id == id }
        if (book == null) menuFor = null else BookSheet(model, book, state.lists, onDismiss = { menuFor = null })
    }
    if (newList) NewListDialog(onDismiss = { newList = false }) { name ->
        if (model.repo.addList(name)) tab = Tab.List(name.trim())
        newList = false
    }
    LaunchedEffect(tab) { if (tab is Tab.List && (tab as Tab.List).name !in state.lists) tab = Tab.All }
}

private fun Book.inTabInternal(t: Tab): Boolean = when (t) {
    Tab.ReadingNow -> status == Status.Reading
    Tab.ToRead -> status == Status.ToRead
    Tab.Finished -> status == Status.Finished
    Tab.Favorites -> favorite
    Tab.All -> true
    is Tab.List -> t.name in lists
}

@Composable
private fun cover(model: FolioViewModel, book: Book): ImageBitmap? =
    produceState<ImageBitmap?>(null, book.id) { value = model.covers.get(book)?.asImageBitmap() }.value

@Composable
private fun Cover(model: FolioViewModel, book: Book, modifier: Modifier = Modifier) {
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
                Text(book.name.take(1).uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 40.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (book.favorite) {
            Icon(Icons.Rounded.Star, null, tint = Color(0xFFF2B12E), modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp))
        }
        if (book.progress > 0f) {
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.35f),
            )
        }
    }
}

private fun placeholderColor(name: String): Color {
    val palette = listOf(0xFFB54A4A, 0xFF4A7FB5, 0xFF4A9E7A, 0xFF9A6BB5, 0xFFC08A3E, 0xFF5B6B8C)
    return Color(palette[(name.hashCode() and Int.MAX_VALUE) % palette.size])
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroCard(model: FolioViewModel, book: Book, onOpen: () -> Unit, onMenu: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onMenu),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Cover(model, book, Modifier.width(96.dp).aspectRatio(0.68f))
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
                }
                Column {
                    LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onOpen) { Icon(Icons.Rounded.AutoStories, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Continue") }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridBook(model: FolioViewModel, book: Book, onOpen: () -> Unit, onMenu: () -> Unit) {
    Column(Modifier.combinedClickable(onClick = onOpen, onLongClick = onMenu)) {
        Cover(model, book, Modifier.fillMaxWidth().aspectRatio(0.7f))
        Spacer(Modifier.height(8.dp))
        Text(book.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            if (book.status == Status.Finished) "Finished" else "${book.pages} pages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        ReadingProgress(book)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBook(model: FolioViewModel, book: Book, onOpen: () -> Unit, onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onOpen, onLongClick = onMenu).padding(vertical = 8.dp),
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
        IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, "More") }
    }
}

@Composable
private fun EmptyShelf(tab: Tab, onOpen: () -> Unit) {
    val (title, hint) = when (tab) {
        Tab.ReadingNow -> "Nothing here yet" to "Books you open show up here, so you can pick up where you stopped."
        Tab.ToRead -> "Your to-read shelf is empty" to "Press and hold a book, then choose To read."
        Tab.Finished -> "No finished books" to "Press and hold a book, then choose Mark as finished."
        Tab.Favorites -> "No favorites yet" to "Press and hold a book, then choose Add to favorites."
        Tab.All -> "No books yet" to "Open a PDF to start your library."
        is Tab.List -> "This list is empty" to "Press and hold a book, then choose Add to list."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookSheet(model: FolioViewModel, book: Book, lists: List<String>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(book.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            SheetRow(if (book.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (book.favorite) "Remove from favorites" else "Add to favorites") {
                model.repo.updateBook(book.id) { it.copy(favorite = !it.favorite) }; onDismiss()
            }
            SheetRow(Icons.Rounded.BookmarkAdd, if (book.status == Status.ToRead) "Remove from To read" else "To read") {
                model.repo.updateBook(book.id) { it.copy(status = if (it.status == Status.ToRead) Status.Reading else Status.ToRead) }; onDismiss()
            }
            SheetRow(Icons.Rounded.CheckCircle, if (book.status == Status.Finished) "Mark as reading" else "Mark as finished") {
                model.repo.updateBook(book.id) { it.copy(status = if (it.status == Status.Finished) Status.Reading else Status.Finished) }; onDismiss()
            }
            lists.forEach { list ->
                val on = list in book.lists
                SheetRow(Icons.Rounded.Folder, if (on) "In $list" else "Add to $list", highlighted = on) { model.repo.toggleList(book.id, list) }
            }
            SheetRow(Icons.Rounded.Delete, "Remove from library") { model.remove(book.id); onDismiss() }
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(icon, null, tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun NewListDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New list") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("List name") }) },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Tab.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    Tab.ReadingNow -> Icons.Rounded.AutoStories
    Tab.ToRead -> Icons.Rounded.BookmarkAdd
    Tab.Finished -> Icons.Rounded.Done
    Tab.Favorites -> Icons.Rounded.Star
    Tab.All -> Icons.Rounded.CollectionsBookmark
    is Tab.List -> Icons.Rounded.Folder
}

@Composable
private fun DrawerEntry(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, count: Int?, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, null) },
        label = { Text(label) },
        badge = { if (count != null) Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

/** A thin bar and the percentage read, on every book (empty until it is opened). */
@Composable
private fun ReadingProgress(book: Book, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)))
        Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
