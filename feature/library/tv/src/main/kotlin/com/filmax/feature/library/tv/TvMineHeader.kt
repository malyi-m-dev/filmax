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
import com.filmax.feature.library.common.MineEvent
import com.filmax.feature.library.common.MineSegment
import com.filmax.feature.library.common.MineState

/** Заголовок раздела стоит на 36dp от верха — как у всех разделов с меню. */
private val HeaderTop = 36.dp

@Composable
internal fun MineHeader(state: MineState, actions: MineActions, focus: MineFocus) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = TvMetrics.SectionStart, end = TvMetrics.SectionEnd, top = HeaderTop),
    ) {
        Text("Моё", style = FxType.screenTitle, color = LocalFxColors.current.text)
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TvSegmentedTabs(
                segments = MineSegment.entries.map { TvSegment(it.label, state.countOf(it)) },
                selectedIndex = state.segment.ordinal,
                onSelect = { index -> actions.onEvent(MineEvent.SelectSegment(MineSegment.entries[index])) },
            )
            Spacer(Modifier.weight(1f))
            SegmentAction(state, actions, focus)
        }
    }
}

/** Действие справа от сегментов; у «Продолжить» его нет, у пустой истории — тоже. */
@Composable
private fun SegmentAction(state: MineState, actions: MineActions, focus: MineFocus) {
    val action = segmentAction(state) ?: return
    TvChip(
        label = action.label,
        selected = false,
        onClick = { actions.onEvent(action.event) },
        leadingIcon = action.icon,
        modifier = Modifier.focusRequester(focus.segmentAction),
    )
}

private data class SegmentAction(val label: String, val icon: ImageVector, val event: MineEvent)

private fun segmentAction(state: MineState): SegmentAction? = when (state.segment) {
    MineSegment.CONTINUE -> null
    MineSegment.WATCHLIST ->
        SegmentAction(state.watchlistSort.label, Icons.Rounded.SwapVert, MineEvent.CycleWatchlistSort)
    MineSegment.FOLDERS ->
        SegmentAction(state.folderSort.label, Icons.AutoMirrored.Rounded.Sort, MineEvent.CycleFolderSort)
    MineSegment.HISTORY -> SegmentAction("Очистить историю", Icons.Rounded.DeleteSweep, MineEvent.AskClearHistory)
        .takeIf { state.history.isNotEmpty() }
}
