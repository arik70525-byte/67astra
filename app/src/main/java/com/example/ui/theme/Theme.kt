package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF004D5B),
    onPrimaryContainer = Color(0xFF9CF4FF),
    secondary = JarvisBlue,
    onSecondary = Color(0xFF001B3D),
    secondaryContainer = Color(0xFF0D2F60),
    onSecondaryContainer = Color(0xFFB8D5FF),
    tertiary = JarvisGold,
    onTertiary = Color(0xFF291B00),
    background = JarvisBackground,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurface,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceVariant,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisCardBorder,
    error = JarvisRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
