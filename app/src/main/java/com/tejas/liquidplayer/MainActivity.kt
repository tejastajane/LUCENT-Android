package com.tejas.liquidplayer

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tejas.liquidplayer.ui.LiquidPlayerApp
import com.tejas.liquidplayer.ui.LiquidPlayerTheme

class MainActivity : ComponentActivity() {
    private var pipMode by mutableStateOf(false)

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        val preferences = getSharedPreferences("liquid_player", MODE_PRIVATE)
        setContent {
            val inPip = pipMode
            var isDarkTheme by rememberSaveable { mutableStateOf(preferences.getBoolean("dark_theme", true)) }
            SideEffect {
                window.statusBarColor = if (isDarkTheme) AndroidColor.BLACK else AndroidColor.WHITE
                window.navigationBarColor = AndroidColor.TRANSPARENT
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDarkTheme
                    isAppearanceLightNavigationBars = !isDarkTheme
                }
            }
            LiquidPlayerTheme(darkTheme = isDarkTheme) {
                LiquidPlayerApp(
                    isDarkTheme = isDarkTheme,
                    onThemeChanged = { dark ->
                        isDarkTheme = dark
                        preferences.edit().putBoolean("dark_theme", dark).apply()
                    },
                    isInPictureInPictureMode = inPip
                )
            }
        }
    }
}
