// Шапка «Моё»: заголовок, сегменты со счётчиками и действие текущего сегмента справа
// (сортировка, «Очистить историю»). Не прокручивается вместе с сеткой.
package com.filmax.feature.library.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.filmax.core.tv.designsystem.FxType
import com.filmax.core.tv.designsystem.LocalFxColors
import com.filmax.core.tv.designsystem.TvChip
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvSegment
import com.filmax.core.tv.designsystem.TvSegmentedTabs
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

/** Заголовок раздела стоит на 36dp от верха — как у всех разделов с меню. */
private val HeaderTop = 36.dp

@Composable
internal fun MineHeader(state: MineState, onEvent: (MineEvent) -> Unit, focus: MineFocus) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = TvMetrics.SectionStart, end = TvMetrics.SectionEnd, top = HeaderTop),
    ) {
        // Заголовок и сегменты появляются первыми, карточки сетки — следом (FIRST_CARD_ENTER).
        Text("Моё", style = FxType.screenTitle, color = LocalFxColors.current.text, modifier = Modifier.fxEnter(0))
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp).fxEnter(1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvSegmentedTabs(
                segments = MineSegment.entries.map { TvSegment(it.label, state.countOf(it)) },
                selectedIndex = state.segment.ordinal,
                onSelect = { index -> onEvent(MineEvent.SelectSegment(MineSegment.entries[index])) },
            )
            Spacer(Modifier.weight(1f))
            SegmentAction(state, onEvent, focus)
        }
    }
}

/**
 * Действие справа от сегментов — чип сортировки или «Очистить историю»; у «Продолжить» его нет,
 * у пустой истории — тоже. Какой чип рисовать, зависит только от сегмента — это раскладка шапки.
 */
@Composable
private fun SegmentAction(state: MineState, onEvent: (MineEvent) -> Unit, focus: MineFocus) {
    val chip = Modifier.focusRequester(focus.segmentAction)
    when (state.segment) {
        MineSegment.CONTINUE -> Unit
        MineSegment.WATCHLIST -> ActionChip(state.watchlistSort.label, Icons.Rounded.SwapVert, chip) {
            onEvent(MineEvent.CycleWatchlistSort)
        }
        MineSegment.FOLDERS -> ActionChip(state.folderSort.label, Icons.AutoMirrored.Rounded.Sort, chip) {
            onEvent(MineEvent.CycleFolderSort)
        }
        MineSegment.HISTORY -> if (state.history.isNotEmpty()) {
            ActionChip("Очистить историю", Icons.Rounded.DeleteSweep, chip) { onEvent(MineEvent.AskClearHistory) }
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    TvChip(label = label, selected = false, onClick = onClick, leadingIcon = icon, modifier = modifier)
}
