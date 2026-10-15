package dev.manhal.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.manhal.reader.R
import dev.manhal.reader.data.SearchHit
import dev.manhal.reader.data.SearchProgress
import dev.manhal.reader.data.TextSearch
import dev.manhal.reader.s
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Search inside the open book: a field, a progress bar while a long book is read, and the matches with a snippet and
 * where they are ([where] names a hit's page, chapter or line). Tapping a match calls [onPick] with it and the query.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchSheet(search: (String) -> Flow<SearchProgress>, where: (SearchHit) -> String, onPick: (SearchHit, String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf<SearchProgress?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(submitted) {
        progress = null
        if (submitted.isNotBlank()) search(submitted).collect { progress = it }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                label = { Text(s(R.string.search_in_this_book)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitted = query.trim() }),
            )
            val p = progress
            if (p != null && p.fraction < 1f) LinearProgressIndicator(progress = { p.fraction }, Modifier.fillMaxWidth())
            when {
                submitted.isBlank() -> Unit
                p == null -> Text(s(R.string.searching), color = MaterialTheme.colorScheme.onSurfaceVariant)
                p.hits.isEmpty() && p.fraction >= 1f -> Text(if (p.textFound) s(R.string.no_matches) else s(R.string.no_text_in_this_book), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(p.hits) { hit ->
                        Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onPick(hit, submitted) }.padding(vertical = 8.dp)) {
                            Text(where(hit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(highlight(hit.snippet, submitted), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        }
                    }
                }
            }
        }
    }
}

/** The snippet with the first match in bold. */
private fun highlight(snippet: String, query: String) = buildAnnotatedString {
    val at = TextSearch.find(snippet, query, limit = 1, context = 0).firstOrNull()
    if (at == null) append(snippet) else {
        append(snippet.substring(0, at.start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(snippet.substring(at.start, at.end)) }
        append(snippet.substring(at.end))
    }
}
