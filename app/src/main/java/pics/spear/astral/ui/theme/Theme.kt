package pics.spear.astral.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AstralColorScheme = darkColorScheme(
    primary = Blue60,
    onPrimary = SpaceBlack,
    primaryContainer = Blue20,
    onPrimaryContainer = Blue80,

    secondary = Purple60,
    onSecondary = SpaceBlack,
    secondaryContainer = Purple40,
    onSecondaryContainer = Purple80,

    tertiary = Emerald60,
    onTertiary = SpaceBlack,
    tertiaryContainer = Emerald40,
    onTertiaryContainer = Emerald80,

    background = SpaceBlack,
    onBackground = TextPrimary,
    surface = SpaceSurface,
    onSurface = TextPrimary,
    surfaceVariant = SpaceSurfaceVariant,
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
