package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogSort
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
        }
    }

    override fun onFetchData() {
        screenModelScope {
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
                    collections = feed.collections,
                    trending = feed.trending,
                    forYou = feed.forYou,
                    error = feed.error,
                )
            }
            // Если контент не загрузился вовсе — показываем модалку ошибки.
            if (feed.hero == null && feed.trending.isEmpty() && feed.error != null) {
                showError(feed.error)
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
        screenModelScope {
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
}
