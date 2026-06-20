package com.filmax.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType
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
            try {
                val genres = catalog.getGenres()
                _state.update { it.copy(loadingGenres = false, genres = genres) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingGenres = false, error = e.message) }
            }
        }
    }

    fun onGenreClick(genre: Genre) {
        _state.update {
            it.copy(selectedGenre = genre, genreItems = null, loadingItems = true)
        }
        viewModelScope.launch {
            try {
                val page = catalog.getItemsByGenre(ItemType.MOVIE, genre.id, page = 1)
                _state.update { it.copy(loadingItems = false, genreItems = page) }
            } catch (e: Exception) {
                _state.update { it.copy(loadingItems = false, error = e.message) }
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
            try {
                val next = catalog.getItemsByGenre(
                    type = ItemType.MOVIE,
                    genreId = genre.id,
                    page = current.pagination.current + 1,
                )
                _state.update { s ->
                    s.copy(genreItems = next.copy(items = current.items + next.items))
                }
            } catch (_: Exception) {}
        }
    }
}
