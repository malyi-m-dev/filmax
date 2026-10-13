package com.filmax.core.ui.components

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Прячет статус-бар и панель навигации, пока экран в композиции, — полноэкранное видео. Свайп от
 * края показывает их на пару секунд поверх картинки, не сдвигая её: так устроены плееры Netflix и
 * YouTube, а системный жест «назад» остаётся под рукой.
 *
 * Уходит экран — панели возвращаются: остальное приложение живёт edge-to-edge с видимыми барами.
 * Отступы контролов поверх видео берите из `WindowInsets.displayCutout` вместе с барами: без
 * статус-бара в альбомной ориентации кнопки иначе заезжают под вырез камеры.
 */
@Composable
fun HideSystemBars() {
    val activity = LocalActivity.current ?: return
    val view = LocalView.current
    DisposableEffect(activity, view) {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}
