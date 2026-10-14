package dev.folio.reader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The Zimam look shared by the screens: warm surfaces, one orange accent, a soft glow, large rounded cards with a hairline border. */
val ZimamOrange = Color(0xFFFF5A1F)
val CardCorner = 24.dp

/** A rounded card with a 1 dp border at about 8% of the text colour. */
@Composable
fun ZimamCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        content = content,
    )
}

/** A soft orange glow, to sit behind a hero area. */
fun Modifier.orangeGlow(strength: Float = 0.22f): Modifier = background(
    Brush.radialGradient(listOf(ZimamOrange.copy(alpha = strength), Color.Transparent), radius = 520f),
)

/** The small orange diamond (the Zimam semi-square), used as a signature on cards and headings. */
@Composable
fun Diamond(size: Dp = 10.dp) {
    Canvas(Modifier.size(size)) { zimamDot(Offset(this.size.width / 2f, this.size.height / 2f), this.size.width * 1.04f, ZimamOrange) }
}

@Composable
fun GlowBox(modifier: Modifier = Modifier, strength: Float = 0.22f, content: @Composable () -> Unit) {
    Box(modifier.orangeGlow(strength)) { content() }
}
