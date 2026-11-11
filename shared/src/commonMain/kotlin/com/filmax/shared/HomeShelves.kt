package com.filmax.shared

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogRepository
import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.GenreFilter
import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.SortOption
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.common.RequestResult

/**
 * Тематические ряды Главной на iOS — те же, что у Android (HomeTitleSources в feature:home): от
 * массового к нишевому. Жанровые ряды — выдача каталога по просмотрам (жанр принимает именно она),
 * остальные — готовые выборки источника.
 */
enum class HomeShelf(val title: String) {
    POPULAR_SERIES("Популярные сериалы"),
    CARTOONS("Мультфильмы"),
    COMEDIES("Комедии"),
    SCI_FI("Фантастика"),
    ANIME("Аниме"),
    DORAMAS("Дорамы"),
    DOCUMENTARY("Документальные фильмы"),
    TV_SHOWS("ТВ-шоу"),
    CONCERTS("Концерты"),
}

/** Страницы тематических рядов: единственное место, где ряд превращается в запрос. */
class HomeShelves(private val catalog: CatalogRepository) {

    suspend fun load(shelf: HomeShelf, page: Int): RequestResult<ItemPage> = when (shelf) {
        HomeShelf.POPULAR_SERIES -> catalog.getShortcutItems(CatalogShortcut.POPULAR, ItemType.SERIES, page)
        HomeShelf.CARTOONS -> themed(ItemType.MOVIE, ReferenceGenre.CARTOON, page)
        HomeShelf.COMEDIES -> themed(ItemType.MOVIE, ReferenceGenre.COMEDY, page)
        HomeShelf.SCI_FI -> themed(ItemType.MOVIE, ReferenceGenre.SCI_FI, page)
        HomeShelf.ANIME -> themed(ItemType.SERIES, ReferenceGenre.ANIME, page)
        HomeShelf.DORAMAS -> themed(ItemType.SERIES, ReferenceGenre.DORAMA, page)
        HomeShelf.DOCUMENTARY -> catalog.getShortcutItems(CatalogShortcut.POPULAR, ItemType.DOCUMENTARY_MOVIE, page)
        HomeShelf.TV_SHOWS -> catalog.getShortcutItems(CatalogShortcut.POPULAR, ItemType.TV_SHOW, page)
        HomeShelf.CONCERTS -> catalog.getShortcutItems(CatalogShortcut.POPULAR, ItemType.CONCERT, page)
    }

    private suspend fun themed(type: ItemType, genre: ReferenceGenre, page: Int): RequestResult<ItemPage> =
        catalog.getItems(type, GenreFilter.Reference(genre), CatalogFilters(), SortOption(CatalogSort.VIEWS), page)
}
