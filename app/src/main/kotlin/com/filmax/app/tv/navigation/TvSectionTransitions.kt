package com.filmax.app.tv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.filmax.app.navigation.NAV_FADE_MS
import com.filmax.app.navigation.navFadeIn
import com.filmax.app.navigation.navFadeOut
import kotlin.math.sign

/**
 * Переходы TV-графа. Смена раздела из бокового меню — не просто кросс-фейд: контент въезжает
 * по ходу движения по меню (ниже по списку — снизу, выше — сверху), и подсветка в меню с
 * контентом перетекают в одну сторону. Остальные переходы (детали, плеер) — общий фейд.
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tvEnterTransition(): EnterTransition {
    val direction = sectionDirection() ?: return navFadeIn
    return fadeIn(tween(NAV_FADE_MS, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(NAV_FADE_MS, easing = FastOutSlowInEasing)) { fullHeight ->
            direction * fullHeight / SECTION_SHIFT_DIVISOR
        }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tvExitTransition(): ExitTransition {
    val direction = sectionDirection() ?: return navFadeOut
    // Уходящий раздел гаснет быстрее, чем проявляется новый: два раздела не смешиваются в кашу.
    return fadeOut(tween(SECTION_EXIT_MS, easing = FastOutSlowInEasing)) +
        slideOutVertically(tween(NAV_FADE_MS, easing = FastOutSlowInEasing)) { fullHeight ->
            -direction * fullHeight / SECTION_SHIFT_DIVISOR
        }
}

/** +1 — раздел ниже по меню, −1 — выше, null — переход не между разделами. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.sectionDirection(): Int? {
    val from = sectionIndexOf(initialState.destination)
    val to = sectionIndexOf(targetState.destination)
    if (from < 0 || to < 0 || from == to) return null
    return (to - from).sign
}

/** Сдвиг — доля высоты экрана: намёк на направление, а не прокрутка страницы целиком. */
private const val SECTION_SHIFT_DIVISOR = 14

private const val SECTION_EXIT_MS = 200
