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

/** С какой оценки пилюля рейтинга красится вторым акцентом. */
private const val HIGH_RATING = 8.0

/** Высокая ли оценка Кинопоиска — такая выделяется цветом. */
internal fun isHighRating(kinopoisk: String?): Boolean = (kinopoisk?.toDoubleOrNull() ?: 0.0) >= HIGH_RATING

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
