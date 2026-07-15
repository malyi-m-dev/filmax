package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.search.SearchRepository
import com.filmax.core.presentation.BaseScreenModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

private const val SEARCH_DEBOUNCE_MILLIS = 400L

class SearchScreenModel(
    private val search: SearchRepository,
) : BaseScreenModel<SearchState, SearchSideEffect, SearchEvent>(SearchState()) {

    private val queryFlow = MutableStateFlow("")

    init {
        onFetchData()
    }

    override fun dispatch(event: SearchEvent) {
        when (event) {
            is SearchEvent.QueryChange -> onQueryChange(event.query)
            is SearchEvent.FilterChange -> onFilterChange(event.filter)
            is SearchEvent.SubmitQuery -> {
                onQueryChange(event.query)
                performSearch(event.query)
            }
            SearchEvent.ClearRecent -> screenModelScope { snapshot ->
                updateState { it.copy(recentQueries = emptyList()) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    override fun onFetchData() {
        screenModelScope { snapshot ->
            updateState {
                it.copy(
                    trendingQueries = listOf(
                        "Мстители",
                        "Дюна",
                        "Офис",
                        "Ведьмак",
                        "Интерстеллар",
                        "Во все тяжкие",
                        "Оппенгеймер",
                        "Игра престолов",
                    )
                )
            }
        }
        screenModelScope {
            queryFlow
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .collect { q -> if (q.length >= 2) performSearch(q) }
        }
    }

    private fun onQueryChange(q: String) {
        queryFlow.value = q
        screenModelScope { snapshot ->
            updateState { it.copy(query = q, error = null) }
            if (q.isBlank()) updateState { it.copy(results = emptyList(), loading = false) }
        }
    }

    private fun onFilterChange(type: ItemType?) {
        screenModelScope { snapshot ->
            updateState { it.copy(filter = type) }
            val q = state.query
            if (q.length >= 2) performSearch(q)
        }
    }

    private fun performSearch(q: String) {
        screenModelScope { snapshot ->
            updateState { it.copy(loading = true) }
            when (val result = search.search(q, state.filter, perPage = 20)) {
                is RequestResult.Success -> updateState { s ->
                    val recent = (listOf(q) + s.recentQueries).distinct().take(8)
                    s.copy(loading = false, results = result.data, recentQueries = recent)
                }

                is RequestResult.Error -> updateState {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
