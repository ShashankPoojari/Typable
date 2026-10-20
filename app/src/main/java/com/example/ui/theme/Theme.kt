package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.storage.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = TypableSky,
    onPrimary = Color.White,
    primaryContainer = TypableSkyLight,
    onPrimaryContainer = TypableNavy,
    secondary = TypableSlate,
    onSecondary = Color.White,
    background = TypableSurfaceLight,
    onBackground = TypableNavy,
    surface = TypableWhite,
    onSurface = TypableNavy,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TypableMutedSlate,
    outline = TypableLightBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = TypableSky,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF94A3B8),
    onSecondary = TypableNavy,
    background = TypableNavy,
    onBackground = Color(0xFFF8FAFC),
    surface = TypableSlate,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

private val SepiaColorScheme = lightColorScheme(
    primary = SepiaAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E5D3),
    onPrimaryContainer = SepiaText,
    secondary = SepiaMuted,
    onSecondary = Color.White,
    background = SepiaBackground,
    onBackground = SepiaText,
    surface = SepiaSurface,
    onSurface = SepiaText,
    surfaceVariant = Color(0xFFEFE8DB),
    onSurfaceVariant = SepiaMuted,
    outline = SepiaBorder
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightAccent,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color.White,
    secondary = MidnightMuted,
    onSecondary = Color.Black,
    background = MidnightBackground,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = MidnightBorder
)

@Composable
fun TypableTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.SEPIA -> SepiaColorScheme
        AppThemeMode.MIDNIGHT -> MidnightColorScheme
        AppThemeMode.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemInDark) DarkColorScheme else LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
