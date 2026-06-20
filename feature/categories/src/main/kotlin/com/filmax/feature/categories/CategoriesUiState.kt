package com.filmax.feature.categories

import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemPage

data class CategoriesUiState(
    val genres: List<Genre> = emptyList(),
    val selectedGenre: Genre? = null,
    val genreItems: ItemPage? = null,
    val loadingGenres: Boolean = true,
    val loadingItems: Boolean = false,
    val error: String? = null,
)
