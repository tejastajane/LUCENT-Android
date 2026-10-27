package com.tejas.liquidplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val NeutralDarkScheme = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    secondary = Color(0xFFCECECE), onSecondary = Color.Black,
    background = Color.Black, onBackground = Color.White,
    surface = Color(0xFF080808), onSurface = Color.White,
    surfaceVariant = Color(0xFF181818), onSurfaceVariant = Color(0xFFB8B8B8),
    outline = Color(0xFF777777), outlineVariant = Color(0xFF454545),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005)
)

private val NeutralLightScheme = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White,
    secondary = Color(0xFF444444), onSecondary = Color.White,
    background = Color.White, onBackground = Color.Black,
    surface = Color(0xFFF7F7F7), onSurface = Color(0xFF101010),
    surfaceVariant = Color(0xFFECECEC), onSurfaceVariant = Color(0xFF515151),
    outline = Color(0xFF777777), outlineVariant = Color(0xFFBDBDBD),
    error = Color(0xFFBA1A1A), onError = Color.White
)

@Composable
fun LiquidPlayerTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) NeutralDarkScheme else NeutralLightScheme
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface) { content() }
    }
}
