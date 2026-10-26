package com.filmax.feature.collections.common

import com.filmax.core.domain.catalog.model.Item

/**
 * Состояние экрана подборки. Название и id приходят из маршрута и лежат здесь же: экран не читает
 * маршрут, а оттенок шапки на обеих платформах выводится из [collectionId] дизайн-системой.
 */
data class CollectionDetailState(
    val collectionId: Int = 0,
    val title: String = "",
    val loading: Boolean = true,
    val items: List<Item> = emptyList(),
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
    /** Шапка ТВ свёрнута в полосу, пока фокус ниже первой строки сетки. */
    val headerCollapsed: Boolean = false,
    /**
     * Сколько тайтлов в подборке — только когда число точное: все страницы уже загружены. Пока есть
     * следующая, kino.pub знает лишь число страниц, а «примерно 40» на экране — неправда.
     */
    val titlesCount: Int? = null,
    /** Подпись шапки ТВ: «24 фильма»; null — число ещё не точное. */
    val caption: String? = null,
    /** С какого тайтла начинается просмотр подборки: первый в её порядке. null — смотреть нечего. */
    val startItemId: Int? = null,
) {
    fun withFirstPage(items: List<Item>, hasNextPage: Boolean, cover: String?): CollectionDetailState =
        copy(loading = false, items = items, page = 1, hasNextPage = hasNextPage, loadingMore = false, cover = cover)
            .counted()

    fun withNextPage(items: List<Item>, page: Int, hasNextPage: Boolean): CollectionDetailState =
        copy(loadingMore = false, items = items, page = page, hasNextPage = hasNextPage).counted()

    fun withFocusedRow(row: Int?): CollectionDetailState =
        copy(focusedRow = row, headerCollapsed = isHeaderCollapsed(row))

    /** Счётчик и старт просмотра — из списка и его полноты; зовётся после каждой смены списка. */
    private fun counted(): CollectionDetailState {
        val count = items.size.takeUnless { loading || hasNextPage || items.isEmpty() }
        return copy(
            titlesCount = count,
            caption = count?.let(::filmsCaption),
            startItemId = items.firstOrNull()?.id,
        )
    }
}

/**
 * Свёрнута ли шапка подборки при фокусе на строке [focusedRow]: первая строка и кнопки шапки
 * (null) держат её раскрытой, вторая и ниже — сворачивают.
 */
fun isHeaderCollapsed(focusedRow: Int?): Boolean = focusedRow != null && focusedRow >= 1

/** «24 фильма», «1 фильм», «11 фильмов» — подпись шапки подборки. */
fun filmsCaption(count: Int): String {
    val lastTwo = count % HUNDRED
    val last = count % TEN
    val word = when {
        lastTwo in TEENS -> "фильмов"
        last == 1 -> "фильм"
        last in FEW -> "фильма"
        else -> "фильмов"
    }
    return "$count $word"
}

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4

sealed interface CollectionDetailEvent {

    /** Хвост сетки на экране: подгрузить следующую страницу подборки. */
    data object LoadMore : CollectionDetailEvent

    /** Фокус переехал на строку сетки [row]; null — ушёл в шапку. */
    data class FocusRow(val row: Int?) : CollectionDetailEvent

    data class OpenItem(val itemId: Int) : CollectionDetailEvent

    /** «Смотреть подряд» — плеер с первого тайтла подборки. */
    data object PlayAll : CollectionDetailEvent
    data object Back : CollectionDetailEvent
}

/** Одноразовых эффектов у подборки нет: переходы модель отдаёт навигатору сама. */
sealed interface CollectionDetailSideEffect
