// Движение сеток «Моё» и папки: схлопывание убираемой карточки и прокрутка к опорной точке.
@file:OptIn(ExperimentalFoundationApi::class)

package com.filmax.feature.library.tv

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.filmax.core.tv.designsystem.FxEasing
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.core.tv.designsystem.LocalReducedMotion
import com.filmax.core.tv.designsystem.PivotBringIntoViewSpec

/** Убираемая карточка сжимается до 0.9 и гаснет — как в макете, за токен «fade» (200 мс). */
private const val COLLAPSED_SCALE = 0.9f

/**
 * Схлопывание карточки, которую убирают долгим «ОК»: scale 1→0.9 и alpha 1→0, и только потом
 * [onCollapsed] — событие модели. Сетка уберёт уже невидимую карточку, соседи съедут на её место
 * (`animateItem`). Событие уходит и тогда, когда карточка покинула композицию раньше конца
 * анимации (прокрутка за фокусом соседа увела её за край): убрать её зритель уже попросил.
 * Прогресс читается в слое — анимация не перекомпонует карточку.
 */
@Composable
internal fun Modifier.collapseOnRemove(removing: Boolean, onCollapsed: () -> Unit): Modifier {
    val progress = remember { Animatable(0f) }
    val reduced = LocalReducedMotion.current
    val currentOnCollapsed by rememberUpdatedState(onCollapsed)
    LaunchedEffect(removing) {
        if (!removing) return@LaunchedEffect
        try {
            if (!reduced) progress.animateTo(1f, tween(FxMotion.FADE_MS, easing = FxEasing.Exit))
        } finally {
            currentOnCollapsed()
        }
    }
    return graphicsLayer {
        val scale = 1f - (1f - COLLAPSED_SCALE) * progress.value
        scaleX = scale
        scaleY = scale
        alpha = 1f - progress.value
    }
}

/**
 * Прокрутка списка или сетки к опорной точке: элемент в фокусе встаёт краем на [pivot] от начала
 * контейнера — двигается лента, а не фокус ([PivotBringIntoViewSpec]).
 */
@Composable
internal fun PivotScroll(pivot: Dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val spec = remember(density, pivot) { PivotBringIntoViewSpec(with(density) { pivot.toPx() }) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
