// Полноэкранный режим мобильного плеера: скрытые системные панели, поворот кнопкой «На весь
// экран» и выключатель звука. Раньше обе кнопки были заглушками `onClick = {}`, а статус-бар
// висел поверх видео даже в альбомной ориентации.

package com.filmax.feature.player.mobile

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.filmax.core.ui.components.HideSystemBars

private val ControlButtonSize = 44.dp
private val ControlIconSize = 20.dp

/**
 * Отступы контролов поверх видео. Системные панели скрыты, и safeDrawing сводится к вырезу
 * камеры: в альбомной он сбоку, и без отступа кнопки заезжали бы под него.
 */
internal val OverlayTopInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

internal val OverlayBottomInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)

/** Ниже этой высоты (альбомная на телефоне) оверлей ужимается — см. [overlayMetrics]. */
private const val COMPACT_HEIGHT_DP = 480

/** Размеры оверлея, зависящие от высоты экрана. */
internal class OverlayMetrics(
    val playSize: Dp,
    val seekSize: Dp,
    /** Насколько поднять центральный ряд над серединой экрана — в зазор между шапкой и полосой. */
    val centerLift: Dp,
    val bottomPadding: Dp,
    val controlsGap: Dp,
)

private val RegularOverlay = OverlayMetrics(
    playSize = 88.dp,
    seekSize = 64.dp,
    centerLift = 0.dp,
    bottomPadding = 24.dp,
    controlsGap = 16.dp,
)

private val CompactOverlay = OverlayMetrics(
    playSize = 64.dp,
    seekSize = 52.dp,
    centerLift = 44.dp,
    bottomPadding = 8.dp,
    controlsGap = 4.dp,
)

/**
 * В альбомной на телефоне высоты всего ~360dp, и оверлей, рассчитанный на портрет, наезжал
 * паузой и перемоткой на полосу прогресса. В компактной высоте кнопки меньше, нижний блок
 * плотнее, а центральный ряд поднят в свободную зону между шапкой и полосой.
 */
@Composable
internal fun overlayMetrics(): OverlayMetrics =
    if (LocalConfiguration.current.screenHeightDp < COMPACT_HEIGHT_DP) CompactOverlay else RegularOverlay

/**
 * Эффекты полноэкранного плеера на всё время экрана: системные панели скрыты, а поворот,
 * заданный кнопкой [FullscreenButton], снимается на выходе. Живёт на уровне экрана, а не в
 * кнопке: кнопка уходит из композиции вместе с автоскрытием оверлея, и поворот сбрасывался бы
 * через четыре секунды после нажатия.
 */
@Composable
internal fun PlayerFullscreenEffects() {
    HideSystemBars()
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

/**
 * «На весь экран» — поворот в альбомную и обратно. Видео 16:9 в портрете занимает треть экрана,
 * а с выключенным автоповоротом без этой кнопки в альбомной не посмотреть вовсе. Выход фиксирует
 * портрет: просто «как решит датчик» оставил бы лежащий боком телефон в альбомной, и кнопка
 * выглядела бы мёртвой.
 */
@Composable
internal fun FullscreenButton() {
    val activity = LocalActivity.current ?: return
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    GlassButton(
        size = ControlButtonSize,
        onClick = {
            activity.requestedOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        },
    ) {
        Icon(
            if (isLandscape) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
            contentDescription = if (isLandscape) "Свернуть" else "На весь экран",
            tint = Color.White,
            modifier = Modifier.size(ControlIconSize),
        )
    }
}

/**
 * Выключатель звука. Громкость регулируют кнопки телефона; здесь — быстро заглушить видео, не
 * трогая системную громкость (уведомления и звонок остаются слышны).
 */
@Composable
internal fun MuteButton(player: Player) {
    var muted by remember(player) { mutableStateOf(player.volume == 0f) }
    GlassButton(
        size = ControlButtonSize,
        onClick = {
            muted = !muted
            player.volume = if (muted) 0f else 1f
        },
    ) {
        Icon(
            if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (muted) "Включить звук" else "Выключить звук",
            tint = Color.White,
            modifier = Modifier.size(ControlIconSize),
        )
    }
}
