package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Immutable
data class SocialAgentColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val cardHover: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentSecondary: Color,
    val success: Color,
    val successSubtle: Color,
    val warning: Color,
    val warningSubtle: Color,
    val error: Color,
    val errorSubtle: Color,
    val heroStart: Color,
    val heroEnd: Color
)

val LightAppPalette = SocialAgentColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    card = LightCardSurface,
    cardElevated = LightCardSurfaceElevated,
    cardHover = LightCardSurfaceHover,
    border = LightCardBorder,
    borderSubtle = LightCardBorderSubtle,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    accent = AccentIndigo,
    accentSoft = Color(0xFFEEF2FF),
    accentSecondary = AccentCyan,
    success = EmeraldGreen,
    successSubtle = EmeraldSubtle,
    warning = AccentAmber,
    warningSubtle = AccentAmberSubtle,
    error = AlertRed,
    errorSubtle = AlertRedSubtle,
    heroStart = Color(0xFFEEF2FF),
    heroEnd = Color(0xFFE0F2FE)
)

val DarkAppPalette = SocialAgentColors(
    isDark = true,
    background = DarkBackground,
    surface = DarkSurface,
    card = CardSurface,
    cardElevated = CardSurfaceElevated,
    cardHover = CardSurfaceHover,
    border = CardBorder,
    borderSubtle = CardBorderSubtle,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    accent = AccentIndigoDarkMode,
    accentSoft = Color(0x296366F1),
    accentSecondary = NeonCyanDarkMode,
    success = EmeraldGreenDarkMode,
    successSubtle = Color(0x2610B981),
    warning = AccentAmberDarkMode,
    warningSubtle = Color(0x26F59E0B),
    error = AlertRedDarkMode,
    errorSubtle = Color(0x26F43F5E),
    heroStart = Color(0xFF172038),
    heroEnd = Color(0xFF13253B)
)

val LocalAppColors = staticCompositionLocalOf { LightAppPalette }

object AppTheme {
    val colors: SocialAgentColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

private val LightColorScheme = lightColorScheme(
    primary = AccentIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = AccentIndigo,
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = AccentCyan,
    tertiary = EmeraldGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    outlineVariant = LightCardBorderSubtle,
    error = AlertRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentIndigoDarkMode,
    onPrimary = Color.White,
    primaryContainer = CardSurfaceElevated,
    onPrimaryContainer = DarkTextPrimary,
    secondary = NeonCyanDarkMode,
    onSecondary = DarkBackground,
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = NeonCyanDarkMode,
    tertiary = EmeraldGreenDarkMode,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorderSubtle,
    error = AlertRedDarkMode,
    onError = Color.White
)

@Composable
fun SocialAgentTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appPalette = if (darkTheme) DarkAppPalette else LightAppPalette

    CompositionLocalProvider(LocalAppColors provides appPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
