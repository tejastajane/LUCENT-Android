package com.tejas.liquidplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeutralDarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFCECECE),
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF080808),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFF454545),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun LiquidPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NeutralDarkScheme) {
        // The app uses custom Box/Backdrop surfaces rather than Material Surface.
        // Set a visible default for all unstyled text and icons on the AMOLED-black UI.
        CompositionLocalProvider(LocalContentColor provides NeutralDarkScheme.onSurface) {
            content()
        }
    }
}
