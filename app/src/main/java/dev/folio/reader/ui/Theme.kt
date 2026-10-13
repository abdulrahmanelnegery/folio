package dev.folio.reader.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Orange = Color(0xFFE5481A)
private val Paper = Color(0xFFF5F1E8)
private val Ink = Color(0xFF121214)

/** Zimam's colours: warm paper and near-black with an orange accent. */
private val Light: ColorScheme = lightColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDD0),
    onPrimaryContainer = Color(0xFF3A0E00),
    secondary = Color(0xFF6B5B4B),
    secondaryContainer = Color(0xFFEBE2D3),
    onSecondaryContainer = Ink,
    background = Paper,
    surface = Paper,
    surfaceContainer = Color(0xFFFFFBF3),
    surfaceContainerHigh = Color(0xFFEDE6D8),
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5E5A52),
    outlineVariant = Color(0xFFD8D0C0),
)

private val Dark: ColorScheme = darkColorScheme(
    primary = Color(0xFFFF5A1F),
    onPrimary = Color(0xFF1A0A02),
    primaryContainer = Color(0xFF4A1D0B),
    onPrimaryContainer = Color(0xFFFFDDD0),
    secondary = Color(0xFFD9CDB8),
    secondaryContainer = Color(0xFF2A2A2E),
    onSecondaryContainer = Color(0xFFF5F1E8),
    background = Ink,
    surface = Ink,
    surfaceContainer = Color(0xFF1B1B1E),
    surfaceContainerHigh = Color(0xFF26262A),
    onSurface = Color(0xFFF5F1E8),
    onSurfaceVariant = Color(0xFFA9A59B),
    outlineVariant = Color(0xFF3A3A40),
)

private val FolioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun FolioTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, shapes = FolioShapes, content = content)
}
