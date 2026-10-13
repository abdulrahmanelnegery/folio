package dev.folio.reader.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.folio.reader.FolioViewModel
import dev.folio.reader.data.FileTypes
import dev.folio.reader.data.PageTheme
import dev.folio.reader.data.TextLang
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/** Paper and ink of a reading theme, for text screens. */
internal fun themeColors(theme: PageTheme): Pair<Color, Color> = when (theme) {
    PageTheme.Normal -> Color(0xFFFBF8F1) to Color(0xFF211F1A)
    PageTheme.Night -> Color(0xFF0C0C0C) to Color(0xFFE4E4E4)
    PageTheme.SoftNight -> Color(0xFF1E1C18) to Color(0xFFD6CFBE)
    PageTheme.Sepia -> Color(0xFFF4E8D0) to Color(0xFF3B3226)
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TextScreen(model: FolioViewModel, id: String) {
    val library by model.repo.state.collectAsState()
    val book = library.books.firstOrNull { it.id == id } ?: return
    LaunchedEffect(id) { model.loadText(book) }
    val session = model.texts[id]
    ReadingClock(model, id)
    val lang = FileTypes.textLang(book.fileName.ifEmpty { book.name }) ?: TextLang.Plain
    val theme = library.theme
    val (paper, ink) = themeColors(theme)
    val darkPaper = paper.red + paper.green + paper.blue < 1.5f
    val syntax = if (darkPaper) SyntaxColors.Dark else SyntaxColors.Light
    var fontSize by rememberSaveable { mutableFloatStateOf(15f) }
    var wrap by rememberSaveable { mutableStateOf(lang == TextLang.Plain || lang == TextLang.Markdown) }
    var lineNumbers by rememberSaveable { mutableStateOf(true) }
    var menu by remember { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val saveAs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { model.saveTextAs(id, it) }
    }
    val saveAsAndClose = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) model.saveTextAs(id, uri) { ok -> if (ok) { model.discardText(id); model.closeBook() } }
    }

    fun close() {
        if (session?.dirty == true) confirmClose = true else { model.discardText(id); model.closeBook() }
    }
    BackHandler { close() }

    // Pick up where the reader stopped, and remember where it is now.
    var restored by remember(id) { mutableStateOf(false) }
    LaunchedEffect(session != null) {
        if (session != null && !restored) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }.collect { total ->
                if (total > 1 && !restored) {
                    listState.scrollToItem((book.offset * total).toInt().coerceIn(0, total - 1))
                    restored = true
                }
            }
        }
    }
    LaunchedEffect(restored) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.layoutInfo.totalItemsCount }
            .debounce(600)
            .collect { (index, total) ->
                if (total > 1) model.repo.updateBook(id) { it.copy(offset = (index.toFloat() / (total - 1)).coerceIn(0f, 1f)) }
            }
    }

    Scaffold(
        containerColor = paper,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        book.name + if (session?.dirty == true) "  •" else "",
                        maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = {
                    if (session != null) {
                        IconButton(onClick = { session.editing = !session.editing }) {
                            Icon(if (session.editing) Icons.Rounded.MenuBook else Icons.Rounded.Edit, if (session.editing) "Read" else "Edit")
                        }
                        IconButton(onClick = { model.saveText(id) }, enabled = session.dirty) { Icon(Icons.Rounded.Save, "Save") }
                    }
                    IconButton(onClick = { model.repo.update { it.copy(theme = if (it.theme == PageTheme.Normal) PageTheme.Night else PageTheme.Normal) } }) {
                        Icon(if (theme == PageTheme.Normal) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, "Theme")
                    }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Text bigger") }, onClick = { fontSize = (fontSize + 1f).coerceAtMost(32f) })
                            DropdownMenuItem(text = { Text("Text smaller") }, onClick = { fontSize = (fontSize - 1f).coerceAtLeast(9f) })
                            DropdownMenuItem(text = { Text("Wrap long lines") }, trailingIcon = { if (wrap) Icon(Icons.Rounded.Check, null) }, onClick = { wrap = !wrap; menu = false })
                            DropdownMenuItem(text = { Text("Line numbers") }, trailingIcon = { if (lineNumbers) Icon(Icons.Rounded.Check, null) }, onClick = { lineNumbers = !lineNumbers; menu = false })
                            DropdownMenuItem(text = { Text("Save as a new file…") }, onClick = { menu = false; saveAs.launch(book.fileName.ifEmpty { book.name }) })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = paper, titleContentColor = ink, navigationIconContentColor = ink, actionIconContentColor = ink),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(paper)) {
            when {
                session == null -> Text("Opening…", Modifier.align(Alignment.Center), color = ink.copy(alpha = 0.6f))
                session.editing -> EditBody(session.text, { session.text = it }, lang, syntax, ink, fontSize, wrap)
                lang == TextLang.Markdown -> MarkdownView(session.text, ink, paper, fontSize, listState, Modifier.fillMaxSize().navigationBarsPadding())
                else -> CodeBody(session.text, lang, syntax, ink, fontSize, wrap, lineNumbers, listState)
            }
        }
    }

    if (confirmClose) AlertDialog(
        onDismissRequest = { confirmClose = false },
        title = { Text("Save your changes?") },
        text = { Text("“${book.name}” has edits that are not saved. You can save over the file, save a new copy and keep the original, or discard the edits.") },
        confirmButton = {
            Row {
                TextButton(onClick = { confirmClose = false; model.saveText(id) { ok -> if (ok) { model.discardText(id); model.closeBook() } } }) { Text("Save") }
                TextButton(onClick = { confirmClose = false; saveAsAndClose.launch(book.fileName.ifEmpty { book.name }) }) { Text("Save as new") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmClose = false; model.discardText(id); model.closeBook() }) { Text("Discard") }
                TextButton(onClick = { confirmClose = false }) { Text("Cancel") }
            }
        },
    )
}

