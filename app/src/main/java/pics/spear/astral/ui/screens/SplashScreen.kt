package pics.spear.astral.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pics.spear.astral.ui.theme.*

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(true) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        )
        delay(400)
        visible = false
        onFinished()
    }

    if (visible) {
        val alphaVal by animateFloatAsState(
            targetValue = progress.value,
            animationSpec = tween(300),
            label = "alpha",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack),
            contentAlignment = Alignment.Center,
        ) {
            // Animated particles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = (progress.value * 80).toInt()
                val centerX = size.width / 2
                val centerY = size.height / 2
                repeat(count) {
                    val angle = it * 137.5f
                    val radius = (progress.value * size.minDimension * 0.4f) * (0.3f + (it % 7) * 0.1f)
                    val x = centerX + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * radius
                    val y = centerY + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * radius
                    val r = (1..3).random().toFloat() * progress.value
                    drawCircle(
                        color = Color.White.copy(
                            alpha = (0.1f + (it % 5) * 0.05f) * progress.value
                        ),
                        radius = r,
                        center = Offset(x, y),
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Logo circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                45f,
                                Blue60.copy(alpha = 0.2f * progress.value),
                                Purple60.copy(alpha = 0.15f * progress.value),
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "A",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = progress.value),
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Astral",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary.copy(alpha = progress.value),
                    letterSpacing = (-0.5).sp,
                )

                Text(
                    text = "KakaoTalk Bot Platform",
                    fontSize = 14.sp,
                    color = TextSecondary.copy(alpha = progress.value * 0.7f),
                )

                Spacer(Modifier.height(40.dp))

                // Loading indicator
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(3.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Blue60.copy(alpha = 0.2f),
                                    Blue60.copy(alpha = progress.value),
                                    Blue60.copy(alpha = 0.2f),
                                )
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}
