package com.filmax.feature.home.common

import com.filmax.core.domain.usecase.home.GetHomeFeedUseCase
import com.filmax.core.presentation.BaseScreenModel

class HomeScreenModel(
    private val getHomeFeed: GetHomeFeedUseCase,
) : BaseScreenModel<HomeState, HomeSideEffect, HomeEvent>(HomeState()) {

    init {
        onFetchData()
    }

    override fun dispatch(event: HomeEvent) {
        when (event) {
            HomeEvent.Load -> onFetchData()
        }
    }

    override fun onFetchData() {
        screenModelScope {
            updateState { it.copy(loading = true, error = null) }
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
    }
}
