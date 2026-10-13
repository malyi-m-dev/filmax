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
import com.filmax.app.perf.JankMonitor
import com.filmax.app.tv.FilmaxTvPaletteTheme
import com.filmax.app.tv.navigation.FilmaxTvNavGraph
import com.filmax.app.update.AppUpdateEvent
import com.filmax.app.update.AppUpdatePrompt
import com.filmax.app.update.AppUpdateScreenModel
import com.filmax.core.designsystem.FilmaxTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    /** Рывки кадров по экранам — сводкой в logcat (FilmaxJank). */
    private var jankMonitor: JankMonitor? = null

    override fun onResume() {
        super.onResume()
        jankMonitor?.setTracking(true)
    }

    override fun onPause() {
        jankMonitor?.setTracking(false)
        super.onPause()
    }

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

        jankMonitor = JankMonitor(window)

        // Один APK на оба форм-фактора: на Android TV (leanback) — TV-граф, иначе телефонный.
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        setContent {
            // Модель обновлений одна на приложение и держится здесь явно: её диалог живёт
            // поверх графа, а ручную проверку дёргает строка «Проверить обновления» в Профиле —
            // внутри графа, и на телефоне, и на ТВ.
            val updateScreenModel: AppUpdateScreenModel = koinViewModel()
            val onCheckUpdates = { updateScreenModel.dispatch(AppUpdateEvent.Check) }
            if (isTv) {
                FilmaxTvPaletteTheme {
                    FilmaxTvNavGraph(onCheckUpdates = onCheckUpdates)
                    AppUpdatePrompt(updateScreenModel)
                }
            } else {
                FilmaxTheme {
                    FilmaxNavGraph(onCheckUpdates = onCheckUpdates)
                    AppUpdatePrompt(updateScreenModel)
                }
            }
        }
    }
}
