// Неизменные образцы состояния Каталога и Поиска — для превью телефона. Тайтлы — настоящие Item:
// карточки и подписи к ним состояние собирает само, как на экране.
package com.filmax.feature.search.common

import com.filmax.core.domain.catalog.CatalogCount
import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogSection
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.Rating
import com.filmax.core.domain.catalog.model.RatingSource

private const val SAMPLE_YEAR = 2022
private const val SAMPLE_MINUTES = 112.0

object SearchSamples {

    private val drama = Genre(id = 8, title = "Драма")
    private val sciFi = Genre(id = 4, title = "Фантастика")

    private val titles = listOf(
        item(1, "Хроники Пограничья", ItemType.SERIES, rating = 8.4, genre = sciFi),
        item(2, "Дом на краю карты", ItemType.MOVIE, rating = 8.1, genre = drama),
        item(3, "Северный ветер", ItemType.SERIES, rating = 7.4, genre = drama),
        item(4, "Код доступа", ItemType.MOVIE, rating = 6.9, genre = sciFi),
        item(5, "Полярный круг", ItemType.DOCUMENTARY, rating = null, genre = null),
        item(6, "Хроника одного лета", ItemType.MOVIE, rating = 7.7, genre = drama),
    )

    private fun item(id: Int, title: String, type: ItemType, rating: Double?, genre: Genre?) = Item(
        id = id,
        title = title,
        type = type,
        year = SAMPLE_YEAR - id,
        plot = "",
        director = "",
        cast = "",
        country = "",
        genres = listOfNotNull(genre),
        rating = ItemRating(listOfNotNull(rating?.let { Rating(RatingSource.KINOPOISK, it) })),
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = SAMPLE_MINUTES, totalMinutes = null),
        tracklist = emptyList(),
        trailer = null,
        inWatchlist = false,
        finished = false,
    )

    private val catalogLoaded = SearchState(
        genres = listOf(drama, sciFi),
        catalogItems = titles,
        catalogEnabled = true,
        catalogCount = CatalogCount(titles.size, exact = true),
        trendingQueries = listOf("Дюна", "Офис", "Ведьмак"),
    )

    /** Витрина по умолчанию, раздел с Главной, применённые фильтры, открытая шторка, пусто, загрузка. */
    val catalog: List<SearchState> = listOf(
        catalogLoaded,
        catalogLoaded.withSection(CatalogSection.UHD),
        catalogLoaded.copy(filters = CatalogFilters(only4k = true, yearFrom = 2015), selectedGenreId = drama.id),
        catalogLoaded.copy(sheetOpen = true, sheet = CatalogSheet.SORT),
        catalogLoaded.copy(catalogItems = emptyList(), catalogCount = null),
        SearchState(loading = true, catalogEnabled = true),
    )

    private val found = catalogLoaded.copy(query = "хрони", results = titles.filter { "Хрони" in it.title })

    /** Подсказки до запроса, найденное, «ничего не нашлось» и поиск в пути. */
    val search: List<SearchState> = listOf(
        catalogLoaded.copy(recentQueries = listOf("Хроники", "Дюна")),
        found,
        found.copy(query = "нильсен", results = emptyList()),
        found.copy(results = emptyList(), loading = true),
    )
}
