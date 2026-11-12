// Геометрия ТВ-подборки — чистые функции без UI: строка карточки в сетке и положение бегунка
// прокрутки. Подписи (число фильмов) считает модель.
package com.filmax.feature.collections.tv

/** Колонок в сетке подборки: шесть постеров 132dp с шагом 16dp — ровно ширина экрана без полей. */
internal const val COLLECTION_COLUMNS = 6

/** Строка сетки, в которой стоит карточка с индексом [index]. */
internal fun gridRow(index: Int): Int = index / COLLECTION_COLUMNS

/**
 * Бегунок прокрутки сетки: доля пройденного и доля видимого. Считается по строкам, а не по
 * пикселям: строки в сетке одной высоты, а ленивый список не знает высоту ещё не построенных.
 */
internal data class ScrollThumb(val start: Float, val length: Float)

internal fun scrollThumb(firstVisible: Int, visibleCount: Int, total: Int): ScrollThumb {
    if (total <= 0 || visibleCount >= total) return ScrollThumb(start = 0f, length = 1f)
    val hidden = (total - visibleCount).toFloat()
    return ScrollThumb(
        start = (firstVisible / hidden).coerceIn(0f, 1f),
        length = visibleCount.toFloat() / total,
    )
}
