package com.filmax.app

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.filmax.app.navigation.FilmaxNavGraph
import com.filmax.app.tv.navigation.FilmaxTvNavGraph
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.tv.designsystem.FilmaxTvTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // Один APK на оба форм-фактора: на Android TV (leanback) — TV-граф, иначе телефонный.
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        setContent {
            if (isTv) {
                FilmaxTvTheme { FilmaxTvNavGraph() }
            } else {
                FilmaxTheme { FilmaxNavGraph() }
            }
        }
    }
}
