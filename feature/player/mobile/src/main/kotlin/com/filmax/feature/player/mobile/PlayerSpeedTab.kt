// Вкладка «Скорость»: крупное значение, ползунок по всем скоростям плеера и чипы частых скоростей.

package com.filmax.feature.player.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.filmax.core.designsystem.FilmaxColors
import com.filmax.core.designsystem.FilmaxTheme
import com.filmax.core.designsystem.FilmaxType
import com.filmax.core.ui.components.FilmaxChip
import com.filmax.core.ui.components.FilmaxChipSize
import com.filmax.core.ui.components.FilmaxSlider
import com.filmax.feature.player.common.PlaybackSpeeds
import com.filmax.feature.player.common.PlayerEvent
import com.filmax.feature.player.common.PlayerState
import com.filmax.feature.player.common.PresetSpeeds
import com.filmax.feature.player.common.speedValue
import kotlin.math.roundToInt

/** Знак «×» в значении скорости — акцентом, само число — белым. */
private const val TIMES = "×"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpeedTab(state: PlayerState, onEvent: (PlayerEvent) -> Unit) {
    val speeds = PlaybackSpeeds.options
    val index = speeds.indexOfFirst { it.value == state.currentSpeed }.coerceAtLeast(0)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val value = speedValue(state.currentSpeed)
        Text(
            buildAnnotatedString {
                append(value.removeSuffix(TIMES))
                withStyle(SpanStyle(color = FilmaxTheme.palette.accentText)) { append(TIMES) }
            },
            style = FilmaxType.bigValue,
            color = FilmaxColors.Text,
        )
        // Тон сохраняет сам плеер (setPlaybackSpeed меняет только темп) — об этом и говорим.
        Text("Высота голоса сохраняется", style = FilmaxType.metaSmall, color = FilmaxColors.TextDim)
        FilmaxSlider(
            value = index / (speeds.size - 1).toFloat(),
            onValueChange = { fraction ->
                val chosen = speeds[(fraction * (speeds.size - 1)).roundToInt()]
                if (chosen.value != state.currentSpeed) onEvent(PlayerEvent.SetSpeed(chosen.value))
            },
            trackHeight = 6.dp,
            thumbSize = 24.dp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(speeds.first(), speeds.first { it.value == PlaybackSpeeds.NormalSpeed }, speeds.last()).forEach {
                Text(speedValue(it.value), style = FilmaxType.metaSmall, color = FilmaxColors.TextDim)
            }
        }
        FlowRow(
            Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PresetSpeeds.forEach { speed ->
                FilmaxChip(
                    label = speedValue(speed),
                    selected = speed == state.currentSpeed,
                    onClick = { onEvent(PlayerEvent.SetSpeed(speed)) },
                    size = FilmaxChipSize.Small,
                )
            }
        }
    }
}
