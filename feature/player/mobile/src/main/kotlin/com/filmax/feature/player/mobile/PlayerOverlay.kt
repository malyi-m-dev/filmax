// Оверлей плеера: название и действия сверху, перемотка и пауза по центру, громкость справа,
// ползунок и чипы вкладок внизу; чип «Далее» отсчитывает секунды до следующей серии.

package com.filmax.feature.player.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxMetrics
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxChipSize
import com.filmax.core.ui.components.FilmaxCountdownChip
import com.filmax.core.ui.components.FilmaxIconButton
import com.filmax.core.ui.components.FilmaxSeekBar
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.label
import com.filmax.feature.player.common.panelTabs
import com.filmax.feature.player.common.playerSubtitle
import com.filmax.feature.player.common.speedValue

/** За сколько секунд до конца чип «Далее» начинает отсчёт. */
private const val NEXT_COUNTDOWN_MS = 10_000L
private const val SECOND_MS = 1000L

/** Шаг перемотки кнопками ⟲10 / 10⟳. */
internal const val SEEK_STEP_MS = 10_000L

/** Что оверлей умеет делать — одним объектом: порог LongParameterList у detekt — 6. */
internal data class OverlayActions(
    val onBack: () -> Unit,
    val onTogglePlay: () -> Unit,
    val onSeekBy: (Long) -> Unit,
    val onScrub: (Long) -> Unit,
    val onScrubFinished: () -> Unit,
    val onOpenTab: (PlayerTab) -> Unit,
    val onPlayOnTv: () -> Unit,
    val onLock: () -> Unit,
    /** null — следующей серии нет (фильм или последняя серия). */
    val onNext: (() -> Unit)?,
)

@Composable
internal fun PlayerOverlay(state: PlayerState, playback: PlaybackUi, actions: OverlayActions) {
    Box(Modifier.fillMaxSize().background(OverlayScrim)) {
        TopRow(state = state, actions = actions, modifier = Modifier.align(Alignment.TopStart))
        CenterControls(playing = playback.playing, actions = actions, modifier = Modifier.align(Alignment.Center))
        PlayerVolume(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(OverlayTopInsets)
                .padding(end = 24.dp),
        )
        BottomControls(state = state, playback = playback, actions = actions)
    }
}

@Composable
private fun TopRow(state: PlayerState, actions: OverlayActions, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(OverlayTopInsets)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilmaxIconButton(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", onClick = actions.onBack)
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                state.item?.title.orEmpty(),
                style = FilmaxType.playerTitle,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            playerSubtitle(state, paused = false).takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = FilmaxType.meta,
                    color = FilmaxColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        // Продолжить на телевизоре с этого места: модель соберёт серию и позицию.
        FilmaxIconButton(Icons.Filled.Cast, contentDescription = "Смотреть на телевизоре", onClick = actions.onPlayOnTv)
        FilmaxIconButton(Icons.Filled.LockOpen, contentDescription = "Заблокировать экран", onClick = actions.onLock)
    }
}

@Composable
private fun CenterControls(playing: Boolean, actions: OverlayActions, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilmaxIconButton(
            Icons.Filled.Replay10,
            contentDescription = "Назад 10 секунд",
            onClick = { actions.onSeekBy(-SEEK_STEP_MS) },
            size = SeekSize,
        )
        FilmaxIconButton(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (playing) "Пауза" else "Смотреть",
            onClick = actions.onTogglePlay,
            size = PlaySize,
        )
        FilmaxIconButton(
            Icons.Filled.Forward10,
            contentDescription = "Вперёд 10 секунд",
            onClick = { actions.onSeekBy(SEEK_STEP_MS) },
            size = SeekSize,
        )
    }
}

@Composable
private fun BoxScope.BottomControls(state: PlayerState, playback: PlaybackUi, actions: OverlayActions) {
    Column(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .windowInsetsPadding(OverlayBottomInsets)
            .padding(start = 32.dp, end = 32.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilmaxSeekBar(
            positionMs = playback.shownPositionMs,
            durationMs = playback.durationMs,
            bufferedMs = playback.bufferedMs,
            onScrub = actions.onScrub,
            onScrubFinished = actions.onScrubFinished,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.panelTabs().forEach { tab ->
                    FilmaxChip(
                        label = chipLabel(tab, state),
                        selected = false,
                        onClick = { actions.onOpenTab(tab) },
                        leadingIcon = tab.icon,
                        size = FilmaxChipSize.Large,
                    )
                }
            }
            val next = state.nextTrack
            val onNext = actions.onNext
            if (next != null && onNext != null) {
                NextChip(next = next, remainingMs = playback.remainingMs, playing = playback.playing, onClick = onNext)
            }
        }
    }
}

/**
 * «Далее · S4E8»; за [NEXT_COUNTDOWN_MS] до конца — «через 7» и заливка отсчёта: серия доиграет и
 * следующая начнётся сама, а чип показывает, когда.
 */
@Composable
private fun NextChip(next: MediaTrack, remainingMs: Long, playing: Boolean, onClick: () -> Unit) {
    val counting = playing && remainingMs in 1..NEXT_COUNTDOWN_MS
    val code = if (next.seasonNumber > 0) "S${next.seasonNumber}E${next.number}" else "Серия ${next.number}"
    val seconds = (remainingMs + SECOND_MS - 1) / SECOND_MS
    FilmaxCountdownChip(
        label = if (counting) "Далее · $code через $seconds" else "Далее · $code",
        icon = Icons.Filled.SkipNext,
        onClick = onClick,
        progress = if (counting) 1f - remainingMs.toFloat() / NEXT_COUNTDOWN_MS else null,
    )
}

/**
 * Заблокированный экран: видно только «замок» — случайные касания ладонью не ставят паузу и не
 * перематывают. Тап по замку снимает блокировку.
 */
@Composable
internal fun LockedOverlay(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        FilmaxIconButton(
            Icons.Filled.Lock,
            contentDescription = "Разблокировать экран",
            onClick = onUnlock,
            size = FilmaxMetrics.IconButtonLarge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(OverlayTopInsets)
                .padding(24.dp),
        )
    }
}

/** Подпись чипа вкладки: качество и скорость — текущим значением, остальные — названием. */
private fun chipLabel(tab: PlayerTab, state: PlayerState): String = when (tab) {
    PlayerTab.Quality -> state.currentQuality ?: tab.label
    PlayerTab.Speed -> speedValue(state.currentSpeed)
    else -> tab.label
}

internal val PlayerTab.icon: ImageVector
    get() = when (this) {
        PlayerTab.Episodes -> Icons.Filled.VideoLibrary
        PlayerTab.Audio -> Icons.Filled.GraphicEq
        PlayerTab.Subtitles -> Icons.Filled.Subtitles
        PlayerTab.Quality -> Icons.Filled.Hd
        PlayerTab.Speed -> Icons.Filled.Speed
    }

/** Затемнение кадра под контролами: сверху и снизу плотнее — там текст и ползунок. */
private val OverlayScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.6f),
    0.28f to Color.Transparent,
    0.52f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.8f),
)

private val SeekSize = 60.dp
private val PlaySize = 84.dp
