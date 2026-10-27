package com.tejas.liquidplayer

import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tejas.liquidplayer.ui.LiquidPlayerApp
import com.tejas.liquidplayer.ui.LiquidPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        val preferences = getSharedPreferences("liquid_player", MODE_PRIVATE)
        setContent {
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
                LiquidPlayerApp(isDarkTheme = isDarkTheme, onThemeChanged = { dark ->
                    isDarkTheme = dark
                    preferences.edit().putBoolean("dark_theme", dark).apply()
                })
            }
        }
    }
}
