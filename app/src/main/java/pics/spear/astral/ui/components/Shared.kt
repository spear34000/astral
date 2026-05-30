package pics.spear.astral.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import pics.spear.astral.ui.theme.*

// ── Cosmic Background ──────────────────────────────────
@Composable
fun CosmicBackground(modifier: Modifier = Modifier) {
    val particles = remember {
        List(80) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = Random.nextFloat() * 2f + 0.5f,
                alpha = Random.nextFloat() * 0.5f + 0.1f,
                speed = Random.nextFloat() * 0.0003f + 0.0001f,
                angle = Random.nextFloat() * 360f,
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "cosmic")
    val tick by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40000, easing = LinearEasing)),
        label = "tick",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Nebula gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AstralBlue.copy(alpha = 0.03f),
                    AstralPurple.copy(alpha = 0.02f),
                    Color.Transparent,
                ),
                center = Offset(
                    size.width * (0.3f + 0.2f * sin(Math.toRadians((tick * 0.5f).toDouble())).toFloat()),
                    size.height * (0.4f + 0.2f * cos(Math.toRadians((tick * 0.3f).toDouble())).toFloat()),
                ),
            ),
            radius = size.minDimension * 0.6f,
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AstralPurple.copy(alpha = 0.02f),
                    Color.Transparent,
                ),
                center = Offset(
                    size.width * (0.6f + 0.15f * cos(Math.toRadians((tick * 0.4f).toDouble())).toFloat()),
                    size.height * (0.3f + 0.15f * sin(Math.toRadians((tick * 0.6f).toDouble())).toFloat()),
                ),
            ),
            radius = size.minDimension * 0.5f,
        )

        // Stars
        particles.forEach { p ->
            val driftX = sin(Math.toRadians((tick * p.speed * 100 + p.angle).toDouble())).toFloat() * 15f
            val driftY = cos(Math.toRadians((tick * p.speed * 100 + p.angle * 0.7f).toDouble())).toFloat() * 15f
            val x = (p.x * size.width + driftX + size.width) % size.width
            val y = (p.y * size.height + driftY + size.height) % size.height
            val shimmer = 0.7f + 0.3f * sin(Math.toRadians((tick * 2f + p.angle).toDouble())).toFloat()
            drawCircle(Color.White.copy(alpha = p.alpha * shimmer), p.r, Offset(x, y))
        }
    }
}

private data class Particle(val x: Float, val y: Float, val r: Float, val alpha: Float, val speed: Float, val angle: Float)

// ── Screen Container ───────────────────────────────────
@Composable
fun AstralScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().background(SpaceBlack),
        contentAlignment = Alignment.TopStart,
    ) {
        CosmicBackground()
        content()
    }
}

// ── Premium Glass Card ─────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    glow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale",
    )

    val mod = modifier.scale(scale).clip(shape).then(
        if (onClick != null) Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        else Modifier
    )

    Box(modifier = mod) {
        if (glow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(Modifier.background(AstralBlue.copy(alpha = 0.05f), shape))
                    .padding(4.dp)
                    .clip(shape),
            )
        }
        Surface(shape = shape, color = Color.Transparent) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GlassWhite, GlassWhite.copy(alpha = 0.12f)),
                        ), shape,
                    )
                    .border(0.5.dp, GlassBorder, shape),
            ) { content() }
        }
    }
}

// ── Glowing Gradient Button ────────────────────────────
@Composable
fun GlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gradient: List<Color> = listOf(AstralBlue, AstralPurple),
) {
    val shape = RoundedCornerShape(14.dp)
    val infinite = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
        label = "ga",
    )

    Box(modifier = modifier.clip(shape)) {
        if (enabled) {
            Box(
                modifier = Modifier.matchParentSize()
                    .background(gradient[0].copy(alpha = glowAlpha * 0.25f))
                    .padding(8.dp).clip(shape),
            )
        }
        Surface(
            modifier = Modifier.clip(shape)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.4f)),
            shape = shape, color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier.background(Brush.horizontalGradient(gradient), shape)
                    .padding(horizontal = 28.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ── Astral Switch ──────────────────────────────────────
@Composable
fun AstralSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val anim by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "s",
    )
    Box(
        modifier = modifier.size(50.dp, 28.dp).clip(RoundedCornerShape(14.dp))
            .background(if (checked) Brush.horizontalGradient(listOf(AstralBlue, AstralPurple)) else SpaceOutline)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier.padding(start = if (checked) 24.dp else 3.dp).size(22.dp)
                .clip(CircleShape).background(Color.White).scale(if (checked) 1.1f else 1f),
        )
    }
}

// ── Stat Card ──────────────────────────────────────────
@Composable
fun StatCard(
    label: String, value: String, icon: @Composable () -> Unit,
    modifier: Modifier = Modifier, accent: Color = AstralBlue,
) {
    GlassCard(modifier = modifier, glow = true) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

// ── Section Header ─────────────────────────────────────
@Composable
fun SectionHeader(
    title: String, modifier: Modifier = Modifier,
    subtitle: String? = null, action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            action?.invoke()
        }
        Spacer(Modifier.height(2.dp))
        Box(Modifier.width(32.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
            .background(Brush.horizontalGradient(listOf(AstralBlue, AstralPurple))))
    }
}

