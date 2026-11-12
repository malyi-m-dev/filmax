// Подписи Каталога и Поиска — готовые строки из состояния, без UI: заголовок раздела, сводка выборки
// со счётчиком, чипы, «Ещё N», подсказки поиска. Раньше их считала вёрстка ТВ и телефона, каждая
// свою половину; теперь экраны берут готовое, а тесты видят ровно то, что на экране.
package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogCount
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.presentation.format.label

/** Заголовок Каталога: открытый с Главной раздел или сама вкладка. */
val SearchState.title: String
    get() = section?.label ?: "Каталог"

/** Выбранный жанр; null — жанр не выбран или справочник ещё не пришёл. */
val SearchState.selectedGenre: Genre?
    get() = genres.firstOrNull { it.id == selectedGenreId }

/**
 * Можно ли выбрать жанр. У «Аниме» жанр уже задан самим типом — второй параметр genre API не
 * принимает; без справочника выбирать не из чего.
 */
val SearchState.genrePickable: Boolean
    get() = genres.isNotEmpty() && filter != ItemType.ANIME

/** Применённые фильтры чипами по одному: «✕» на каждом снимает только его. */
val SearchState.appliedFilters: List<AppliedFilter>
    get() = filters.applied(countries)

/**
 * Сводка над сеткой: «Фантастика · 42 результата», «Сериалы · около 60 результатов». Чип «Все»
 * в сводку не попадает — он ничего не сужает. Пока витрина грузится, счётчика нет.
 */
val SearchState.catalogSummary: String
    get() = listOfNotNull(
        TypeOptions.firstOrNull { (type, _) -> type == filter && type != null }?.second,
        selectedGenre?.title,
        catalogCount?.let(::countLabel),
    ).joinToString(" · ")

/** Чип сортировки: поле и стрелка, если порядок обратный привычному («Год ↑»). */
val SearchState.sortChip: String
    get() = if (sort.ascending) "${sortLabel(sort.field)} ↑" else sortLabel(sort.field)

/** Чип фильтров: «Фильтры» или «Фильтры · 2» — сколько применено. */
val SearchState.filtersChip: String
    get() = if (filters.activeCount > 0) "Фильтры · ${filters.activeCount}" else "Фильтры"

/** «Найдено 12» — над результатами поиска телефона. */
val SearchState.foundLabel: String
    get() = "Найдено ${resultCards.size}"

/** «Ещё 6 результатов» — над постерами под лучшим совпадением. */
val SearchState.moreResults: String
    get() = otherResults.size.let { count -> "Ещё $count ${resultsWord(count)}" }

/** Мета лучшего совпадения: «Сериал · 2012 · Драма»; null — сказать нечего. */
val SearchState.bestMatchMeta: String?
    get() = bestMatch?.let { match -> listOfNotNull(match.typeAndYear, match.genre).joinToString(" · ") }
        ?.ifBlank { null }

/** Что сказать, пока карточек нет: набрать запрос, подождать или — что ничего не нашлось. */
val SearchState.searchHint: String
    get() = when {
        query.trim().length < MIN_QUERY_LENGTH -> "Наберите название — результаты появятся здесь"
        loading -> "Ищем…"
        else -> "По запросу «${query.trim()}» ничего не нашлось"
    }

/**
 * Счётчик выдачи. Точный — «42 результата»; оценка по числу страниц — «около 62 результатов»:
 * kino.pub отдаёт страницы, а не тайтлы, и до последней страницы честнее сказать «около».
 */
fun countLabel(count: CatalogCount): String {
    val value = count.value
    return if (count.exact) "$value ${resultsWord(value)}" else "около $value ${resultsAfterAbout(value)}"
}

/** «1 результат», «2 результата», «5 результатов». */
private fun resultsWord(count: Int): String = when {
    count % HUNDRED in TEENS -> "результатов"
    count % TEN == 1 -> "результат"
    count % TEN in FEW -> "результата"
    else -> "результатов"
}

/** После «около» — родительный падеж: «около 21 результата», «около 62 результатов». */
private fun resultsAfterAbout(count: Int): String =
    if (count % TEN == 1 && count % HUNDRED !in TEENS) "результата" else "результатов"

private const val TEN = 10
private const val HUNDRED = 100
private val TEENS = 11..14
private val FEW = 2..4
