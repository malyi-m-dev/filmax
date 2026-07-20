package com.filmax.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Не даёт экрану гаснуть, пока [enabled] — таймаут сна системы плееру не указ: без этого
 * через 10 минут воспроизведения экран гас, а звук продолжал идти.
 *
 * Плееры передают `isPlaying` (на паузе экран гаснет по обычному таймауту — это правильно),
 * короткоживущие экраны вроде трейлера могут держать флаг безусловно. Флаг живёт на окне
 * (`View.keepScreenOn`) и снимается при уходе экрана из композиции.
 */
@Composable
fun KeepScreenOn(enabled: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
