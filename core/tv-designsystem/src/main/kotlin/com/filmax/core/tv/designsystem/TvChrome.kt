// Элементы оболочки экрана: часы в углу и индикатор страниц карусели.
package com.filmax.core.tv.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ClockFormat = DateTimeFormatter.ofPattern("HH:mm")
private const val MILLIS_IN_MINUTE = 60_000L

/** Часы чуть ярче вторичного текста: время ищут глазами, оно не должно теряться на кадре hero. */
private const val CLOCK_ALPHA = 0.85f
private val ClockIconSize = 20.dp
private val ClockGap = 14.dp

/**
 * Часы в углу экрана, как у системного лаунчера: на телевизоре, где смотрят вечером, время —
 * то, что ищут глазами. Слева — значок поиска: напоминание, что Каталог с поиском в меню.
 * Обновляются ровно на смене минуты, а не по таймеру раз в секунду.
 */
@Composable
fun TvClock(modifier: Modifier = Modifier) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            delay(MILLIS_IN_MINUTE - System.currentTimeMillis() % MILLIS_IN_MINUTE)
            value = LocalTime.now()
        }
    }
    val color = LocalFxColors.current.text.copy(alpha = CLOCK_ALPHA)
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(ClockGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = color, modifier = Modifier.size(ClockIconSize))
        Text(time.format(ClockFormat), style = FxType.clock, color = color)
    }
}

private val DotSize = 6.dp
private val ActiveDotWidth = 22.dp
private const val IDLE_DOT_ALPHA = 0.35f

/** Индикатор страниц карусели: текущая — вытянутая полоска акцентом, остальные — точки. */
@Composable
fun TvPageIndicator(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) ActiveDotWidth else DotSize, FxMotion.focus(), label = "dotWidth")
            val color by animateColorAsState(
                if (active) LocalFxColors.current.accent else Color.White.copy(alpha = IDLE_DOT_ALPHA),
                FxMotion.focus(),
                label = "dotColor",
            )
            Box(Modifier.width(width).height(DotSize).background(color, CircleShape))
        }
    }
}

/**
 * Подсказка «назад» в левом верхнем углу экранов без меню (детали, подборка): клавиша-значок и
 * куда она ведёт. [labelAlpha] — прозрачность подписи, читается при отрисовке: в свёрнутой шапке
 * подборки остаётся только значок, а подпись гаснет вместе со сворачиванием.
 */
@Composable
fun TvBackHint(label: String, modifier: Modifier = Modifier, labelAlpha: () -> Float = { 1f }) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        TvKeyCap(Icons.AutoMirrored.Rounded.ArrowBack)
        Text(
            label,
            style = FxType.metaOnImage,
            color = LocalFxColors.current.textSecondary,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha() },
        )
    }
}