/** Read mode for code and plain text: highlighted lines, numbered, optionally wrapped. */
@Composable
private fun CodeBody(text: String, lang: TextLang, syntax: SyntaxColors, ink: Color, fontSize: Float, wrap: Boolean, lineNumbers: Boolean, state: androidx.compose.foundation.lazy.LazyListState) {
    val styled = remember(text, lang, syntax) { TextHighlight.highlight(text, lang, syntax) }
    val starts = remember(text) {
        val list = ArrayList<Int>()
        list += 0
        text.forEachIndexed { i, c -> if (c == '\n') list += i + 1 }
        list
    }
    val mono = lang != TextLang.Plain
    val gutter = (starts.size.toString().length * fontSize * 0.62f + 12f).dp
    val base = if (wrap) Modifier.fillMaxSize() else Modifier.fillMaxSize().horizontalScroll(rememberScrollState())
    LazyColumn(state = state, modifier = base, contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 48.dp)) {
        items(starts.size) { n ->
            val from = starts[n]
            val to = if (n + 1 < starts.size) starts[n + 1] - 1 else text.length
            Row {
                if (lineNumbers && lang != TextLang.Plain) {
                    Text("${n + 1}", Modifier.width(gutter).padding(end = 8.dp), color = ink.copy(alpha = 0.35f), fontFamily = FontFamily.Monospace, fontSize = (fontSize * 0.85f).sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Text(
                    styled.subSequence(from, to.coerceAtLeast(from)),
                    color = ink,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.5f).sp,
                    softWrap = wrap,
                )
            }
        }
    }
}

/** Edit mode: the same colours while typing. */
@Composable
private fun EditBody(text: String, onChange: (String) -> Unit, lang: TextLang, syntax: SyntaxColors, ink: Color, fontSize: Float, wrap: Boolean) {
    val transformation = remember(lang, syntax) {
        VisualTransformation { input -> TransformedText(TextHighlight.highlight(input.text, lang, syntax), OffsetMapping.Identity) }
    }
    val scroll = rememberScrollState()
    val horizontal = rememberScrollState()
    BasicTextField(
        value = text,
        onValueChange = onChange,
        textStyle = TextStyle(
            color = ink,
            fontFamily = if (lang == TextLang.Plain || lang == TextLang.Markdown) FontFamily.Default else FontFamily.Monospace,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5f).sp,
        ),
        cursorBrush = Brush.verticalGradient(listOf(ink, ink)),
        visualTransformation = transformation,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll)
            .let { if (wrap) it else it.horizontalScroll(horizontal) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 120.dp),
    )
}
