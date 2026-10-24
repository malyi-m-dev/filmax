// Разделы каталога в терминах выборки: что значит «Фильмы» или «4K и HDR» для витрины.

package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemType

/**
 * Выборка раздела каталога поверх состояния; без раздела — витрина по умолчанию. Раздел задаёт
 * стартовую выборку, дальше зритель меняет её чипами, как в обычном Каталоге.
 */
internal fun SearchState.withSection(section: CatalogSection?): SearchState = when (section) {
    null -> this
    CatalogSection.MOVIES -> copy(filter = ItemType.MOVIE)
    CatalogSection.SERIES -> copy(filter = ItemType.SERIES)
    CatalogSection.ANIME -> copy(filter = ItemType.ANIME)
    CatalogSection.DOCUMENTARY -> copy(filter = ItemType.DOCUMENTARY)
    CatalogSection.UHD -> copy(filters = filters.copy(only4k = true))
    CatalogSection.FRESH -> copy(sort = SortOption(CatalogSort.CREATED))
}
