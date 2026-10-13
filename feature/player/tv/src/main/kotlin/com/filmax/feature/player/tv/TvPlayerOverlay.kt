// Контролы транспорта поверх кадра: название, полоса перемотки с временем, пауза с перемоткой,
// плашки текущих дорожек и подсказки пульта. Уходят сами через 5 с бездействия.
package com.filmax.feature.player.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.Onest
import com.filmax.core.tv.designsystem.TvBackHint
import com.filmax.core.tv.designsystem.TvKeyHint
import com.filmax.core.tv.designsystem.TvKeyHints
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvPlayPauseButton
import com.filmax.core.tv.designsystem.TvSeekBar
import com.filmax.core.tv.designsystem.TvStatusChip
import com.filmax.core.tv.designsystem.TvTransportGlyph
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PlayerTab
import com.filmax.feature.player.common.formatPlayerTime
import com.filmax.feature.player.common.panelTabs

/** Появление и уход контролов — 250 мс, как в спецификации плеера. */
private const val OVERLAY_FADE_MS = 250

private val TitleTop = 30.dp
private val BackHintTop = 32.dp
private val BackHintEnd = 40.dp
private val SeekBarInset = 80.dp
private val ControlsGap = 22.dp

/** Затемнение кадра под контролами: сверху под названием, снизу под транспортом, середина чистая. */
private val TransportScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.75f),
    0.26f to Color.Transparent,
    0.5f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.88f),
)

private val TitleStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.01).em,
)
private val TimeStyle = TextStyle(
    fontFamily = Onest,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 17.sp
)

/** Плашки слева и справа от паузы — как в макете: дорожки слева, качество и серии справа. */
private val LeftStatus = listOf(PlayerTab.Subtitles, PlayerTab.Audio)
private val RightStatus = listOf(PlayerTab.Quality, PlayerTab.Episodes)

/**
 * Контролы транспорта. Скрываются прозрачностью, а не уходом из композиции: кнопка паузы держит
 * фокус и тогда, когда контролов не видно, — пульт работает одинаково при видимых и скрытых.
 */
@Composable
internal fun TransportOverlay(
    state: PlayerState,
    transport: TvTransportState,
    visible: Boolean,
    pauseModifier: Modifier
) {
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(OVERLAY_FADE_MS), label = "transportAlpha")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(TransportScrim),
    ) {
        PlayerTitle(
            state.item?.title.orEmpty(),
            playerSubtitle(state, paused = false),
            Modifier.align(Alignment.TopStart)
        )
        TvBackHint("Назад — выход", Modifier.align(Alignment.TopEnd).padding(top = BackHintTop, end = BackHintEnd))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 4.dp)) {
            TransportSeekBar(transport)
            TransportTimes(transport, Modifier.padding(horizontal = SeekBarInset))
            TransportButtons(state, transport, pauseModifier)
            TransportHints(transport, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** Название и строка под ним — общие для контролов и открытой панели. */
@Composable
internal fun PlayerTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = LocalFxColors.current
    Column(modifier.padding(start = TvMetrics.SafeHorizontal, top = TitleTop, end = TvMetrics.SafeHorizontal)) {
        Text(title, style = TitleStyle, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                style = FxType.metaRow,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/** Полоса перемотки: позиция читается при отрисовке; плашка времени — только пока перематывают. */
@Composable
private fun TransportSeekBar(transport: TvTransportState) {
    TvSeekBar(
        progress = { fraction(transport.shownPositionMs, transport.durationMs) },
        buffered = { fraction(transport.bufferedMs, transport.durationMs) },
        thumbLabel = if (transport.isScrubbing) formatPlayerTime(transport.scrubTargetMs) else null,
        modifier = Modifier.padding(horizontal = SeekBarInset),
    )
}

internal fun fraction(partMs: Long, totalMs: Long): Float =
    if (totalMs > 0) (partMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

/** Время слева и остаток справа — отдельный узел: тик раз в секунду перекомпоновывает только его. */
@Composable
internal fun TransportTimes(transport: TvTransportState, modifier: Modifier = Modifier) {
    val color = LocalFxColors.current.textSecondary
    val position = transport.shownPositionMs
    val duration = transport.durationMs
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatPlayerTime(position), style = TimeStyle, color = color)
        Text(remainingLabel(position, duration), style = TimeStyle, color = color)
    }
}

@Composable
private fun TransportButtons(state: PlayerState, transport: TvTransportState, pauseModifier: Modifier) {
    val tabs = state.panelTabs()
    Box(Modifier.fillMaxWidth().padding(horizontal = TvMetrics.SafeHorizontal)) {
        StatusChips(state, LeftStatus.filter { it in tabs }, Modifier.align(Alignment.CenterStart))
        Row(
            Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(ControlsGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvTransportGlyph(Icons.Rounded.Replay10, contentDescription = "Назад на 10 секунд")
            TvPlayPauseButton(playing = transport.isPlaying, onClick = transport::togglePlay, modifier = pauseModifier)
            TvTransportGlyph(Icons.Rounded.Forward10, contentDescription = "Вперёд на 10 секунд")
        }
        StatusChips(state, RightStatus.filter { it in tabs }, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun StatusChips(state: PlayerState, tabs: List<PlayerTab>, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { tab -> statusLabel(tab, state)?.let { TvStatusChip(it, tab.icon) } }
    }
}

/** Подсказки пульта; пока видна карточка «Далее» — как её принять или отменить. */
@Composable
private fun TransportHints(transport: TvTransportState, modifier: Modifier) {
    val hints = if (transport.autoNext.visible) {
        listOf(
            TvKeyHint(listOf(Icons.Rounded.RadioButtonChecked), "следующая серия"),
            TvKeyHint(listOf(Icons.AutoMirrored.Rounded.ArrowBack), "остаться"),
        )
    } else {
        listOf(
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowDown), "настройки и серии"),
            TvKeyHint(listOf(Icons.Rounded.KeyboardArrowLeft, Icons.Rounded.KeyboardArrowRight), "перемотка"),
        )
    }
    TvKeyHints(hints, modifier.padding(top = 4.dp))
}
