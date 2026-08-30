// Транспорт плеера: строка прогресса с перемоткой, кнопка паузы и подсказки D-pad.
// Верхняя панель, карточки и ряд настроек — в TvPlayerOverlay и TvPlayerSettings.
package com.filmax.feature.player.tv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvFocus
import com.filmax.core.tv.designsystem.TvFocusHalo
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOnAccent
import com.filmax.core.tv.designsystem.TvOnSurface
import com.filmax.core.tv.designsystem.TvOnSurfaceDim
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvSurfaceContainerHighest
import com.filmax.feature.player.common.formatPlayerTime
import kotlin.math.roundToInt

@Composable
internal fun PlayerTransport(ui: TvPlayerUiState, menu: PlayerActions, modifier: Modifier = Modifier) {
    val hint = when {
        SettingsAction.Episodes in menu.items -> "↓ настройки и серии · ↕ показать прогресс"
        menu.items.isNotEmpty() -> "↓ настройки · ↕ показать прогресс"
        else -> "↕ показать прогресс"
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = TvMetrics.SafeHorizontal)
            .padding(bottom = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Основные контролы (скраббер + пауза/перемотка) прижаты к низу. Ряд настроек — ПОД ними,
        // за AnimatedVisibility: в транспорте его нет вовсе (место не держит), а по «вниз» он
        // раскрывается снизу и толкает основные контролы вверх. Так «вниз» открывает настройки, а
        // не приходится жать «вниз», потом несколько раз «вверх».
        Scrubber(
            positionMs = if (ui.isScrubbing) ui.scrubTargetMs else ui.positionMs,
            durationMs = ui.durationMs,
            active = ui.isScrubbing,
            modifier = Modifier.fillMaxWidth(),
        )
        TransportHints(
            isPlaying = ui.isPlaying,
            // Виртуальный фокус транспорта: пока не перематываем и не в настройках — «работаем»
            // с кнопкой паузы; при перемотке фокус-кольцо переезжает на thumb скраббера.
            focused = ui.mode == PlayerMode.Transport && !ui.isScrubbing,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = TvOnSurfaceDim,
            modifier = Modifier.padding(top = 12.dp),
        )
        AnimatedVisibility(
            visible = ui.mode == PlayerMode.Settings,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
        ) {
            SettingsBar(
                ui = ui,
                menu = menu,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

/** Полоса с временем: слева — текущее, справа — длительность; обе цифры табличные, чтобы не дёргались. */
@Composable
private fun Scrubber(positionMs: Long, durationMs: Long, active: Boolean, modifier: Modifier = Modifier) {
    val timeStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            formatPlayerTime(positionMs),
            style = timeStyle,
            color = TvOnSurface,
            modifier = Modifier.widthIn(min = 56.dp),
        )
        ScrubTrack(
            fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            active = active,
        )
        Text(
            formatPlayerTime(durationMs),
            style = timeStyle,
            color = TvOnSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 56.dp),
        )
    }
}

/**
 * Трек, заливка и thumb. Ширина известна только на месте — от неё считается позиция thumb.
 * При скраббинге ([active]) полоса и thumb заметно вырастают: видно, что перемотка «взята в руки».
 */
@Composable
private fun RowScope.ScrubTrack(fraction: Float, active: Boolean) {
    val trackHeight by animateDpAsState(if (active) ScrubTrackHeightActive else ScrubTrackHeight, label = "scrubTrack")
    val thumbSize by animateDpAsState(if (active) ScrubThumbActive else ScrubThumb, label = "scrubThumb")
    val haloSize by animateDpAsState(if (active) ScrubThumbHaloActive else ScrubThumbHalo, label = "scrubHalo")
    BoxWithConstraints(
        Modifier
            .weight(1f)
            .height(ScrubThumbHaloActive),
    ) {
        val density = LocalDensity.current
        val trackPx = with(density) { maxWidth.toPx() }

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(trackHeight)
                .clip(CircleShape)
                .background(TvAccent.copy(alpha = 0.2f)),
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraction)
                .height(trackHeight)
                .clip(CircleShape)
                .background(TvAccent),
        )
        // Кольцо фокуса при перемотке, тёмный ореол и сам thumb — тремя концентрическими кругами:
        // белая точка на светлом кадре без ореола теряется.
        val ringSize = haloSize + ScrubFocusRingExtra
        val ringPx = with(density) { ringSize.toPx() }
        CircleBox(
            size = ringSize,
            color = if (active) TvFocus else TvFocus.copy(alpha = 0f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * trackPx - ringPx / 2f).roundToInt(), 0) },
        ) {
            CircleBox(size = haloSize, color = TvFocusHalo) {
                CircleBox(size = thumbSize, color = TvAccent)
            }
        }
    }
}

/**
 * Подсказки транспорта. Это именно подсказки, а не кнопки: перемотку и паузу ведёт D-pad,
 * фокусу тут ходить не по чему. [focused] — виртуальный фокус транспорта на кнопке OK:
 * белое кольцо с тёмным зазором (белая рамка на белой кнопке иначе не видна, как у TvButton).
 */
@Composable
private fun TransportHints(isPlaying: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeekHint(label = "−${SEEK_STEPS_SEC.first()} с")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            CircleBox(size = PauseFocusOuter, color = if (focused) TvFocus else TvFocus.copy(alpha = 0f)) {
                CircleBox(size = PauseFocusInner, color = if (focused) TvFocusHalo else TvFocusHalo.copy(alpha = 0f)) {
                    CircleBox(size = PauseButtonSize, color = TvAccent) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = TvOnAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Text(
                if (isPlaying) "OK — пауза" else "OK — смотреть",
                style = MaterialTheme.typography.labelSmall,
                color = TvOnSurfaceVariant,
            )
        }
        SeekHint(label = "+${SEEK_STEPS_SEC.first()} с")
    }
}

/** Базовый шаг перемотки в кольце: с разгона реальный шаг показывает подпись по центру кадра. */
@Composable
private fun SeekHint(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .border(1.dp, TvSurfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("${SEEK_STEPS_SEC.first()}", style = MaterialTheme.typography.labelLarge, color = TvOnSurface)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = TvOnSurfaceVariant)
    }
}

/**
 * Ряд настроек. Всплывает по «вниз» через AnimatedVisibility над основными контролами; в транспорте
 * его нет вовсе, поэтому место под себя он не держит и низ панели не прыгает.
 *
 * Чипы намеренно не фокусируемые: в плеере фокус никуда не ходит, курсор ведёт обработчик клавиш.
 * Курсор рисуют белая заливка (`selected`) И увеличение — на ярком кадре одной заливки мало, чтобы
 * сразу читалось, что выбрано. `onClick` остаётся настоящим: у TV-Surface он обслуживает
 * accessibility-действие «активировать».
 */

private val ScrubTrackHeight = 6.dp
private val ScrubTrackHeightActive = 9.dp
private val ScrubThumb = 15.dp
private val ScrubThumbActive = 24.dp
private val ScrubThumbHalo = 24.dp
private val ScrubThumbHaloActive = 38.dp

/** Насколько кольцо фокуса при перемотке шире тёмного ореола thumb. */
private val ScrubFocusRingExtra = 6.dp

/** Кнопка OK и кольца её виртуального фокуса: белое снаружи и тёмный зазор вокруг круга кнопки. */
private val PauseButtonSize = 50.dp
private val PauseFocusOuter = 62.dp
private val PauseFocusInner = 56.dp
