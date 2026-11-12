package com.filmax.feature.collections.common

import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.presentation.format.filmsLabel
import com.filmax.core.presentation.format.titlesLabel

data class CollectionDetailState(
    /** Id подборки из маршрута — по нему экраны берут её оттенок. */
    val collectionId: Int = 0,
    /** Название из маршрута: шапка показывает его сразу, не дожидаясь первой страницы. */
    val title: String = "",
    val loading: Boolean = true,
    val items: List<TitleCard> = emptyList(),
    val error: String? = null,
    /** Последняя загруженная страница (1-based). */
    val page: Int = 0,
    val hasNextPage: Boolean = false,
    val loadingMore: Boolean = false,
    /** Обложка подборки — фон шапки на ТВ; null — kino.pub её не прислал, шапка без кадра. */
    val cover: String? = null,
    /**
     * Строка сетки, на которой стоит фокус; null — фокус в шапке или ещё нигде. Из неё ТВ решает,
     * свёрнута ли шапка, — по фокусу, а не по прокрутке: так шапка не дёргается на быстром скролле.
     */
    val focusedRow: Int? = null,
) {
    /** Шапка свёрнута в полосу, пока фокус ниже первой строки сетки. */
    val headerCollapsed: Boolean get() = isHeaderCollapsed(focusedRow)

    /**
     * Сколько тайтлов в подборке — только когда число точное: все страницы уже загружены. Пока есть
     * следующая, kino.pub знает лишь число страниц, а «примерно 40» на экране — неправда.
     */
    val titlesCount: Int? get() = items.size.takeUnless { loading || hasNextPage || items.isEmpty() }

    /** «12 тайтлов» под названием на телефоне; null — число не точное. */
    val titlesCaption: String? get() = titlesCount?.let(::titlesLabel)

    /** «12 фильмов» в шапке ТВ, как в макете; null — число не точное. */
    val filmsCaption: String? get() = titlesCount?.let(::filmsLabel)

    /** Постеры веера в шапке телефона — по порядку подборки. */
    val fanPosters: List<String> get() = items.map { it.posterUrl }

    /** С какого тайтла начинается просмотр подборки: первый в её порядке. */
    val startItemId: Int? get() = items.firstOrNull()?.id
}

/**
 * Свёрнута ли шапка подборки при фокусе на строке [focusedRow]: первая строка и кнопки шапки
 * (null) держат её раскрытой, вторая и ниже — сворачивают.
 */
fun isHeaderCollapsed(focusedRow: Int?): Boolean = focusedRow != null && focusedRow >= 1

sealed interface CollectionDetailEvent {

    /** Хвост сетки на экране: подгрузить следующую страницу подборки. */
    data object LoadMore : CollectionDetailEvent

    /** Фокус переехал на строку сетки [row]; null — ушёл в шапку. */
    data class FocusRow(val row: Int?) : CollectionDetailEvent
}

sealed interface CollectionDetailSideEffect
