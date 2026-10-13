// Сегменты раздела («Продолжить · Буду смотреть · Папки · История»): капсула со вкладками,
// у вкладки — необязательный счётчик. Вкладка выбирается фокусом, как TabRow в tv-material.
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

private val CapsuleShape = RoundedCornerShape(20.dp)
private val SegmentShape = RoundedCornerShape(16.dp)
private val SegmentFocus = TvFocusStyle(SegmentShape, glowRadius = 16.dp, focusedScale = TvMetrics.FocusScale)

/** Капсула — стекло 6%: тише невыбранного чипа, вкладки читаются как одно целое. */
private const val CAPSULE_ALPHA = 0.06f

/** Счётчик невыбранной вкладки — приглушённым текстом, выбранной — цветом подписи на акценте. */
private const val SELECTED_COUNT_ALPHA = 0.75f

/** Вкладка: подпись и число элементов; null — счётчик не показывается. */
@Immutable
data class TvSegment(val label: String, val count: Int? = null)

/**
 * Ряд вкладок в капсуле. Вкладка выбирается фокусом, без «ОК»: пульт листает сегменты, и
 * содержимое под ними меняется сразу. Вход в ряд снизу — на выбранную вкладку, а не на
 * ближайшую по геометрии ([tvFocusGroup] с fallback).
 */
@Composable
fun TvSegmentedTabs(
    segments: List<TvSegment>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requesters = remember(segments.size) { List(segments.size) { FocusRequester() } }
    Row(
        modifier
            .background(Color.White.copy(alpha = CAPSULE_ALPHA), CapsuleShape)
            .padding(4.dp)
            .tvFocusGroup(fallback = requesters.getOrElse(selectedIndex) { FocusRequester.Default }),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        segments.forEachIndexed { index, segment ->
            SegmentTab(
                segment = segment,
                selected = index == selectedIndex,
                onFocused = { onSelect(index) },
                modifier = Modifier.focusRequester(requesters[index]),
            )
        }
    }
}

@Composable
private fun SegmentTab(segment: TvSegment, selected: Boolean, onFocused: () -> Unit, modifier: Modifier) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        // «ОК» на вкладке ничего не добавляет: она выбрана фокусом.
        onClick = onFocused,
        modifier = modifier.onFocusChanged { if (it.isFocused) onFocused() },
        style = SegmentFocus,
        colors = if (selected) {
            TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        } else {
            TvSurfaceColors(container = Color.Transparent, content = colors.text)
        },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val style = if (selected) FxType.chip.copy(fontWeight = FontWeight.SemiBold) else FxType.chip
            Text(segment.label, style = style, maxLines = 1)
            if (segment.count != null) {
                Text(
                    segment.count.toString(),
                    style = style,
                    color = if (selected) colors.onAccent.copy(alpha = SELECTED_COUNT_ALPHA) else colors.textDim,
                )
            }
        }
    }
}
