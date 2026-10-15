package dev.manhal.reader.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.manhal.reader.R
import dev.manhal.reader.data.AppSettings
import dev.manhal.reader.data.AppLanguage
import dev.manhal.reader.data.LibraryState
import dev.manhal.reader.data.homeTabs
import dev.manhal.reader.data.label
import dev.manhal.reader.s

/** The top level of Settings, in the order the Zimam apps share (ui-spec-shared.md); Manhal's own row is Reader defaults. */
enum class SettingsPage(@StringRes val titleRes: Int, val icon: ImageVector) {
    Appearance(R.string.appearance, Icons.Rounded.Brush),
    Layout(R.string.sections_and_layout, Icons.Rounded.Dashboard),
    Language(R.string.language, Icons.Rounded.Language),
    Reader(R.string.reader_defaults, Icons.Rounded.MenuBook),
    Data(R.string.data_and_storage, Icons.Rounded.Storage),
    Privacy(R.string.privacy_and_security, Icons.Rounded.Lock),
    About(R.string.about, Icons.Rounded.Info),
}

/** The one-line summary of the current value shown under each row's title. */
fun SettingsPage.summary(app: AppSettings, library: LibraryState, sharedStorage: Boolean, version: String): String = when (this) {
    SettingsPage.Appearance -> listOf(app.theme.label, s(app.colour.labelRes), s(app.textSize.labelRes)).joinToString(", ")
    SettingsPage.Layout -> library.homeTabs().filter { it.visible }.joinToString(", ") { it.tab.label }
    SettingsPage.Language -> when (app.language) {
        AppLanguage.System -> s(R.string.system)
        AppLanguage.English -> "English"
        AppLanguage.Arabic -> "العربية"
    } + if (app.arabicDigits) ", " + s(R.string.arabic_digits_short) else ""
    SettingsPage.Reader -> s(R.string.reader_defaults_summary, s(library.theme.labelRes), library.epubTextPercent)
    SettingsPage.Data -> if (sharedStorage) s(R.string.data_summary_shared) else s(R.string.data_summary_private)
    SettingsPage.Privacy -> s(R.string.privacy_summary)
    SettingsPage.About -> s(R.string.version, version)
}

/** One row of the settings list: icon in a tinted circle, title, one-line summary, chevron. At least 56 dp high. */
@Composable
fun SettingsRow(page: SettingsPage, summary: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(page.icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(s(page.titleRes), style = MaterialTheme.typography.bodyLarge)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The list of rows. */
@Composable
fun SettingsHub(app: AppSettings, library: LibraryState, sharedStorage: Boolean, version: String, onOpen: (SettingsPage) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingsPage.entries.forEach { page -> SettingsRow(page, page.summary(app, library, sharedStorage, version)) { onOpen(page) } }
    }
}
