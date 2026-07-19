package com.filmax.core.tv.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Чип-фильтр (тип, жанр, сортировка, сезон, сегмент «Моё»).
 *
 * Выбранный чип — белая заливка с тёмным текстом, невыбранный — тёмная поверхность. Это
 * единственный способ показать выбор в монохроме, и он же отличает «выбрано» от «в фокусе»
 * (фокус — рамка с ореолом, её рисует [TvFocusCard]).
 */
@Composable
fun TvChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvFocusCard(onClick = onClick, shape = TvMetrics.ChipShape, modifier = modifier) {
        Box(
            Modifier
                .clip(TvMetrics.ChipShape)
                .background(if (selected) TvAccent else TvSurfaceContainerHigh)
                .padding(horizontal = 18.dp, vertical = 9.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) TvOnAccent else TvOnSurfaceVariant,
            )
        }
    }
}

/**
 * Ряд карточек с заголовком.
 *
 * `focusRestorer` обязателен: без него «вниз» в ряд ведёт на пространственно-ближайшую
 * карточку, а после горизонтального скролла фокус мажет мимо первой.
 *
 * [content] получает [FocusRequester], который ОБЯЗАН быть привязан к первой карточке ряда:
 * это fallback для focusRestorer. Без него первый вход в ряд отдаёт фокус D-pad-поиску, и тот
 * сажает его на пространственно-ближайшую карточку (2–3-ю — под вкладкой таб-бара или кнопкой
 * hero), а не на первую. Повторные входы по-прежнему восстанавливают последнюю сфокусированную.
 *
 * Отступы ряда живут в `contentPadding`, а не на родителе: карточка при фокусе растёт
 * (scale 1.08), и её рамка обязана поместиться внутрь viewport.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvRail(
    title: String,
    modifier: Modifier = Modifier,
    content: LazyListScope.(firstItemFocus: FocusRequester) -> Unit,
) {
    val firstItemFocus = remember { FocusRequester() }
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = TvOnSurface,
            modifier = Modifier.padding(start = TvMetrics.SafeHorizontal, bottom = 12.dp),
        )
        LazyRow(
            modifier = Modifier.focusRestorer(firstItemFocus),
            contentPadding = PaddingValues(
                start = TvMetrics.SafeHorizontal,
                end = TvMetrics.SafeHorizontal,
                top = TvMetrics.FocusInset,
                bottom = TvMetrics.FocusInset,
            ),
            horizontalArrangement = Arrangement.spacedBy(TvMetrics.CardGap),
            content = { content(firstItemFocus) },
        )
    }
}

/**
 * Надзаголовок секции: верхний регистр с трекингом («ВЫБОР РЕДАКЦИИ», «ПРОСМОТР»).
 *
 * [color] по умолчанию — вторичный: надзаголовок поверх постера (hero) должен читаться.
 * Для служебных заголовков групп на ровном фоне (Профиль) макет берёт приглушённый
 * [TvOnSurfaceDim] — он отступает на шаг назад от значений в строках.
 */
@Composable
fun TvOverline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TvOnSurfaceVariant,
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
    )
}

/** Строка меты через точки-разделители: `2024 · Фантастика · 2 ч 46 мин`. */
@Composable
fun TvMetaRow(
    parts: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        parts.filter { it.isNotBlank() }.forEach { part ->
            Text(part, style = MaterialTheme.typography.bodyLarge, color = TvOnSurfaceVariant)
        }
    }
}
