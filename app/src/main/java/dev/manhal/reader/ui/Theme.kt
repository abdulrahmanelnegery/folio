package dev.manhal.reader.ui

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

private val Orange = Color(0xFFFF5A1F)
private val Paper = Color(0xFFFFF8F0)
private val Ink = Color(0xFF0F0F0F)

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
    surfaceContainer = Color(0xFFFFFDF9),
    surfaceContainerHigh = Color(0xFFF6E9DB),
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5E5A52),
    outlineVariant = Color(0xFFE8DBC9),
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
    surfaceContainer = Color(0xFF1A1613),
    surfaceContainerHigh = Color(0xFF241E1A),
    onSurface = Color(0xFFF5F1E8),
    onSurfaceVariant = Color(0xFFA9A59B),
    outlineVariant = Color(0xFF2E2823),
)

private val ManhalShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

/** True when the reader asked for less animation; screens that animate between states check it. */
val LocalReduceMotion = androidx.compose.runtime.staticCompositionLocalOf { false }

/** The chosen density (row padding and gaps); shared rows read it. */
val LocalDensitySetting = androidx.compose.runtime.staticCompositionLocalOf { dev.manhal.reader.data.Density.Comfortable }

private fun ColorScheme.withAccent(c: dev.manhal.reader.data.ColourTheme, dark: Boolean): ColorScheme =
    if (c == dev.manhal.reader.data.ColourTheme.Orange) this else copy(
        primary = Color(c.accent),
        primaryContainer = Color(if (dark) c.containerDark else c.containerLight),
        onPrimaryContainer = Color(if (dark) c.onContainerDark else c.onContainerLight),
    )

@Composable
fun ManhalTheme(settings: dev.manhal.reader.data.AppSettings = dev.manhal.reader.data.AppSettings(), content: @Composable () -> Unit) {
    val dark = settings.theme.isDark(isSystemInDarkTheme())
    val base = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(base.density, base.fontScale * settings.textSize.scale),
        LocalReduceMotion provides settings.reduceMotion,
        LocalDensitySetting provides settings.density,
    ) {
        MaterialTheme(colorScheme = (if (dark) Dark else Light).withAccent(settings.colour, dark), shapes = ManhalShapes, content = content)
    }
}
