package com.filmax.data.catalog.mapper

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.CollectionPage
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemPage
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Pagination
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.SubtitleTrack
import com.filmax.core.domain.catalog.model.Trailer
import com.filmax.core.domain.catalog.model.VideoFile
import com.filmax.data.catalog.remote.dto.AudioDto
import com.filmax.data.catalog.remote.dto.CollectionDto
import com.filmax.data.catalog.remote.dto.CollectionItemsDto
import com.filmax.data.catalog.remote.dto.DurationDto
import com.filmax.data.catalog.remote.dto.GenreDto
import com.filmax.data.catalog.remote.dto.ItemDto
import com.filmax.data.catalog.remote.dto.ItemsResponseDto
import com.filmax.data.catalog.remote.dto.MediaTrackDto
import com.filmax.data.catalog.remote.dto.PaginationDto
import com.filmax.data.catalog.remote.dto.PostersDto
import com.filmax.data.catalog.remote.dto.SubtitleDto
import com.filmax.data.catalog.remote.dto.TrailerDto
import com.filmax.data.catalog.remote.dto.VideoFileDto

fun ItemsResponseDto.toDomain(): ItemPage = ItemPage(
    items = items.map { it.toDomain() },
    pagination = pagination?.toDomain() ?: Pagination(0, 1, 20),
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
        imdb = imdbRating.toString(),
        kinopoisk = kinopoiskRating.toString(),
    ),
    posters = posters?.toDomain() ?: Posters("", "", "", ""),
    duration = duration.toDomain(),
    tracklist = videos?.map { it.toDomain() } ?: emptyList(),
    trailer = trailer?.toDomain(),
    inWatchlist = inWatchlist,
    finished = finished,
)

fun GenreDto.toDomain() = Genre(id = id, title = title)

fun PostersDto?.toDomain() = Posters(
    small = this?.small ?: "",
    medium = this?.medium ?: "",
    big = this?.big ?: "",
    wide = this?.wide ?: "",
)

// API отдаёт длительность в секундах — переводим в минуты.
fun DurationDto.toDomain() = Duration(
    averageMinutes = average?.let { it / 60 },
    totalMinutes = total?.let { it / 60 },
)

fun MediaTrackDto.toDomain() = MediaTrack(
    id = id,
    number = number,
    seasonNumber = snumber,
    title = title,
    thumbnail = thumbnail,
    durationSeconds = duration,
    files = files.map { it.toDomain() },
    audios = audios.map { it.toDomain() },
    subtitles = subtitles.map { it.toDomain() },
)

fun VideoFileDto.toDomain() = VideoFile(
    quality = quality,
    hls4 = url?.hls4,
    hls = url?.hls ?: urls?.hls,
    http = url?.http ?: urls?.http,
)

fun AudioDto.toDomain() = AudioTrack(
    id = id,
    lang = lang,
    title = title,
    channels = channels,
)

fun SubtitleDto.toDomain() = SubtitleTrack(
    lang = lang,
    url = url,
    shiftMs = shift,
)

fun TrailerDto.toDomain() = Trailer(id = id.toString(), url = url ?: "")

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
    pagination = pagination?.toDomain() ?: Pagination(0, 1, 20),
)
