// Как разделы, сортировки и фильтры каталога kino.pub ложатся на выдачу Jellyfin.
package com.filmax.data.jellyfin

import com.filmax.core.domain.catalog.CatalogFilters
import com.filmax.core.domain.catalog.CatalogGenres
import com.filmax.core.domain.catalog.CatalogShortcut
import com.filmax.core.domain.catalog.CatalogSort
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.data.jellyfin.mapper.IsoDates
import com.filmax.data.jellyfin.remote.ItemsQuery

internal const val TYPE_MOVIE = "Movie"
internal const val TYPE_SERIES = "Series"
internal const val TYPE_BOX_SET = "BoxSet"
internal val MOVIES_AND_SERIES = listOf(TYPE_MOVIE, TYPE_SERIES)

/**
 * У сервера всего два типа видео — фильм и сериал. Разделы, которые у kino.pub отдельные типы
 * (документальное, концерты, ТВ-шоу) или жанр (аниме), здесь — жанр по имени. Имена жанров
 * приходят из метаданных медиатеки и зависят от её языка, поэтому в списке и русские, и
 * английские: TMDB пишет «Documentary» или «документальный».
 */
internal fun ItemType.toQuery(): ItemsQuery = when (this) {
    ItemType.MOVIE -> ItemsQuery(includeItemTypes = listOf(TYPE_MOVIE))
    ItemType.SERIES -> ItemsQuery(includeItemTypes = listOf(TYPE_SERIES))
    ItemType.ANIME -> ItemsQuery(includeItemTypes = MOVIES_AND_SERIES, genres = ANIME_GENRES)
    ItemType.DOCUMENTARY -> ItemsQuery(includeItemTypes = listOf(TYPE_SERIES), genres = DOCUMENTARY_GENRES)
    ItemType.DOCUMENTARY_MOVIE -> ItemsQuery(includeItemTypes = listOf(TYPE_MOVIE), genres = DOCUMENTARY_GENRES)
    ItemType.TV_SHOW -> ItemsQuery(includeItemTypes = listOf(TYPE_SERIES), genres = TV_SHOW_GENRES)
    ItemType.CONCERT -> ItemsQuery(includeItemTypes = listOf(TYPE_MOVIE), genres = CONCERT_GENRES)
    // Эфирных каналов в медиатеке нет — такой тип у сервера пуст.
    ItemType.TV -> ItemsQuery(includeItemTypes = listOf("TvChannel"))
}

/**
 * «Обновлённое» у сериала — время последней добавленной серии, а не самого сериала: иначе
 * сериал, к которому докачали новый сезон, остался бы в хвосте.
 */
internal fun CatalogSort.sortBy(type: ItemType): List<String> = when (this) {
    CatalogSort.UPDATED -> if (type.episodic) listOf(LAST_CONTENT_ADDED) else listOf(DATE_CREATED)
    CatalogSort.CREATED -> listOf(DATE_CREATED)
    CatalogSort.RATING, CatalogSort.KINOPOISK_RATING, CatalogSort.IMDB_RATING -> listOf(COMMUNITY_RATING)
    CatalogSort.VIEWS -> listOf(PLAY_COUNT)
    CatalogSort.YEAR -> listOf(PRODUCTION_YEAR, PREMIERE_DATE)
}

/**
 * Готовые выборки. «Горячее» уходит в карусель Главной, поэтому это свежие тайтлы с широким
 * фоном: вертикальный постер на всю ширину карусели выглядит кашей. «Популярное» — то, что
 * чаще смотрели в этом доме.
 */
internal fun CatalogShortcut.toQuery(type: ItemType): ItemsQuery = when (this) {
    CatalogShortcut.HOT -> type.toQuery().copy(sortBy = listOf(DATE_CREATED), imageTypes = listOf(BACKDROP))
    CatalogShortcut.POPULAR -> type.toQuery().copy(sortBy = listOf(PLAY_COUNT, COMMUNITY_RATING))
    CatalogShortcut.FRESH -> type.toQuery().copy(sortBy = CatalogSort.UPDATED.sortBy(type))
}

/**
 * Фильтры каталога. Отдельной оценки Кинопоиска у сервера нет: оба порога сводятся к общей
 * оценке сервера. Страну сервер не фильтрует — список стран для фильтра пуст, и её не выбрать.
 */
internal fun ItemsQuery.filtered(filters: CatalogFilters): ItemsQuery = copy(
    minPremiereDate = filters.yearFrom?.let(IsoDates::startOfYear),
    maxPremiereDate = filters.yearTo?.let(IsoDates::endOfYear),
    minCommunityRating = listOfNotNull(filters.kpRatingFrom, filters.imdbRatingFrom).maxOrNull()?.toDouble(),
    is4K = filters.only4k.takeIf { it },
    seriesStatus = when (filters.onlyFinished) {
        true -> listOf(STATUS_ENDED)
        false -> listOf(STATUS_CONTINUING)
        null -> emptyList()
    },
)

/**
 * Жанр выдачи. Опорные жанры kino.pub ([CatalogGenres]) экраны передают их числом — ряды Главной
 * «Комедии», «Мультфильмы», фильтр «Аниме». На сервере таких id нет, поэтому они ищутся по имени;
 * прочие id — жанры самого сервера из справочника ([JellyfinIds] выдаёт их от 2³⁰ и с опорными
 * не путает).
 */
internal fun ItemsQuery.withGenre(genreId: Int?, ids: JellyfinIds): ItemsQuery = when (genreId) {
    null -> this
    in REFERENCE_GENRES -> copy(genres = REFERENCE_GENRES.getValue(genreId))
    else -> copy(genreIds = listOf(ids.requireGuid(genreId)))
}

private const val DATE_CREATED = "DateCreated"
private const val LAST_CONTENT_ADDED = "DateLastContentAdded"
private const val COMMUNITY_RATING = "CommunityRating"
private const val PLAY_COUNT = "PlayCount"
private const val PRODUCTION_YEAR = "ProductionYear"
private const val PREMIERE_DATE = "PremiereDate"
private const val BACKDROP = "Backdrop"
private const val STATUS_ENDED = "Ended"
private const val STATUS_CONTINUING = "Continuing"

private val ANIME_GENRES = listOf("Anime", "Аниме", "аниме")
private val DOCUMENTARY_GENRES = listOf("Documentary", "Документальный", "документальный")
private val TV_SHOW_GENRES = listOf("Reality", "Talk", "Game Show", "реалити-шоу", "ток-шоу")
private val CONCERT_GENRES = listOf("Music", "Concert", "Музыка", "музыка", "Концерт")

private val REFERENCE_GENRES: Map<Int, List<String>> = mapOf(
    CatalogGenres.COMEDY to listOf("Comedy", "Комедия", "комедия"),
    CatalogGenres.SCI_FI to listOf("Science Fiction", "Sci-Fi & Fantasy", "Фантастика", "фантастика"),
    CatalogGenres.CARTOON to listOf("Animation", "Мультфильм", "мультфильм"),
    CatalogGenres.ANIME to ANIME_GENRES,
    CatalogGenres.DORAMA to listOf("Дорама", "дорама", "K-Drama"),
)
