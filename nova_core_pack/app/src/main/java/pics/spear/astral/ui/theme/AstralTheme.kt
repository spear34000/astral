package pics.spear.astral.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import pics.spear.astral.R

private val AstralLightColors = lightColorScheme(
    primary = Color(0xFF2F6BFF),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3F8CFF),
    onSecondary = Color(0xFF0F1726),
    tertiary = Color(0xFF5B89FF),
    onTertiary = Color(0xFF111B2E),
    background = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4E7ED),
    onSurface = Color(0xFF1B1F2A),
    onSurfaceVariant = Color(0xFF5B6575),
    error = Color(0xFFE15C5C),
    outline = Color(0xFFD6DAE2)
)

private val AstralDarkColors = darkColorScheme(
    primary = Color(0xFF3B78FF),
    onPrimary = Color(0xFF0A0F16),
    secondary = Color(0xFF4A90FF),
    onSecondary = Color(0xFF0A111D),
    tertiary = Color(0xFF6B96FF),
    onTertiary = Color(0xFF0B1220),
    background = Color(0xFF0F1114),
    surface = Color(0xFF171A1F),
    surfaceVariant = Color(0xFF222831),
    onSurface = Color(0xFFE7EAF0),
    onSurfaceVariant = Color(0xFFA0A8B5),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF2A313B)
)

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val headlineFont = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = fontProvider, weight = FontWeight.Bold)
)

private val bodyFont = FontFamily(
    Font(googleFont = GoogleFont("Sora"), fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Sora"), fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Sora"), fontProvider = fontProvider, weight = FontWeight.SemiBold)
)

private val AstralTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    titleMedium = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = bodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

private val AstralShapes = androidx.compose.material3.Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun AstralTheme(content: @Composable () -> Unit) {
    val useDark = isSystemInDarkTheme()
    val colors = if (useDark) AstralDarkColors else AstralLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AstralTypography,
        shapes = AstralShapes,
        content = content
    )
}
