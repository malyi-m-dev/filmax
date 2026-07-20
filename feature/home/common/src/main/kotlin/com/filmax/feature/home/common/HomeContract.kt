package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.watching.model.WatchHistory

/**
 * Срез постранично догружаемого ряда главной. [page] — последняя загруженная страница каталога
 * (0 — в ряду только стартовая горстка из фида, каталог ещё не листали).
 */
data class RowPaging(
    val items: List<Item> = emptyList(),
    val page: Int = 0,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
)

data class HomeState(
    val loading: Boolean = true,
    /** Инициалы текущего пользователя для аватара в шапке (пусто — пока не загружено). */
    val initials: String = "",
    val hero: Item? = null,
    val continueWatching: List<WatchHistory> = emptyList(),
    val collections: List<Collection> = emptyList(),
    /** «В тренде» — фильмы по просмотрам, догружается при доскролле ряда. */
    val trendingRow: RowPaging = RowPaging(),
    /** «Сериалы с высоким рейтингом» — догружается при доскролле ряда. */
    val forYouRow: RowPaging = RowPaging(),
    /** Секция «Все» — постранично подгружаемый список фильмов (newest first). */
    val all: List<Item> = emptyList(),
    /** Последняя загруженная страница секции «Все» (0 — ещё не загружали). */
    val allPage: Int = 0,
    /** Идёт догрузка следующей страницы «Все». */
    val allLoadingMore: Boolean = false,
    /** Достигнут конец списка «Все» — больше не грузим. */
    val allEndReached: Boolean = false,
    val error: String? = null,
)

sealed interface HomeEvent {
    data object Load : HomeEvent

    /** Догрузить следующую страницу секции «Все» (триггерится при подходе скролла/фокуса к концу). */
    data object LoadMoreAll : HomeEvent

    /** Догрузить ряд «В тренде» (триггерится при подходе к хвосту ряда). */
    data object LoadMoreTrending : HomeEvent

    /** Догрузить ряд «Сериалы с высоким рейтингом». */
    data object LoadMoreForYou : HomeEvent
}

/** Экран пока не порождает одноразовых эффектов — навигация открытия айтема идёт колбэком из Screen. */
sealed interface HomeSideEffect
