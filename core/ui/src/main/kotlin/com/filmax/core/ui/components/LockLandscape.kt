package com.filmax.core.ui.components

import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable

/**
 * Альбомная ориентация, пока экран в композиции, — полноэкранное видео. Сенсорная альбомная:
 * телефон можно перевернуть другой стороной. Уходит последний такой экран — ориентация отдаётся
 * системе; экран, сменивший сам себя (следующая серия), блокировку не снимает.
 */
@Composable
fun LockLandscape() {
    HoldWindowMode(
        mode = WindowMode.Landscape,
        enable = { activity -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE },
        disable = { activity -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED },
    )
}