// ── Bot Card ───────────────────────────────────────────
@Composable
fun BotCard(
    name: String, language: String, enabled: Boolean,
    onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, onClick = onClick, glow = enabled) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (enabled) Brush.linearGradient(45f, AstralBlue, AstralPurple) else SpaceSurface3),
                contentAlignment = Alignment.Center,
            ) {
                Text(language.take(2).uppercase(), color = if (enabled) Color.White else TextTertiary,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) TextPrimary else TextTertiary, fontWeight = FontWeight.SemiBold)
                Text(language.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            AstralSwitch(checked = enabled, onCheckedChange = onToggle)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Empty State ────────────────────────────────────────
@Composable
fun EmptyState(
    icon: @Composable () -> Unit, title: String, subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(GlassWhite), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp))
            }
            if (action != null) { Spacer(Modifier.height(20.dp)); action() }
        }
    }
}

// ── Shimmer ────────────────────────────────────────────
@Composable
fun Shimmer(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(12.dp)) {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val a by infinite.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "a")
    Box(modifier.clip(shape).background(GlassWhite.copy(alpha = a)))
}

// ── Vibe Coding — creative inspiration components ─────

/** Random creative prompts that cycle. */
private val vibeQuotes = listOf(
    "let the code flow through you",
    "make something that didn't exist before",
    "every great bot starts with a single line",
    "your imagination + AI = ✨",
    "vibe > perfection · ship it",
    "code is poetry in motion",
    "what do you want to create today?",
    "small steps, big magic",
    "the best time to create is now",
    "your bot, your rules, your vibe",
    "from spark to automation",
    "build cool shit · break things · learn",
    "think in flows, not just functions",
    "creation is a conversation",
    "let the vibes guide your logic",
)

@Composable
fun VibeQuote(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "vibe_q")
    val idx by infinite.animateFloat(
        initialValue = 0f, targetValue = (vibeQuotes.size - 1).toFloat(),
        animationSpec = infiniteRepeatable(tween(8000), repeatMode = RepeatMode.Restart),
        label = "qi",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), repeatMode = RepeatMode.Reverse),
        label = "qa",
    )

    Text(
        text = "\"" + vibeQuotes[idx.toInt() % vibeQuotes.size] + "\"",
        fontSize = 14.sp,
        color = TextPrimary.copy(alpha = alpha),
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = modifier,
    )
}

/** Pulsing text for creative emphasis. */
@Composable
fun PulseText(
    text: String,
    color: Color = AstralBlue,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val a by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
        label = "pa",
    )
    val s by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200), repeatMode = RepeatMode.Reverse),
        label = "ps",
    )
    Text(
        text = text,
        color = color.copy(alpha = a),
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier.scale(s),
    )
}

/** A creative prompt card shown on the dashboard. */
@Composable
fun VibePrompt(
    title: String = "today's vibe",
    prompt: String,
    gradient: List<Color> = listOf(AstralPurple, AstralBlue),
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    val infinite = rememberInfiniteTransition(label = "vp")
    val shimmer by infinite.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(3000), repeatMode = RepeatMode.Restart),
        label = "vs",
    )

    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    gradient[0].copy(alpha = 0.2f), gradient[1].copy(alpha = 0.15f),
                )
            )
            .border(0.5.dp, gradient[0].copy(alpha = 0.3f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", color = gradient[0], fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    title, fontSize = 10.sp, color = gradient[0].copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                prompt,
                fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth(0.3f).height(2.dp)
                    .background(Brush.horizontalGradient(gradient), RoundedCornerShape(1.dp))
            )
        }
    }
}

/** Floating creative tip overlay for the editor. */
@Composable
fun VibeTip(
    tip: String,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = AstralPurple.copy(alpha = 0.15f),
            tonalElevation = 0.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💡", fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    tip,
                    fontSize = 12.sp, color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

/** Glowing line accent for section headers. */
@Composable
fun GlowDivider(
    modifier: Modifier = Modifier,
    gradient: List<Color> = listOf(AstralBlue, AstralPurple, AstralEmerald),
) {
    val infinite = rememberInfiniteTransition(label = "gd")
    val a by infinite.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(1500), repeatMode = RepeatMode.Reverse),
        label = "ga",
    )
    Box(
        modifier
            .height(2.dp).fillMaxWidth(0.4f)
            .clip(RoundedCornerShape(1.dp))
            .background(Brush.horizontalGradient(gradient.map { it.copy(alpha = a) }))
    )
}

/** Vibe meter: small animated indicator of creative energy. */
@Composable
fun VibeMeter(level: Float = 0.8f, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "vm")
    val pulse by infinite.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse),
        label = "vmp",
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("vibe", fontSize = 9.sp, color = TextTertiary, letterSpacing = 1.sp)
        Spacer(Modifier.width(6.dp))
        (0..4).forEach { i ->
            Box(
                Modifier
                    .width(12.dp).height(4.dp)
                    .padding(end = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (i / 4f < level) AstralBlue.copy(alpha = pulse * (0.4f + 0.6f * (1f - i / 5f)))
                        else SpaceOutline
                    ),
            )
        }
    }
}
