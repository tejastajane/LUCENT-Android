package com.tejas.liquidplayer

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tejas.liquidplayer.ui.LiquidPlayerApp
import com.tejas.liquidplayer.ui.LiquidPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.BLACK
        // Keep the system navigation area transparent so the screen can continue behind
        // the overlaid liquid-glass tabs instead of creating a solid black strip.
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            LiquidPlayerTheme {
                LiquidPlayerApp()
            }
        }
    }
}
