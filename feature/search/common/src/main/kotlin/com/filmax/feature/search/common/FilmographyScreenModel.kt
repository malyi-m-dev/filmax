package com.filmax.feature.search.common

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.presentation.BaseScreenModel
import com.filmax.feature.search.common.navigation.FilmographyRoute

/**
 * Список работ одного человека. Источник выбирает [FilmographyRoute.isDirector]: работы
 * режиссёра (`searchByDirector`) или роли актёра (`searchByActor`). perPage оставляем дефолтным —
 * фильмография человека в один экран укладывается.
 */
class FilmographyScreenModel(
    savedStateHandle: SavedStateHandle,
    private val search: SearchRepository,
) : BaseScreenModel<FilmographyState, FilmographySideEffect, FilmographyEvent>(FilmographyState()) {

    private val route = savedStateHandle.toRoute<FilmographyRoute>()

    init {
        onFetchData()
    }

    override fun dispatch(event: FilmographyEvent) {
        when (event) {
            FilmographyEvent.Retry -> onFetchData()
        }
    }

    override fun onFetchData() {
        screenModelScope { _ ->
            updateState { it.copy(loading = true, error = null, heading = route.name) }
            val result = if (route.isDirector) {
                search.searchByDirector(route.name)
            } else {
                search.searchByActor(route.name)
            }
            when (result) {
                is RequestResult.Success -> updateState {
                    it.copy(loading = false, items = result.data)
                }

                is RequestResult.Error -> {
                    updateState { it.copy(loading = false, error = result.message) }
                    showError(result)
                }
            }
        }
    }
}
