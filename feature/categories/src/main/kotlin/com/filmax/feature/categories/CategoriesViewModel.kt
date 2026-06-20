package com.filmax.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult
import com.filmax.core.domain.common.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesUiState())
    val state = _state.asStateFlow()

    init {
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            when (val result = catalog.getGenres()) {
                is RequestResult.Success ->
                    _state.update { it.copy(loadingGenres = false, genres = result.data) }

                is RequestResult.Error ->
                    _state.update { it.copy(loadingGenres = false, error = result.message) }
            }
        }
    }

    fun onGenreClick(genre: Genre) {
        _state.update {
            it.copy(selectedGenre = genre, genreItems = null, loadingItems = true)
        }
        viewModelScope.launch {
            when (val result = catalog.getItemsByGenre(ItemType.MOVIE, genre.id, page = 1)) {
                is RequestResult.Success ->
                    _state.update { it.copy(loadingItems = false, genreItems = result.data) }

                is RequestResult.Error ->
                    _state.update { it.copy(loadingItems = false, error = result.message) }
            }
        }
    }

    fun onDismissSheet() {
        _state.update { it.copy(selectedGenre = null, genreItems = null) }
    }

    fun loadMoreGenreItems() {
        val current = _state.value.genreItems ?: return
        if (!current.pagination.hasNextPage) return
        val genre = _state.value.selectedGenre ?: return
        viewModelScope.launch {
            catalog.getItemsByGenre(
                type = ItemType.MOVIE,
                genreId = genre.id,
                page = current.pagination.current + 1,
            ).onSuccess { next ->
                _state.update { s ->
                    s.copy(genreItems = next.copy(items = current.items + next.items))
                }
            }
        }
    }
}
