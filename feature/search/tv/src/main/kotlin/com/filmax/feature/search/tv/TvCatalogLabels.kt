// Подписи ТВ-поиска, которым нужен цвет, — подсветка совпадения в названии и карточка лучшего
// совпадения. Сами строки (сводка, чипы, «Ещё N», подсказки) считает модель: SearchLabels.
package com.filmax.feature.search.tv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.tv.designsystem.TvSpotlight
import com.filmax.feature.search.common.matchRange

/** Название с подсветкой совпадения с [query] цветом [highlight]; нет совпадения — как есть. */
internal fun highlightedTitle(title: String, query: String, highlight: Color): AnnotatedString {
    val match = matchRange(title, query) ?: return AnnotatedString(title)
    return buildAnnotatedString {
        append(title.substring(0, match.first))
        withStyle(SpanStyle(color = highlight)) { append(title.substring(match)) }
        append(title.substring(match.last + 1))
    }
}

/** Лучшее совпадение крупной карточкой: кадр, название с подсветкой, оценка и [meta]. */
internal fun bestMatchSpotlight(match: TitleCard, meta: String?, query: String, highlight: Color) = TvSpotlight(
    title = highlightedTitle(match.title, query, highlight),
    meta = meta,
    imageUrl = match.widePosterUrl,
    rating = match.rating,
    ratingHighlighted = match.ratingHigh,
)
