package com.racelink.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Race Link palette - motorsport-inspired dark theme. */
object RaceColors {
    val Bg = Color(0xFF07070B)
    val BgGradientTop = Color(0xFF12121A)
    val BgGradientBottom = Color(0xFF050507)
    val Surface = Color(0xFF12121A)
    val SurfaceRaised = Color(0xFF1A1A24)
    val SurfaceHigh = Color(0xFF24242F)
    val Outline = Color(0xFF2A2A36)
    val OutlineSoft = Color(0x33FFFFFF)
    val OnSurface = Color(0xFFEDEDF2)
    val OnSurfaceMuted = Color(0xFF9090A0)
    val OnSurfaceFaint = Color(0x66FFFFFF)

    val Accent = Color(0xFFFF2D3A)         // primary brand red
    val AccentSoft = Color(0xFFFF5C66)
    val AccentDeep = Color(0xFFB31420)
    val Amber = Color(0xFFFFB800)
    val Green = Color(0xFF22D17E)
    val Red = Color(0xFFFF4242)
    val Blue = Color(0xFF3B82F6)
}

private val DarkColors = darkColorScheme(
    primary = RaceColors.Accent,
    onPrimary = Color.White,
    primaryContainer = RaceColors.AccentDeep,
    onPrimaryContainer = Color.White,
    secondary = RaceColors.Amber,
    onSecondary = Color.Black,
    tertiary = RaceColors.Green,
    background = RaceColors.Bg,
    onBackground = RaceColors.OnSurface,
    surface = RaceColors.Surface,
    onSurface = RaceColors.OnSurface,
    surfaceVariant = RaceColors.SurfaceRaised,
    onSurfaceVariant = RaceColors.OnSurfaceMuted,
    outline = RaceColors.Outline,
    outlineVariant = RaceColors.OutlineSoft,
    error = RaceColors.Red,
)

private val Display = FontFamily.Default
private val Mono = FontFamily.Monospace

private val RaceTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 88.sp, letterSpacing = (-2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Mono, fontWeight = FontWeight.Black, fontSize = 56.sp, letterSpacing = (-1).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Black, fontSize = 36.sp, letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.2.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal, fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.5.sp,
    ),
)

@Composable
fun RaceLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = RaceTypography,
        content = content,
    )
}
