// Ряд вкладок нижней панели: «Серии · Аудио RU · Субтитры Выкл · Качество 1080p · Скорость 1×».
package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text

private val StripShape = RoundedCornerShape(20.dp)
private val TabShape = RoundedCornerShape(16.dp)
private val TabFocus = TvFocusStyle(TabShape, glowRadius = 16.dp, focusedScale = TvMetrics.FocusScale)
private val TabIconSize = 17.dp
private const val STRIP_ALPHA = 0.06f
private const val IDLE_VALUE_ALPHA = 0.5f
private const val SELECTED_VALUE_ALPHA = 0.75f

/** Вкладка: подпись, иконка и текущее значение рядом с подписью («RU», «Выкл»); null — без значения. */
@Immutable
data class TvPanelTab(val label: String, val icon: ImageVector, val value: String? = null)

/**
 * Ряд вкладок в стеклянной капсуле, выбранная — заливкой акцента. Вкладка выбирается фокусом, без
 * OK, как сегменты tv-material: содержимое под рядом меняется сразу, пока пульт идёт по вкладкам.
 * Ряд — группа фокуса с памятью: вход в него снизу ведёт на выбранную вкладку, а не на ближайшую
 * (ближайшая сменила бы выбор).
 */
@Composable
fun TvPanelTabs(tabs: List<TvPanelTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selectedFocus = remember { FocusRequester() }
    Row(
        modifier
            .background(Color.White.copy(alpha = STRIP_ALPHA), StripShape)
            .padding(4.dp)
            .tvFocusGroup(selectedFocus),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = index == selected
            PanelTab(
                tab = tab,
                selected = isSelected,
                onSelect = { onSelect(index) },
                modifier = if (isSelected) Modifier.focusRequester(selectedFocus) else Modifier,
            )
        }
    }
}

@Composable
private fun PanelTab(tab: TvPanelTab, selected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    val colors = LocalFxColors.current
    TvFocusSurface(
        onClick = onSelect,
        modifier = modifier.onFocusChanged { if (it.isFocused) onSelect() },
        style = TabFocus,
        colors = if (selected) {
            TvSurfaceColors(container = colors.accent, content = colors.onAccent)
        } else {
            TvSurfaceColors(container = Color.Transparent, content = colors.text)
        },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(TabIconSize))
            Text(
                tab.label,
                style = FxType.chip,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
            )
            tab.value?.let { value ->
                val valueColor = if (selected) {
                    colors.onAccent.copy(alpha = SELECTED_VALUE_ALPHA)
                } else {
                    colors.text.copy(alpha = IDLE_VALUE_ALPHA)
                }
                Text(value, style = FxType.chip, color = valueColor, maxLines = 1)
            }
        }
    }
}
