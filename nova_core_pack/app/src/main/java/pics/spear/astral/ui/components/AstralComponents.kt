package pics.spear.astral.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import pics.spear.astral.R

@Composable
fun AstralBackground() {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = scheme.background)
    }
}

@Composable
fun AstralScreen(content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        AstralBackground()
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.raw.astral_starfield)
                .decoderFactory(SvgDecoder.Factory())
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.18f)
        )
        content()
    }
}

@Composable
fun AstralHeader(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                eyebrow,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displayMedium
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AstralPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun VanguardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderAlpha: Float = 0.1f,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    val shape = MaterialTheme.shapes.large
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
    val sheen = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .then(clickableModifier)
            .shadow(3.dp, shape)
            .clip(shape)
            .border(1.dp, outline, shape),
        color = surface
    ) {
        Box(
            modifier = Modifier
                .background(sheen)
        ) {
            content()
        }
    }
}

@Composable
fun ModernBentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    VanguardCard(modifier, onClick, content = content)
}

@Composable
fun PremiumGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    VanguardCard(modifier, borderAlpha = 0.3f, content = content)
}
