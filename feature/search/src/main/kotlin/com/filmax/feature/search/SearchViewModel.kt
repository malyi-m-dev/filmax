package com.filmax.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.search.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val search: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state = _state.asStateFlow()

    private val _queryFlow = MutableStateFlow("")

    init {
        _state.update {
            it.copy(
                trendingQueries = listOf(
                    "Мстители", "Дюна", "Офис", "Ведьмак",
                    "Интерстеллар", "Breaking Bad", "Опpenheimer", "Игра престолов",
                )
            )
        }
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _queryFlow
                .debounce(400)
                .distinctUntilChanged()
                .collect { q -> if (q.length >= 2) performSearch(q) }
        }
    }

    fun onQueryChange(q: String) {
        _state.update { it.copy(query = q, error = null) }
        _queryFlow.value = q
        if (q.isBlank()) _state.update { it.copy(results = emptyList(), loading = false) }
    }

    fun onFilterChange(type: ItemType?) {
        _state.update { it.copy(filter = type) }
        val q = _state.value.query
        if (q.length >= 2) performSearch(q)
    }

    fun onTrendingQueryClick(q: String) {
        onQueryChange(q)
        performSearch(q)
    }

    fun onRecentQueryClick(q: String) {
        onQueryChange(q)
        performSearch(q)
    }

    fun clearRecentQueries() {
        _state.update { it.copy(recentQueries = emptyList()) }
    }

    private fun performSearch(q: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            when (val result = search.search(q, _state.value.filter, perPage = 20)) {
                is RequestResult.Success -> _state.update { s ->
                    val recent = (listOf(q) + s.recentQueries).distinct().take(8)
                    s.copy(loading = false, results = result.data, recentQueries = recent)
                }

                is RequestResult.Error -> _state.update {
                    it.copy(loading = false, error = result.message)
                }
            }
        }
    }
}
