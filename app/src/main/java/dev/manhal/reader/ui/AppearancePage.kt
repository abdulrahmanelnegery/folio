package dev.manhal.reader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.manhal.reader.R
import dev.manhal.reader.data.AppSettings
import dev.manhal.reader.data.AppTheme
import dev.manhal.reader.data.ColourTheme
import dev.manhal.reader.data.Density
import dev.manhal.reader.data.TextSize
import dev.manhal.reader.data.label
import dev.manhal.reader.s
import androidx.compose.foundation.selection.selectable

/** Appearance, the same everywhere in the family: mode, colour theme, text size with a live preview, density, reduce animations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePage(settings: AppSettings, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(s(R.string.mode))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AppTheme.entries.forEachIndexed { i, t ->
                SegmentedButton(settings.theme == t, { onChange { it.copy(theme = t) } }, SegmentedButtonDefaults.itemShape(i, AppTheme.entries.size), Modifier.heightIn(min = 48.dp)) { Text(t.label) }
            }
        }

        SectionTitle(s(R.string.colour_theme))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ColourTheme.entries.forEach { c ->
                val name = s(c.labelRes)
                Box(
                    Modifier.size(48.dp).selectable(settings.colour == c, role = Role.RadioButton, onClick = { onChange { it.copy(colour = c) } }).semantics { contentDescription = name },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(if (settings.colour == c) 40.dp else 32.dp).clip(CircleShape).background(Color(c.accent))
                            .then(if (settings.colour == c) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (settings.colour == c) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }

        SectionTitle(s(R.string.text_size))
        val sizes = TextSize.entries
        Slider(
            value = sizes.indexOf(settings.textSize).toFloat(),
            onValueChange = { v -> val size = sizes[v.toInt().coerceIn(0, sizes.lastIndex)]; if (size != settings.textSize) onChange { it.copy(textSize = size) } },
            valueRange = 0f..sizes.lastIndex.toFloat(),
            steps = sizes.size - 2,
            modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = s(R.string.text_size) },
        )
        Text(s(settings.textSize.labelRes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ZimamCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s(R.string.text_preview_title), style = MaterialTheme.typography.titleMedium)
                Text(s(R.string.text_preview_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        SectionTitle(s(R.string.density))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Density.entries.forEachIndexed { i, d ->
                SegmentedButton(settings.density == d, { onChange { it.copy(density = d) } }, SegmentedButtonDefaults.itemShape(i, Density.entries.size), Modifier.heightIn(min = 48.dp)) { Text(s(d.labelRes)) }
            }
        }

        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { onChange { it.copy(reduceMotion = !it.reduceMotion) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s(R.string.reduce_animations), style = MaterialTheme.typography.bodyLarge)
                Text(s(R.string.reduce_animations_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(settings.reduceMotion, { v -> onChange { it.copy(reduceMotion = v) } })
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
