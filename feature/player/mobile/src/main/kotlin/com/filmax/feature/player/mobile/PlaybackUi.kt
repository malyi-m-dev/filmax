// Что оверлей знает о воспроизведении: идёт ли видео, буферизуется ли, позиция, длительность и
// загруженное. Источник — слушатель ExoPlayer и тик раз в секунду; тот же тик сохраняет прогресс.

package com.filmax.feature.player.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import kotlinx.coroutines.delay

private const val PROGRESS_TICK_MS = 1000L

@Stable
internal class PlaybackUi(playing: Boolean, buffering: Boolean) {
    var playing by mutableStateOf(playing)
    var buffering by mutableStateOf(buffering)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var bufferedMs by mutableLongStateOf(0L)

    /** Палец на ползунке: позиция пальца. Пока тянут, тик плеера её не перезаписывает. */
    var scrubMs by mutableStateOf<Long?>(null)

    /** Что показывать на ползунке и часах: палец, а если его нет — плеер. */
    val shownPositionMs: Long get() = scrubMs ?: positionMs

    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0)
}

/**
 * Состояние воспроизведения для оверлея. [onEnded] — серия доиграла: сразу следующая (как на ТВ),
 * иначе плеер застывал на титрах. [onTick] — позиция раз в секунду: модель сохраняет прогресс.
 */
@Composable
internal fun rememberPlayback(player: Player, onEnded: () -> Unit, onTick: (Long) -> Unit): PlaybackUi {
    val ui = remember(player) {
        PlaybackUi(playing = player.isPlaying, buffering = player.playbackState == Player.STATE_BUFFERING)
    }
    val currentOnEnded by rememberUpdatedState(onEnded)
    val currentOnTick by rememberUpdatedState(onTick)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                ui.playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // Буферизация — под спиннер: старт, перемотка и смена качества иначе выглядели бы
                // как зависший чёрный экран.
                ui.buffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) currentOnEnded()
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(PROGRESS_TICK_MS)
            val duration = player.duration.takeIf { it > 0 } ?: continue
            ui.durationMs = duration
            ui.positionMs = player.currentPosition
            ui.bufferedMs = player.bufferedPosition
            currentOnTick(player.currentPosition)
        }
    }
    return ui
}
