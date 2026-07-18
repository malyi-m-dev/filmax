package com.filmax.data.catalog.mapper

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.Pagination
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.data.catalog.remote.dto.CollectionDto
import com.filmax.data.catalog.remote.dto.CollectionItemsDto
import com.filmax.data.catalog.remote.dto.CountryDto
import com.filmax.data.catalog.remote.dto.DurationDto
import com.filmax.data.catalog.remote.dto.GenreDto
import com.filmax.data.catalog.remote.dto.ItemDto
import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.catalog.remote.dto.PaginationDto
import com.filmax.data.catalog.remote.dto.PostersDto

// Размер страницы по умолчанию для фолбэка пагинации, когда API не вернул блок pagination.
private const val DEFAULT_PER_PAGE = 20

// API отдаёт длительность в секундах — делим на это число, чтобы получить минуты.
private const val SECONDS_PER_MINUTE = 60

fun ItemsResponseDto.toDomain(): ItemPage = ItemPage(
    items = items.map { it.toDomain() },
    pagination = pagination?.toDomain() ?: Pagination(0, 1, DEFAULT_PER_PAGE),
)

fun ItemDto.toDomain(): Item = Item(
    id = id,
    title = title,
    type = ItemType.from(type),
    year = year,
    plot = plot,
    director = director,
    cast = cast,
    country = countries.firstOrNull()?.title ?: "",
    genres = genres.map { it.toDomain() },
    rating = ItemRating(
        filmax = rating,
        filmaxPercentage = ratingPercentage.toString(),
        // API отдаёт оценки числом или null; `null?.toString()` дал бы строку "null",
        // поэтому маппим только реальные значения, отсутствие — настоящий null.
        imdb = imdbRating?.toString(),
        kinopoisk = kinopoiskRating?.toString(),
    ),
    posters = posters?.toDomain() ?: Posters("", "", "", ""),
    duration = duration.toDomain(),
    // Сериал: эпизоды лежат в seasons[].episodes (номер сезона — на родителе). Фильм: в videos.
    tracklist = if (!seasons.isNullOrEmpty()) {
        seasons.flatMap { season -> season.episodes.map { it.toDomain(season.number) } }
    } else {
        videos?.map { it.toDomain() } ?: emptyList()
    },
    trailer = trailer?.toDomain(),
    inWatchlist = inWatchlist,
    finished = finished,
    imdbId = imdb?.toString(),
)

fun GenreDto.toDomain() = Genre(id = id, title = title, type = type)

fun CountryDto.toDomain() = Country(id = id, title = title)

fun PostersDto?.toDomain() = Posters(
    small = this?.small ?: "",
    medium = this?.medium ?: "",
    big = this?.big ?: "",
    wide = this?.wide ?: "",
)

// API отдаёт длительность в секундах — переводим в минуты.
fun DurationDto.toDomain() = Duration(
    averageMinutes = average?.let { it / SECONDS_PER_MINUTE },
    totalMinutes = total?.let { it / SECONDS_PER_MINUTE },
)

fun PaginationDto.toDomain() = Pagination(
    total = total,
    current = current,
    perPage = perPage,
)

fun CollectionDto.toDomain() = Collection(
    id = id,
    title = title,
    description = description,
    posters = posters?.toDomain(),
)

fun CollectionItemsDto.toDomain() = CollectionPage(
    collection = collection?.toDomain(),
    items = items.map { it.toDomain() },
    pagination = pagination?.toDomain() ?: Pagination(0, 1, DEFAULT_PER_PAGE),
)
