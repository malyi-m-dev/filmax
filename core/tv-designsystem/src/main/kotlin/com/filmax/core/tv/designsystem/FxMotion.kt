package com.filmax.core.tv.designsystem

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/** Кривые редизайна: одна на появление и фокус, одна на уход. */
object FxEasing {
    val Standard = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val Exit = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

/**
 * Все анимации ТВ собираются из этих спецификаций — длительности на месте не задаются.
 * Тайминги — из хендоффа «Filmax TV» (docs/tv-redesign.md, раздел «Движение»).
 */
object FxMotion {
    /** Рамка и scale кнопок, строк, чипов. */
    fun <T> focus(): FiniteAnimationSpec<T> = tween(FOCUS_MS, easing = FxEasing.Standard)

    /** Фокус на карточке — scale с лёгким перелётом. */
    fun <T> pop(): FiniteAnimationSpec<T> = spring(dampingRatio = POP_DAMPING, stiffness = POP_STIFFNESS)

    /** Прокрутка по шагу: текст панели описания по стрелке (к фокусу списки едут сами, см. PivotBringIntoViewSpec). */
    fun <T> scroll(): AnimationSpec<T> = tween(SCROLL_MS, easing = FxEasing.Standard)

    /** Сворачивание шапки подборки. */
    fun <T> collapse(): FiniteAnimationSpec<T> = tween(COLLAPSE_MS, easing = FxEasing.Standard)

    /** Появление меню, нижней панели плеера, панели описания. */
    fun <T> panelIn(): FiniteAnimationSpec<T> = tween(PANEL_IN_MS, easing = FxEasing.Standard)

    fun <T> panelOut(): FiniteAnimationSpec<T> = tween(PANEL_OUT_MS, easing = FxEasing.Exit)

    fun <T> fade(): FiniteAnimationSpec<T> = tween(FADE_MS)

    /** Проявление нового экрана при переходе вперёд — половина длительности появления контента. */
    fun <T> screenIn(): FiniteAnimationSpec<T> = tween(ENTER_MS / 2)

    /** Рост полосы прогресса при появлении карточки. */
    fun <T> grow(): FiniteAnimationSpec<T> = tween(GROW_MS, delayMillis = GROW_DELAY_MS, easing = FxEasing.Standard)

    const val FOCUS_MS = 160
    const val SCROLL_MS = 280
    const val COLLAPSE_MS = 320
    const val PANEL_IN_MS = 400
    const val PANEL_OUT_MS = 220
    const val FADE_MS = 200
    const val ENTER_MS = 700
    const val ENTER_STAGGER_MS = 60
    const val KEN_BURNS_MS = 16_000
    const val PULSE_MS = 1_400

    /** Блик на карточке в фокусе: один проход и задержка перед первым. */
    const val SWEEP_MS = 2_600
    const val SWEEP_DELAY_MS = 600

    const val GROW_MS = 1_100
    const val GROW_DELAY_MS = 200

    private const val POP_DAMPING = 0.55f
    private const val POP_STIFFNESS = 420f
}

/**
 * Сниженная анимация: в настройках разработчика масштаб анимаций выставлен в 0. Тогда без
 * ken-burns, пульса, блика и перелёта — переходы только через alpha.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Читает системный масштаб анимаций один раз на тему. */
@Composable
internal fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
