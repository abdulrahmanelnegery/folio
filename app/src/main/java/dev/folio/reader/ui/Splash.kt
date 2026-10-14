package dev.folio.reader.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val SplashInk = Color(0xFF231006)
private val SplashCream = Color(0xFFF5F1E8)
private val SplashOrange = Color(0xFFFF5A1F)

/**
 * The opening screen: the corner draws itself, the semi-square settles into it, then "by Zimam" fades in.
 * The name sits 28 dp below the mark and slides up as it fades in. [onDone] is called when it has played, about a second and a half after it starts.
 */
@Composable
fun Splash(name: String, onDone: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val byline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { draw.animateTo(1f, tween(560, easing = FastOutSlowInEasing)) }
        launch { kotlinx.coroutines.delay(420); pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)) }
        launch { kotlinx.coroutines.delay(640); title.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { kotlinx.coroutines.delay(860); byline.animateTo(1f, tween(380)) }
        kotlinx.coroutines.delay(1450)
        onDone()
    }
    Column(Modifier.fillMaxSize().background(SplashInk), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(dev.folio.reader.R.drawable.zimam_mark), null,
            Modifier.size(132.dp).alpha(draw.value).graphicsLayer { val k = 0.82f + 0.18f * pop.value; scaleX = k; scaleY = k },
        )
        Spacer(Modifier.height(28.dp))
        Text(name, Modifier.alpha(title.value).graphicsLayer { translationY = (1f - title.value) * 24f }, color = SplashCream, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("by Zimam", Modifier.alpha(byline.value), color = SplashOrange, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}
