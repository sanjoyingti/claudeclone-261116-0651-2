package com.aibuilt.claudeclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aibuilt.claudeclone.model.ThemeMode

private val SurfaceVariantLight = Color(0xFFEFECE4)

private val LightScheme = lightColorScheme(
    primary = Clay,
    onPrimary = Color.White,
    primaryContainer = ClayLight,
    onPrimaryContainer = Ink,
    secondary = ClayDark,
    onSecondary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = CreamSurface,
    onSurface = Ink,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = InkMuted,
    outline = OutlineWarm,
    error = ErrorRed,
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = ClayNight,
    onPrimary = Ink,
    primaryContainer = ClayDark,
    onPrimaryContainer = InkNight,
    secondary = ClayNight,
    onSecondary = Ink,
    background = NightBackground,
    onBackground = InkNight,
    surface = NightSurface,
    onSurface = InkNight,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = InkNightMuted,
    outline = OutlineNight,
    error = ErrorRedNight,
    onError = Ink
)

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun ClaudeCloneTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = ClaudeTypography,
            content = content
        )
    }
}
