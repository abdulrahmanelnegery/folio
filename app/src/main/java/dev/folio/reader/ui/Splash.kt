package dev.folio.reader.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val SplashInk = Color(0xFF121214)
private val SplashCream = Color(0xFFF5F1E8)
private val SplashOrange = Color(0xFFFF5A1F)

/**
 * The opening screen: the corner draws itself, the semi-square settles into it, then "by Zimam" fades in.
 * [onDone] is called when it has played, about a second and a quarter after it starts.
 */
@Composable
fun Splash(name: String, onDone: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val byline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { draw.animateTo(1f, tween(520, easing = LinearEasing)) }
        launch { kotlinx.coroutines.delay(380); pop.animateTo(1f, tween(320)) }
        launch { kotlinx.coroutines.delay(620); byline.animateTo(1f, tween(380)) }
        kotlinx.coroutines.delay(1250)
        onDone()
    }
    Column(Modifier.fillMaxSize().background(SplashInk), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(132.dp)) {
            val k = size.width / 268f
            val o = Offset(-122f * k, -122f * k)
            fun p(x: Float, y: Float) = Offset(x * k + o.x, y * k + o.y)
            // 424 units of line: the downstroke (212) then the base (212).
            val len = 424f * draw.value
            val down = minOf(len, 212f)
            val across = (len - 212f).coerceAtLeast(0f)
            val stroke = Stroke(width = 56f * k, cap = StrokeCap.Round)
            drawLine(SplashCream, p(150f, 150f), p(150f, 150f + down), stroke.width, StrokeCap.Round)
            if (across > 0f) drawLine(SplashCream, p(150f, 362f), p(150f + across, 362f), stroke.width, StrokeCap.Round)
            val s = 100f * k * pop.value
            val c = p(312f, 200f)
            zimamDot(c, s * 1.04f, SplashOrange)
        }
        Text(name, Modifier.alpha(byline.value), color = SplashCream, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("by Zimam", Modifier.alpha(byline.value), color = SplashOrange, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}
