package com.filmax.app.tv.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.filmax.core.tv.designsystem.FxMotion
import com.filmax.feature.player.common.navigation.PlayerRoute
import com.filmax.feature.player.common.navigation.TrailerRoute

/**
 * Переходы ТВ-графа — по хендоффу «Filmax TV» (Behavior, «Навигация и переходы»), только через
 * прозрачность: на телевизоре сдвиг целого экрана читается как прокрутка, а не как переход.
 *
 * - вперёд: новый экран проявляется за 350 мс, уходящий гаснет быстрее — за 200, и два экрана
 *   не смешиваются в кашу; раздел из меню открывается так же — его контент поднимается сам (fxEnter);
 * - назад: возвращённый экран проявляется за 200 мс, закрываемый гаснет за 220 по кривой ухода;
 * - плеер и трейлер — кроссфейд 200 мс через чёрный в обе стороны: видео не ждёт анимации.
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tvEnterTransition(): EnterTransition =
    if (involvesPlayer()) fadeIn(FxMotion.fade()) else fadeIn(FxMotion.screenIn())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tvExitTransition(): ExitTransition =
    fadeOut(FxMotion.fade())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tvPopEnterTransition(): EnterTransition =
    fadeIn(FxMotion.fade())

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.tvPopExitTransition(): ExitTransition =
    if (involvesPlayer()) fadeOut(FxMotion.fade()) else fadeOut(FxMotion.panelOut())

/** Переход в плеер, из него или между сериями. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.involvesPlayer(): Boolean =
    initialState.destination.isPlayer() || targetState.destination.isPlayer()

private fun NavDestination.isPlayer(): Boolean = hasRoute(PlayerRoute::class) || hasRoute(TrailerRoute::class)
