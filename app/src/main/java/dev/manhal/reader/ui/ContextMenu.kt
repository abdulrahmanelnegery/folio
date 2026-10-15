package dev.manhal.reader.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.manhal.reader.R
import dev.manhal.reader.data.Book
import dev.manhal.reader.data.Status
import dev.manhal.reader.s

/** One entry of a long-press menu. A destructive entry is drawn in the error colour; the caller asks for confirmation inside [onClick]. */
class MenuAction(val icon: ImageVector, val label: String, val destructive: Boolean = false, val onClick: () -> Unit)

/** Tap does the main action, hold opens the menu with a light haptic. The same for every row, card and tile. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.tapOrHold(onClick: () -> Unit, onLongClick: () -> Unit): Modifier {
    val haptic = LocalHapticFeedback.current
    return combinedClickable(onClick = onClick, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongClick() })
}

/** The one long-press menu of the app: a title, then rows at least 56 dp high. The menu closes before the chosen action runs, unless [dismissOnAction] is false (a caller that shows dialogs from inside it). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextMenu(title: String, subtitle: String? = null, icon: ImageVector? = null, actions: List<MenuAction>, dismissOnAction: Boolean = true, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (icon != null) Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            actions.forEach { a ->
                val colour = if (a.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { if (dismissOnAction) onDismiss(); a.onClick() }.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(a.icon, null, tint = if (a.destructive) colour else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(a.label, Modifier.padding(start = 20.dp), style = MaterialTheme.typography.bodyLarge, color = colour)
                }
            }
        }
    }
}

/** What each book menu entry does; the screen supplies them. */
class BookMenuHandlers(
    val open: () -> Unit,
    val details: () -> Unit,
    val edit: () -> Unit,
    val toggleFavorite: () -> Unit,
    val addToList: () -> Unit,
    val toggleFinished: () -> Unit,
    val remove: () -> Unit,
)

/** Open, Details, Edit, Favourite, Add to a list, Finished, Remove (last, destructive). */
fun bookMenu(book: Book, h: BookMenuHandlers): List<MenuAction> = listOf(
    MenuAction(Icons.Rounded.AutoStories, if (book.physical) s(R.string.log_a_reading_session) else s(R.string.open), onClick = h.open),
    MenuAction(Icons.Rounded.Info, s(R.string.details), onClick = h.details),
    MenuAction(Icons.Rounded.Edit, s(R.string.edit_details_and_cover), onClick = h.edit),
    MenuAction(if (book.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder, if (book.favorite) s(R.string.remove_from_favorites) else s(R.string.add_to_favorites), onClick = h.toggleFavorite),
    MenuAction(Icons.Rounded.Folder, s(R.string.add_to_a_list), onClick = h.addToList),
    MenuAction(Icons.Rounded.CheckCircle, if (book.status == Status.Finished) s(R.string.mark_as_reading) else s(R.string.mark_as_finished), onClick = h.toggleFinished),
    MenuAction(Icons.Rounded.Delete, s(R.string.remove_from_library), destructive = true, onClick = h.remove),
)

/** Open, Rename, Move, New list inside, Delete (last, destructive). */
fun listMenu(open: () -> Unit, rename: () -> Unit, move: () -> Unit, newInside: () -> Unit, delete: () -> Unit): List<MenuAction> = listOf(
    MenuAction(Icons.Rounded.AutoStories, s(R.string.open), onClick = open),
    MenuAction(Icons.Rounded.Edit, s(R.string.rename), onClick = rename),
    MenuAction(Icons.AutoMirrored.Rounded.DriveFileMove, s(R.string.move_to_another_list), onClick = move),
    MenuAction(Icons.Rounded.CreateNewFolder, s(R.string.new_list_inside), onClick = newInside),
    MenuAction(Icons.Rounded.Delete, s(R.string.delete_this_list), destructive = true, onClick = delete),
)

/** Go to the page, or remove the bookmark (destructive). */
fun bookmarkMenu(goTo: () -> Unit, remove: () -> Unit): List<MenuAction> = listOf(
    MenuAction(Icons.Rounded.Bookmark, s(R.string.go_to_page), onClick = goTo),
    MenuAction(Icons.Rounded.Delete, s(R.string.remove_bookmark), destructive = true, onClick = remove),
)
