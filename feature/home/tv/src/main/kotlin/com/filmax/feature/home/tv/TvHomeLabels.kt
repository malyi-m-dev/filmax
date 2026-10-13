// Подписи и правила отображения ТВ-Главной — чистые функции, без UI: заголовки рядов,
// плашки новинок, «высокая» оценка, число тайтлов в подборке, мета hero.
package com.filmax.feature.home.tv

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.durationLabel
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
    }

/** Сколько первых тайтлов «В тренде» несут номер: топ-10, дальше номер теряет смысл. */
internal const val RANKED_COUNT = 10

/** Плашка на карточке новинки: фильм — премьера, сериал — новинка каталога. */
internal fun freshBadge(type: ItemType): String = if (type == ItemType.MOVIE) "Премьера" else "Новинка"

/** «24 фильма», «1 фильм», «3 фильма» — подпись подборки; null, если число неизвестно. */
internal fun collectionCaption(count: Int?): String? = count?.let { "$it ${filmsWord(it)}" }

private fun filmsWord(count: Int): String {
    val lastTwo = count % HUNDRED
    val last = count % TEN
    return when {
        lastTwo in TEENS -> "фильмов"
        last == 1 -> "фильм"
        last in FEW -> "фильма"
        else -> "фильмов"
    }
}

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4

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

/** Больше трёх жанров мета-строка hero не вмещает по ширине текстового блока. */
private const val MAX_HERO_GENRES = 3

/** Мета hero после пилюли рейтинга: жанры через точку, год, длительность. */
internal fun heroMetaParts(item: Item): List<String> = buildList {
    item.genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title }.takeIf { it.isNotBlank() }?.let(::add)
    if (item.year > 0) add(item.year.toString())
    item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { add(durationLabel(it)) }
}
