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

private val Teal = Color(0xFF0F5E5B)
private val Paper = Color(0xFFF7F2E8)

private val Light: ColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE9E5),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF8A6A2F),
    background = Paper,
    surface = Paper,
    surfaceContainer = Color(0xFFFFFBF3),
    surfaceContainerHigh = Color(0xFFF0EADD),
    onSurface = Color(0xFF1C1B17),
    onSurfaceVariant = Color(0xFF5B574C),
    outlineVariant = Color(0xFFD9D2C2),
)

private val Dark: ColorScheme = darkColorScheme(
    primary = Color(0xFF6FD0C6),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF17504C),
    onPrimaryContainer = Color(0xFFCDE9E5),
    secondary = Color(0xFFE0C58A),
    background = Color(0xFF14130F),
    surface = Color(0xFF14130F),
    surfaceContainer = Color(0xFF1E1D18),
    surfaceContainerHigh = Color(0xFF292720),
    onSurface = Color(0xFFE8E2D4),
    onSurfaceVariant = Color(0xFFB4AE9F),
    outlineVariant = Color(0xFF3C392F),
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
