package com.filmax.app.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * Кросс-фейд переходов между экранами — общий для телефонного и TV-графов.
 *
 * Дефолтные fadeIn()/fadeOut() едут на пружине и глазом почти не читаются — переключение
 * выглядело резким. 350 мс — достаточно, чтобы переход ощущался плавным, и мало, чтобы
 * навигация не казалась вязкой.
 */
private const val NAV_FADE_MS = 350

val navFadeIn: EnterTransition = fadeIn(tween(NAV_FADE_MS))
val navFadeOut: ExitTransition = fadeOut(tween(NAV_FADE_MS))
