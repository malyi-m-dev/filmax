// focusProperties.enter — вход в ряд на запомненную карточку; API помечен экспериментальным.
@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.filmax.core.tv.designsystem

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text

/** Фокус внутри ряда: карточки ряда приглушают соседей сфокусированной. */
internal val LocalRailFocused = compositionLocalOf { false }

/**
 * Горизонтальный ряд карточек с заголовком — как ряды Google TV:
 *
 * - карточка в фокусе встаёт к левому полю [contentStart], двигается лента, а не фокус;
 * - пока фокус в ряду, соседи сфокусированной приглушены ([TvMetrics.DimmedAlpha]);
 * - ряд — группа фокуса с памятью ([RailMemory]): возврат в ряд — на ту карточку, с которой
 *   ушли, а первый вход — на первую. Не на ближайшую к той, откуда пришли: иначе «вниз» с третьей
 *   карточки ряда выше ставил фокус на вторую, и лента прыгала. Карточки отмечаются
 *   [tvRailEntry].
 *
 * [count] — число рядом с заголовком («Продолжить просмотр 4»), [subtitle] — пояснение
 * («на этой неделе»). [contentStart] — 80dp у разделов с меню, 58dp у деталей и подборки.
 */
// Компонент дизайн-системы: параметры — его публичный API (заголовок и хвост опций с дефолтами).
@Suppress("LongParameterList")
@Composable
fun TvRail(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    subtitle: String? = null,
    contentStart: Dp = TvMetrics.SectionStart,
    state: LazyListState = rememberLazyListState(),
    content: LazyListScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val memory = rememberSaveable(saver = RailMemory.Saver) { RailMemory() }
    val pivot = with(LocalDensity.current) { contentStart.toPx() }
    val pivotSpec = remember(pivot) { PivotBringIntoViewSpec(pivot) }
    Column(modifier.bringIntoViewAsWhole()) {
        RailTitle(title, count, subtitle, Modifier.padding(start = contentStart))
        CompositionLocalProvider(
            LocalRailFocused provides focused,
            LocalBringIntoViewSpec provides pivotSpec,
            LocalRailMemory provides memory,
        ) {
            LazyRow(
                state = state,
                modifier = Modifier
                    .onFocusChanged { focused = it.hasFocus }
                    .focusProperties { enter = { memory.entry(state) } }
                    .focusGroup(),
                contentPadding = PaddingValues(
                    start = contentStart,
                    end = TvMetrics.SectionEnd,
                    top = TitleGap,
                    bottom = TvMetrics.FocusInset,
                ),
                horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
                content = content,
            )
        }
    }
}

/**
 * Память ряда: на какой карточке фокус был последним. Своя, а не `focusRestorer`: тот запоминает
 * карточку только при выходе из ряда обычным поиском, а выход по `focusProperties` (состав в
 * деталях уводит «вниз» явно) прошёл бы мимо него, и возврат вёл бы на первую карточку.
 * Номер — обычное поле, не состояние: переход по ряду не должен перекомпоновывать ряд.
 */
@Stable
private class RailMemory(private var lastIndex: Int = 0) {
    private val cards = mutableMapOf<Int, FocusRequester>()

    fun card(index: Int): FocusRequester = cards.getOrPut(index) { FocusRequester() }

    fun focused(index: Int) {
        lastIndex = index
    }

    /**
     * Куда войти в ряд: на последнюю карточку в фокусе, а если её уже нет на экране (ряд
     * перезагрузился, список сменился) — на первую видимую: только видимые размещены и могут
     * принять фокус.
     */
    fun entry(state: LazyListState): FocusRequester {
        val visible = state.layoutInfo.visibleItemsInfo.map { it.index }
        val index = if (lastIndex in visible) lastIndex else visible.firstOrNull { it in cards }
        return index?.let { cards[it] } ?: FocusRequester.Default
    }

    companion object {
        /**
         * Номер переживает уход экрана из композиции — в детали, плеер и обратно, как прокрутка ряда.
         * С обычным `remember` память пропадала, и возврат с третьей карточки вёл на первую.
         * Запросчики фокуса не сохраняются: они принадлежат композиции и создаются заново.
         */
        val Saver: Saver<RailMemory, Int> = Saver(save = { it.lastIndex }, restore = { RailMemory(it) })
    }
}

private val LocalRailMemory = staticCompositionLocalOf<RailMemory?> { null }

/**
 * Отметка карточки ряда [TvRail] с номером [index]: ряд помнит, на какой карточке был фокус, и
 * при входе возвращает его туда, а в первый раз — на первую. Вне ряда ничего не делает.
 */
@Composable
fun Modifier.tvRailEntry(index: Int): Modifier {
    val memory = LocalRailMemory.current ?: return this
    return focusRequester(memory.card(index)).onFocusChanged { if (it.isFocused) memory.focused(index) }
}

/**
 * Заголовок ряда → карточки, как в макете. Это же и запас над карточками под масштаб и свечение
 * фокуса: отдельный [TvMetrics.FocusInset] сверху раздвигал заголовок и ряд до 30dp. Сверху
 * его хватает и так — ряд обрезает содержимое только по горизонтали, а по вертикали отпускает
 * ещё 15dp (`clipScrollableContainer`): 14 + 15 покрывают рост карточки на 6% и свечение.
 */
private val TitleGap = 14.dp

@Composable
private fun RailTitle(title: String, count: Int?, subtitle: String?, modifier: Modifier) {
    val colors = LocalFxColors.current
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = FxType.rowTitle, color = colors.text)
        val hint = subtitle ?: count?.toString()
        if (hint != null) Text(hint, style = FxType.meta, color = colors.textDim)
    }
}
