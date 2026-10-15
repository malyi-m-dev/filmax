package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.favorites.FavoritesRepository
import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.domain.usecase.watching.ToggleWatchLaterUseCase
import com.filmax.core.domain.user.UserRepository
import com.filmax.core.domain.user.model.initials
import com.filmax.core.presentation.BaseScreenModel

class HomeScreenModel(
    private val getHomeFeed: GetHomeFeedUseCase,
    private val catalog: CatalogRepository,
    private val user: UserRepository,
    private val favorites: FavoritesRepository,
    private val toggleWatchLater: ToggleWatchLaterUseCase,
) : BaseScreenModel<HomeState, HomeSideEffect, HomeEvent>(HomeState()) {

    init {
        onFetchData()
        fetchUserInitials()
        observeWatchLater()
    }

    override fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Load -> onFetchData()
            is HomeEvent.LoadMoreRow -> loadMoreRow(event.id)
            HomeEvent.NextHero -> screenModelScope { _ -> updateState { current -> current.withNextHero() } }
            is HomeEvent.ToggleWatchLater -> screenModelScope { _ -> toggleWatchLater(event.item) }
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null) }
            val feed = getHomeFeed()
            updateState { current ->
                current.copy(
                    loading = false,
                    heroes = feed.heroes,
                    heroIndex = 0,
                    // Состав и порядок ленты — здесь и только здесь.
                    rows = listOf(
                        HomeRow.Continue(feed.continueWatching),
                        HomeRow.Titles(HomeRowId.TRENDING, RowPaging(feed.trending)),
                        HomeRow.Collections(RowPaging(feed.collections)),
                        HomeRow.Titles(HomeRowId.FOR_YOU, RowPaging(feed.forYou)),
                        // Новинки — сводка двух источников, листать её из каталога нечем.
                        HomeRow.Titles(HomeRowId.FRESH, RowPaging(feed.fresh, endReached = true)),
                    ),
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
            loadCollectionPreviews(feed.collections)
        }
    }

    /** Отметки «Буду смотреть» — живой поток: кнопка hero и детали меняют их с разных экранов. */
    private fun observeWatchLater() {
        screenModelScope {
            favorites.favoriteIds.collect { ids -> updateState { it.copy(watchLaterIds = ids) } }
        }
    }

    /**
     * Превью подборок: по первой странице каждой — два постера и, если подборка в одну
     * страницу, точное число тайтлов. Грузятся параллельно и приходят в ряд по одной.
     */
    private fun loadCollectionPreviews(collections: List<Collection>) {
        collections.take(COLLECTION_PREVIEWS_LIMIT).forEach { collection ->
            screenModelScope { _ ->
                val preview = catalog.collectionPreview(collection.id) ?: return@screenModelScope
                updateRows { row -> row.withPreview(collection.id, preview) }
            }
        }
    }

    /** Инициалы для аватара в шапке — best-effort, ошибки не мешают ленте. */
    private fun fetchUserInitials() {
        screenModelScope {
            (user.getProfile() as? RequestResult.Success)?.let { result ->
                updateState { it.copy(initials = result.data.initials()) }
            }
        }
    }

    private fun loadMoreRow(id: HomeRowId) {
        when (val row = state.rows.firstOrNull { it.id == id }) {
            // История приходит из фида целиком — листать нечего.
            is HomeRow.Continue, null -> Unit
            is HomeRow.Titles -> loadMoreTitles(row)
            is HomeRow.Collections -> loadMoreCollections(row)
        }
    }

    private fun loadMoreTitles(row: HomeRow.Titles) {
        val source = TITLE_SOURCES[row.id] ?: return
        if (!row.paging.canLoadMore) return
        val nextPage = row.paging.page + 1
        screenModelScope { _ ->
            updateRows { current -> current.withTitlesPaging(row.id) { paging -> paging.copy(loadingMore = true) } }
            val result = catalog.getItems(source.type, source.sort, nextPage)
            updateRows { current ->
                current.withTitlesPaging(row.id) { paging ->
                    when (result) {
                        is RequestResult.Success ->
                            paging.append(result.data.items, Item::id, result.data.pagination.hasNextPage)
                        is RequestResult.Error -> paging.copy(loadingMore = false)
                    }
                }
            }
        }
    }

    /**
     * Догрузка «Подборок» отдельно от [loadMoreTitles]: другой источник, и репозиторий отдаёт
     * список без пагинации — конец определяется пустой страницей.
     */
    private fun loadMoreCollections(row: HomeRow.Collections) {
        if (!row.paging.canLoadMore) return
        val nextPage = row.paging.page + 1
        screenModelScope { _ ->
            updateRows { current -> current.withCollectionsPaging { paging -> paging.copy(loadingMore = true) } }
            val result = catalog.getCollections(nextPage)
            updateRows { current ->
                current.withCollectionsPaging { paging ->
                    when (result) {
                        is RequestResult.Success ->
                            paging.append(result.data, Collection::id, hasNextPage = result.data.isNotEmpty())
                        is RequestResult.Error -> paging.copy(loadingMore = false)
                    }
                }
            }
        }
    }

    private suspend fun updateRows(transform: (HomeRow) -> HomeRow) {
        updateState { it.copy(rows = it.rows.map(transform)) }
    }
}

/** Сколько подборок получают превью — столько видно в ряду до первой прокрутки и чуть дальше. */
private const val COLLECTION_PREVIEWS_LIMIT = 6

/** Чем наполняется ряд тайтлов при догрузке: фид даёт стартовую горстку, дальше — каталог. */
private data class TitleSource(val type: ItemType, val sort: CatalogSort)

private val TITLE_SOURCES = mapOf(
    HomeRowId.TRENDING to TitleSource(ItemType.MOVIE, CatalogSort.VIEWS),
    HomeRowId.FOR_YOU to TitleSource(ItemType.SERIES, CatalogSort.RATING),
)

/**
 * Потолок карточек в одном ряду: бесконечный ряд на пульте — сотни нажатий вправо, а каждая
 * сотня карточек ещё и держит в памяти постеры. Дальше пусть зовёт Каталог.
 */
private const val HOME_ROW_MAX = 100

/** Ряд можно листать дальше: не занят, не кончился, не упёрся в потолок и вообще не пуст. */
private val RowPaging<*>.canLoadMore: Boolean
    get() = !loadingMore && !endReached && items.isNotEmpty() && items.size < HOME_ROW_MAX

/**
 * Приклеивает страницу к ряду: дедуп по id (страницы kino.pub пересекаются) и потолок ряда.
 * Пустая страница, отсутствие следующей или упёршийся потолок означают конец.
 */
private fun <T> RowPaging<T>.append(page: List<T>, key: (T) -> Int, hasNextPage: Boolean): RowPaging<T> {
    val seen = items.mapTo(HashSet(), key)
    val merged = (items + page.filterNot { key(it) in seen }).take(HOME_ROW_MAX)
    return copy(
        items = merged,
        page = this.page + 1,
        loadingMore = false,
        endReached = page.isEmpty() || !hasNextPage || merged.size >= HOME_ROW_MAX,
    )
}
