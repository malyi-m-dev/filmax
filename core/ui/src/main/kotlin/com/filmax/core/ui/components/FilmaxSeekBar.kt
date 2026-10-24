package com.filmax.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.designsystem.glow

/**
 * Ползунок перемотки со временем: слева позиция, справа остаток. Пока его тянут, приходят
 * [onScrub] с позицией пальца; отпустили — [onScrubFinished]: перематывать по каждому сдвигу
 * пальца незачем. [bufferedMs] — сколько уже загружено, светлее трека. Длительность неизвестна
 * (≤ 0) — ползунок неактивен.
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@Composable
fun FilmaxSeekBar(
    positionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
    bufferedMs: Long = 0,
) {
    val progress = fraction(positionMs, durationMs)
    Column(modifier.fillMaxWidth()) {
        FilmaxSlider(
            value = progress,
            onValueChange = { onScrub((it * durationMs).toLong()) },
            onValueChangeFinished = onScrubFinished,
            enabled = durationMs > 0,
            secondary = fraction(bufferedMs, durationMs),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatPlayerTime(positionMs), style = TimeStyle, color = FilmaxColors.TextSecondary)
            val remaining = (durationMs - positionMs).coerceAtLeast(0)
            Text("−" + formatPlayerTime(remaining), style = TimeStyle, color = FilmaxColors.TextSecondary)
        }
    }
}

/**
 * Ползунок Filmax 0..1: дорожка с заливкой акцентом и белый бегунок с кольцом и свечением. Тот же,
 * что у перемотки, — в скорости воспроизведения и громкости он должен ощущаться одинаково.
 * [secondary] — вторая заливка светлее трека (загруженная часть видео).
 */
// Компонент дизайн-системы: параметры — его публичный API (см. FilmaxButton).
@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilmaxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    secondary: Float = 0f,
    trackHeight: Dp = 4.dp,
    thumbSize: Dp = 18.dp,
) {
    val palette = FilmaxTheme.palette
    val interaction = remember { MutableInteractionSource() }
    Slider(
        value = value.coerceIn(0f, 1f),
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(thumbSize + ThumbRing * 2),
        thumb = {
            Box(
                Modifier
                    .size(thumbSize + ThumbRing * 2)
                    .glow(palette.glow, CircleShape, blur = 20.dp, spread = 0.dp, offsetY = 0.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = RING_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(thumbSize)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        },
        track = { state ->
            val played = state.coercedValueAsFraction()
            Canvas(Modifier.fillMaxWidth().height(trackHeight)) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(color = TrackColor, cornerRadius = radius)
                drawRoundRect(
                    color = BufferedColor,
                    size = Size(size.width * secondary.coerceIn(0f, 1f), size.height),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = palette.accent,
                    topLeft = Offset.Zero,
                    size = Size(size.width * played, size.height),
                    cornerRadius = radius,
                )
            }
        },
    )
}

/**
 * Цвета Material-ползунков с шагами и диапазоном (год, рейтинг в фильтрах): тот же акцент и трек,
 * что у [FilmaxSlider], — ползунки приложения ощущаются одинаково, даже когда шагает Material.
 */
@Composable
fun filmaxSliderColors(): SliderColors {
    val accent = FilmaxTheme.palette.accent
    return SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = accent,
        inactiveTrackColor = TrackColor,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
}

/** Доля 0..1 значения от полного; неизвестная длительность — ноль. */
private fun fraction(value: Long, total: Long): Float =
    if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f

@OptIn(ExperimentalMaterial3Api::class)
private fun SliderState.coercedValueAsFraction(): Float {
    val span = valueRange.endInclusive - valueRange.start
    return if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
}

private val TimeStyle = FilmaxType.meta.copy(fontWeight = FontWeight.Medium)
private val TrackColor = Color.White.copy(alpha = 0.25f)
private val BufferedColor = Color.White.copy(alpha = 0.4f)
private val ThumbRing = 6.dp
private const val RING_ALPHA = 0.4f
