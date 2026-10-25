package com.filmax.core.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Прячет статус-бар и панель навигации, пока экран в композиции, — полноэкранное видео. Свайп от
 * края показывает их на пару секунд поверх картинки, не сдвигая её: так устроены плееры Netflix и
 * YouTube, а системный жест «назад» остаётся под рукой.
 *
 * Уходит последний такой экран — панели возвращаются: остальное приложение живёт edge-to-edge с
 * видимыми барами. Экран, сменивший сам себя (следующая серия), их не показывает.
 * Отступы контролов поверх видео берите из `WindowInsets.displayCutout` вместе с барами: без
 * статус-бара в альбомной ориентации кнопки иначе заезжают под вырез камеры.
 */
@Composable
fun HideSystemBars() {
    HoldWindowMode(
        mode = WindowMode.HiddenSystemBars,
        enable = { activity ->
            val controller = activity.insetsController()
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        },
        disable = { activity -> activity.insetsController().show(WindowInsetsCompat.Type.systemBars()) },
    )
}

private fun Activity.insetsController(): WindowInsetsControllerCompat =
    WindowCompat.getInsetsController(window, window.decorView)
