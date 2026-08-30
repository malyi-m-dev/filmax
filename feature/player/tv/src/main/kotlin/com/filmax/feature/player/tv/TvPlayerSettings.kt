// Ряд настроек плеера: качество, дорожки, субтитры — и всплывающий список выбора.
package com.filmax.feature.player.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.filmax.core.tv.designsystem.TvAccent
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvOnAccent
import com.filmax.core.tv.designsystem.TvOnSurfaceVariant
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvSurface

@Composable
internal fun SettingsBar(ui: TvPlayerUiState, menu: PlayerActions, modifier: Modifier = Modifier) {
    // fillMaxWidth + центрирующая раскладка: иначе ряд центрируется лишь по обёртке AnimatedVisibility
    // и съезжает вбок. Так баблы всегда по центру снизу, сколько бы их ни было.
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        menu.items.forEachIndexed { index, action ->
            val isCursor = index == ui.settingsCursor
            TvChip(
                label = action.label,
                selected = isCursor,
                onClick = {
                    ui.settingsCursor = index
                    ui.activate(action, menu)
                },
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    // Чип-курсор поверх соседей: увеличенный масштабом чип иначе уходил ПОД
                    // следующий по порядку отрисовки.
                    .zIndex(if (isCursor) 1f else 0f)
                    .scale(if (isCursor) CURSOR_CHIP_SCALE else 1f),
            )
        }
    }
}

/**
 * Поповер выбора: галочка стоит у текущего значения, подсветка — у курсора, и курсор при открытии
 * встаёт на текущее значение (см. [TvPlayerUiState.activate]).
 */
@Composable
internal fun SettingsPopover(
    action: SettingsAction,
    menu: PlayerActions,
    cursor: Int,
    modifier: Modifier = Modifier,
) {
    val options = menu.options(action)
    val current = menu.selected(action)
    Column(
        modifier
            .width(PopoverWidth)
            .playerPanel()
            .padding(12.dp),
    ) {
        TvOverline(action.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        options.forEachIndexed { index, option ->
            SettingsRow(label = option, highlighted = index == cursor, current = option == current)
        }
    }
}

/** [highlighted] — под курсором, [current] — выбранное сейчас значение. Это разные вещи. */
@Composable
private fun SettingsRow(label: String, highlighted: Boolean, current: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(MaterialTheme.shapes.small)
            // Курсор — сплошная белая заливка (акцент), а не еле заметный серый: сразу видно, на чём
            // стоишь. Выбранное сейчас значение помечает галочка независимо от положения курсора.
            .background(if (highlighted) TvAccent else TvSurface.copy(alpha = 0f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
            color = if (highlighted) TvOnAccent else TvOnSurfaceVariant,
        )
        if (current) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = if (highlighted) TvOnAccent else TvAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Увеличение чипа-курсора в ряду настроек — белой заливки на ярком кадре мало для читаемости выбора. */
private const val CURSOR_CHIP_SCALE = 1.3f

private val PopoverWidth = 260.dp
