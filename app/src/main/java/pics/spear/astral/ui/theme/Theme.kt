package pics.spear.astral.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AstralColorScheme = darkColorScheme(
    primary = AstralBlue,
    onPrimary = SpaceBlack,
    primaryContainer = Color(0xFF1A3D9E),
    onPrimaryContainer = Color(0xFFB0C4FF),

    secondary = AstralPurple,
    onSecondary = SpaceBlack,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFC4B5FD),

    tertiary = AstralEmerald,
    onTertiary = SpaceBlack,
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFF86EFAC),

    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSurface2,
    onSurfaceVariant = TextSecondary,
    outline = SpaceOutline,
    outlineVariant = SpaceOutlineVariant,

    error = ErrorRed,
    onError = SpaceBlack,
)

@Composable
fun AstralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AstralColorScheme,
        typography = AstralTypography,
        content = content,
    )
}
