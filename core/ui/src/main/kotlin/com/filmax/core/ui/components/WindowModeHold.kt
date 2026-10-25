package com.filmax.core.ui.components

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.filmax.core.ui.R

/** Режим окна, который экран держит, пока он в композиции. */
internal enum class WindowMode { Landscape, HiddenSystemBars }

/**
 * Держит [mode], пока он нужен хотя бы одному экрану в композиции: [enable] — на первой заявке,
 * [disable] — когда снимается последняя.
 *
 * Счётчик, а не пара «включил в эффекте — выключил в onDispose»: экран, который заменяет сам себя
 * (следующая серия — новый плеер на месте старого), входит в композицию раньше, чем уходит
 * старый, — тот ещё доигрывает кроссфейд перехода. С парой onDispose старого снимал режим, который
 * новый только что включил: телефон с выключенным автоповоротом падал в портрет, а системные
 * панели вылезали поверх видео.
 */
@Composable
internal fun HoldWindowMode(mode: WindowMode, enable: (Activity) -> Unit, disable: (Activity) -> Unit) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity, mode) {
        val holds = activity.windowModeHolds()
        if (holds.take(mode)) enable(activity)
        onDispose { if (holds.release(mode)) disable(activity) }
    }
}

/** Сколько экранов держат каждый режим окна. */
internal class WindowModeHolds {
    private val counts = mutableMapOf<WindowMode, Int>()

    /** Заявить режим; true — заявка первая, режим пора включить. */
    fun take(mode: WindowMode): Boolean {
        val count = counts.getOrDefault(mode, 0) + 1
        counts[mode] = count
        return count == 1
    }

    /** Снять заявку; true — она была последней, режим пора выключить. */
    fun release(mode: WindowMode): Boolean {
        val count = counts.getOrDefault(mode, 0) - 1
        if (count > 0) counts[mode] = count else counts.remove(mode)
        return count == 0
    }
}

/**
 * Заявки живут на окне активити: экраны делят одно окно — его ориентацию и панели, — а remember
 * у каждого свой, и о соседе экран не знает. Тег на decorView уходит вместе с окном.
 */
private fun Activity.windowModeHolds(): WindowModeHolds {
    val decor = window.decorView
    return decor.getTag(R.id.window_mode_holds) as? WindowModeHolds
        ?: WindowModeHolds().also { decor.setTag(R.id.window_mode_holds, it) }
}
