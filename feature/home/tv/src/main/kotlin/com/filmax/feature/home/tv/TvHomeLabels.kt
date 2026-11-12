// Подписи и правила отображения ТВ-Главной — чистые функции, без UI: заголовки рядов и бегунок
// прокрутки. Подписи карточек (плашки новинок, число фильмов, мета hero) считает модель.
package com.filmax.feature.home.tv

import com.filmax.feature.home.common.HomeRowId

/**
 * Заголовок ряда и пояснение рядом с ним. Формулировки живут в ТВ-экране, а не в модели: на
 * десяти футах «Продолжить просмотр» читается лучше, чем телефонное «Продолжить».
 */
internal data class RailHeading(val title: String, val subtitle: String? = null)

internal val HomeRowId.heading: RailHeading
    get() = when (this) {
        HomeRowId.CONTINUE -> RailHeading("Продолжить просмотр")
        HomeRowId.TRENDING -> RailHeading("В тренде", "на этой неделе")
        HomeRowId.COLLECTIONS -> RailHeading("Подборки", "от редакции")
        // Честно: forYou — топ сериалов по рейтингу, персонализации в фиде нет.
        HomeRowId.FOR_YOU -> RailHeading("Сериалы с высоким рейтингом", "КиноПоиск 8+")
        HomeRowId.FRESH -> RailHeading("Новинки")
        HomeRowId.POPULAR_SERIES -> RailHeading("Популярные сериалы")
        HomeRowId.CARTOONS -> RailHeading("Мультфильмы")
        HomeRowId.COMEDIES -> RailHeading("Комедии")
        HomeRowId.SCI_FI -> RailHeading("Фантастика")
        // Пояснения — там, где без них ряд читается шире, чем он есть: аниме здесь только
        // сериалы, а «дорама» знакома не каждому.
        HomeRowId.ANIME -> RailHeading("Аниме", "сериалы")
        HomeRowId.DORAMAS -> RailHeading("Дорамы", "азиатские сериалы")
        HomeRowId.DOCUMENTARY -> RailHeading("Документальное кино")
        HomeRowId.TV_SHOWS -> RailHeading("ТВ-шоу")
        HomeRowId.CONCERTS -> RailHeading("Концерты")
    }

/** Сколько первых тайтлов «В тренде» несут номер: топ-10, дальше номер теряет смысл. */
internal const val RANKED_COUNT = 10

/** Бегунок индикатора прокрутки: начало и длина в долях дорожки. */
internal data class ScrollThumb(val start: Float, val length: Float) {
    companion object {
        /** Всё помещается — бегунок во всю дорожку. */
        val Whole = ScrollThumb(start = 0f, length = 1f)
    }
}

/**
 * Бегунок ленты из элементов разной высоты. [position] — номер первого видимого элемента с
 * дробной частью — сколько его уже уехало; длина — доля видимых элементов. Точной высоты
 * всей ленты ленивый список не знает, а для подсказки «где я» хватает и такой оценки.
 */
internal fun scrollThumb(position: Float, visibleCount: Int, total: Int): ScrollThumb {
    if (total <= 0 || visibleCount >= total) return ScrollThumb.Whole
    val hidden = (total - visibleCount).toFloat()
    return ScrollThumb(start = (position / hidden).coerceIn(0f, 1f), length = visibleCount.toFloat() / total)
}
