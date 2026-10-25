// Форматтеры ТВ-Главной — чистые функции без решений: плашка новинки по типу тайтла, число
// тайтлов подборки, мета hero. Заголовки рядов и что показывать — в HomeState.
package com.filmax.feature.home.tv

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.ui.components.durationLabel

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

/** Больше трёх жанров мета-строка hero не вмещает по ширине текстового блока. */
private const val MAX_HERO_GENRES = 3

/** Мета hero после пилюли рейтинга: жанры через точку, год, длительность. */
internal fun heroMetaParts(item: Item): List<String> = buildList {
    item.genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title }.takeIf { it.isNotBlank() }?.let(::add)
    if (item.year > 0) add(item.year.toString())
    item.duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { add(durationLabel(it)) }
}
