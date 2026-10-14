// «История» в «Моё» (экран 08d): группы по дням — «Сегодня», «Вчера», дата, — в группе ряд
// карточек 16:9. Вертикально листаются группы, горизонтально — день.
package com.filmax.feature.library.tv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.filmax.core.tv.designsystem.ScrollToTopOnNavFocus
import com.filmax.core.tv.designsystem.TvCardSize
import com.filmax.core.tv.designsystem.TvDimGroup
import com.filmax.core.tv.designsystem.TvMetrics
import com.filmax.core.tv.designsystem.TvOverline
import com.filmax.core.tv.designsystem.TvProgressCard
import com.filmax.core.tv.designsystem.TvScreenFocus
import com.filmax.core.tv.designsystem.bringIntoViewAsWhole
import com.filmax.core.tv.designsystem.fxEnter
import com.filmax.core.tv.designsystem.rememberTvScrollFades
import com.filmax.core.tv.designsystem.tvFocusGroup
import com.filmax.core.tv.designsystem.tvScrollFades
import com.filmax.feature.library.common.HistoryDay

/** День в фокусе встаёт заголовком на 78dp от верха списка — как ряды Главной. */
private val DayPivot = 78.dp

/** Низ списка гаснет на 50dp — следующий день виден, но не спорит с текущим. */
private val HistoryFade = 50.dp

/** Надзаголовок дня → карточки; между днями — зазор после подписи карточек. */
private val OverlineGap = 12.dp
private val DayGap = 10.dp
private val CardGap = 20.dp

/**
 * Шаг появления карточки: день — слева направо, следующий день — на шаг позже. Дальние карточки
 * (доехали прокруткой) ждут не дольше [MAX_ENTER_STEP] шагов — иначе появлялись бы с паузой.
 */
private fun enterStep(dayIndex: Int, index: Int): Int = (dayIndex + index).coerceAtMost(MAX_ENTER_STEP)

private const val MAX_ENTER_STEP = 6

/** Первый день стоит под сегментами на том же уровне, что и сетки других сегментов. */
private val ListTop = 20.dp

/**
 * Список дней. Каждый день — ряд с памятью фокуса ([tvFocusGroup]): возврат в день — на карточку,
 * с которой ушли; в ряду карточка в фокусе встаёт к полю раздела, соседи приглушены.
 */
@Composable
internal fun MineHistory(days: List<HistoryDay>, actions: MineActions, screenFocus: TvScreenFocus) {
    if (days.isEmpty()) {
        MineEmpty(MineEmptyText.History, Icons.Rounded.History)
        return
    }
    val listState = rememberLazyListState()
    ScrollToTopOnNavFocus(listState)
    val fades = rememberTvScrollFades(listState)
    PivotScroll(DayPivot) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().tvScrollFades(fades, HistoryFade),
            contentPadding = PaddingValues(top = ListTop, bottom = TvMetrics.SafeVertical + TvMetrics.FocusInset),
            verticalArrangement = Arrangement.spacedBy(DayGap),
        ) {
            days.forEachIndexed { dayIndex, day ->
                item(key = "day:${day.label}") { HistoryDayRow(day, dayIndex, actions, screenFocus) }
            }
        }
    }
}

/**
 * День: надзаголовок и ряд карточек — на опорную точку встаёт весь блок, а не карточка. Первый
 * вход в ряд — на первую карточку дня, а не на ближайшую к той, с которой пришли.
 */
@Composable
private fun HistoryDayRow(day: HistoryDay, dayIndex: Int, actions: MineActions, screenFocus: TvScreenFocus) {
    val firstCard = remember { FocusRequester() }
    Column(Modifier.bringIntoViewAsWhole()) {
        TvOverline(day.label, accent = false, modifier = Modifier.padding(start = TvMetrics.SectionStart))
        PivotScroll(TvMetrics.SectionStart) {
            TvDimGroup {
                LazyRow(
                    modifier = Modifier.tvFocusGroup(fallback = firstCard),
                    contentPadding = PaddingValues(
                        start = TvMetrics.SectionStart,
                        end = TvMetrics.SectionEnd,
                        top = OverlineGap,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    itemsIndexed(day.entries, key = { _, dayEntry -> dayEntry.entry.itemId }) { index, (entry, time) ->
                        TvProgressCard(
                            title = entry.title,
                            meta = historyMeta(entry.progress, time),
                            posterUrl = entry.wideOrPoster,
                            progress = entry.progress?.fraction ?: 0f,
                            onClick = { actions.onOpenItem(entry.itemId) },
                            size = TvCardSize.Small,
                            modifier = Modifier
                                .fxEnter(FIRST_CARD_ENTER + enterStep(dayIndex, index))
                                .then(screenFocus.item(historyKey(entry.itemId)))
                                .then(if (index == 0) Modifier.focusRequester(firstCard) else Modifier),
                        )
                    }
                }
            }
        }
    }
}
