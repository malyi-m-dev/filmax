package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.initials
import com.filmax.core.presentation.BaseScreenModel

class HomeScreenModel(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val catalog: CatalogRepository,
    private val user: UserRepository,
) : BaseScreenModel<HomeState, HomeSideEffect, HomeEvent>(HomeState()) {

    init {
        onFetchData()
        fetchUserInitials()
    }

    /** Инициалы для аватара в шапке — best-effort, ошибки не мешают ленте. */
    private fun fetchUserInitials() {
        screenModelScope {
            (user.getProfile() as? RequestResult.Success)?.let { result ->
                updateState { it.copy(initials = result.data.initials()) }
            }
        }
    }

    override fun dispatch(event: HomeEvent) {
        when (event) {
            HomeEvent.Load -> onFetchData()
            HomeEvent.LoadMoreAll -> loadMoreAll()
            HomeEvent.LoadMoreTrending -> loadMoreRow(
                type = ItemType.MOVIE,
                sort = CatalogSort.VIEWS,
                read = { it.trendingRow },
                write = { current, row -> current.copy(trendingRow = row) },
            )

            HomeEvent.LoadMoreForYou -> loadMoreRow(
                type = ItemType.SERIES,
                sort = CatalogSort.RATING,
                read = { it.forYouRow },
                write = { current, row -> current.copy(forYouRow = row) },
            )

            HomeEvent.LoadMoreCollections -> loadMoreCollections()
        }
    }

    override fun onFetchData() {
        screenModelScope { snapshot ->
            updateState {
                it.copy(
                    loading = true,
                    error = null,
                    // Сбрасываем секцию «Все» — заново начнём с первой страницы.
                    all = emptyList(),
                    allPage = 0,
                    allLoadingMore = false,
                    allEndReached = false,
                )
            }
            val feed = getHomeFeed()
            updateState { s ->
                s.copy(
                    loading = false,
                    hero = feed.hero,
                    continueWatching = feed.continueWatching,
                    collectionsRow = RowPaging(items = feed.collections),
                    trendingRow = RowPaging(items = feed.trending),
                    forYouRow = RowPaging(items = feed.forYou),
                    error = feed.error,
                )
            }
            when {
                // Контент из кэша при офлайне (issue #42) — показываем баннер, не модалку.
                feed.fromCache -> showOfflineBanner()
                // Пусто + ошибка — блокирующая модалка.
                !feed.hasContent && feed.error != null -> showError(feed.error)
                // Свежие данные приехали — прячем баннер, если висел.
                else -> dismissOfflineBanner()
            }
        }
        // Первая страница секции «Все» грузится параллельно ленте.
        loadMoreAll()
    }

    /**
     * Догружает следующую страницу секции «Все» (все фильмы, newest first). Идемпотентна:
     * пока идёт загрузка или достигнут конец — повторный вызов игнорируется, поэтому её
     * безопасно дёргать из UI при подходе скролла/фокуса к концу списка.
     */
    private fun loadMoreAll() {
        if (state.allLoadingMore || state.allEndReached) return
        val nextPage = state.allPage + 1
        screenModelScope { snapshot ->
            updateState { it.copy(allLoadingMore = true) }
            when (val result = catalog.getItems(ItemType.MOVIE, CatalogSort.UPDATED, nextPage)) {
                is RequestResult.Success -> updateState { s ->
                    // Дедуп по id — на случай пересечения страниц при обновлении каталога.
                    val seen = s.all.mapTo(HashSet()) { it.id }
                    val merged = s.all + result.data.items.filter { it.id !in seen }
                    s.copy(
                        all = merged,
                        allPage = nextPage,
                        allLoadingMore = false,
                        allEndReached = !result.data.pagination.hasNextPage || result.data.items.isEmpty(),
                    )
                }

                is RequestResult.Error -> updateState { it.copy(allLoadingMore = false) }
            }
        }
    }

    /**
     * Догрузка горизонтального ряда главной. Идемпотентна (guard на загрузку/конец), с потолком
     * [HOME_ROW_MAX]: бесконечный ряд на пульте — сотни нажатий вправо, а каждая сотня карточек —
     * лишняя память под постеры; дальше пусть зовёт Каталог. Стартовая горстка приходит из фида
     * (page = 0), первая догрузка тянет страницу 1 целиком — пересечение срезает дедуп по id.
     */
    private fun loadMoreRow(
        type: ItemType,
        sort: CatalogSort,
        read: (HomeState) -> RowPaging<Item>,
        write: (HomeState, RowPaging<Item>) -> HomeState,
    ) {
        val row = read(state)
        val busy = row.loadingMore || row.endReached
        val capped = row.items.isEmpty() || row.items.size >= HOME_ROW_MAX
        if (busy || capped) return
        val nextPage = row.page + 1
        screenModelScope { _ ->
            updateState { write(it, read(it).copy(loadingMore = true)) }
            when (val result = catalog.getItems(type, sort, nextPage)) {
                is RequestResult.Success -> updateState { s ->
                    val current = read(s)
                    val seen = current.items.mapTo(HashSet()) { it.id }
                    val merged = (current.items + result.data.items.filter { it.id !in seen })
                        .take(HOME_ROW_MAX)
                    val exhausted = result.data.items.isEmpty() ||
                        !result.data.pagination.hasNextPage ||
                        merged.size >= HOME_ROW_MAX
                    write(
                        s,
                        current.copy(
                            items = merged,
                            page = nextPage,
                            loadingMore = false,
                            endReached = exhausted,
                        ),
                    )
                }

                is RequestResult.Error -> updateState { write(it, read(it).copy(loadingMore = false)) }
            }
        }
    }

    /**
     * Догрузка «Подборок». Отдельно от [loadMoreRow]: другой источник, и репозиторий отдаёт
     * список без пагинации — конец определяется пустой страницей (или потолком [HOME_ROW_MAX]).
     */
    private fun loadMoreCollections() {
        val row = state.collectionsRow
        val busy = row.loadingMore || row.endReached
        val capped = row.items.isEmpty() || row.items.size >= HOME_ROW_MAX
        if (busy || capped) return
        val nextPage = row.page + 1
        screenModelScope { _ ->
            updateState { it.copy(collectionsRow = it.collectionsRow.copy(loadingMore = true)) }
            when (val result = catalog.getCollections(nextPage)) {
                is RequestResult.Success -> updateState { s ->
                    val current = s.collectionsRow
                    val seen = current.items.mapTo(HashSet()) { it.id }
                    val merged = (current.items + result.data.filter { it.id !in seen })
                        .take(HOME_ROW_MAX)
                    s.copy(
                        collectionsRow = current.copy(
                            items = merged,
                            page = nextPage,
                            loadingMore = false,
                            endReached = result.data.isEmpty() || merged.size >= HOME_ROW_MAX,
                        ),
                    )
                }

                is RequestResult.Error -> updateState {
                    it.copy(collectionsRow = it.collectionsRow.copy(loadingMore = false))
                }
            }
        }
    }

    private companion object {
        /** Потолок карточек в одном ряду главной — дальше зовёт Каталог. */
        const val HOME_ROW_MAX = 100
    }
}
