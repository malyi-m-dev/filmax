// Неизменные образцы Деталей — фильм и сериал с начатой серией — для превью телефона и ТВ. Сериал
// собран тем же calculateSeriesData, что в модели: превью показывает настоящую кнопку «Продолжить».
package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.RatingSource

private const val EPISODE_SECONDS = 2_700
private const val WATCHED_SECONDS = 1_260
private const val MOVIE_MINUTES = 131.0
private const val EPISODE_ID_STEP = 100
private const val SEASONS = 2

/** Серия первого сезона, на которой остановились. */
private const val RESUME_EPISODE = 3
private const val EPISODES_PER_SEASON = 4

object DetailsSamples {

    private val movie = Item(
        id = 1,
        title = "Дом на краю карты",
        type = ItemType.MOVIE,
        year = 2022,
        plot = "Картограф находит на старой карте город, которого нет ни в одном атласе, и едет его искать.",
        director = "Ирина Ланская",
        cast = "Анна Северова, Пётр Ильин, Мария Холм",
        country = "Норвегия, Дания",
        genres = listOf(Genre(8, "Драма"), Genre(12, "Приключения")),
        rating = ItemRating.of(RatingSource.KINOPOISK to "8.1", RatingSource.IMDB to "7.6"),
        posters = Posters(small = "", medium = "", big = "", wide = null),
        duration = Duration(averageMinutes = MOVIE_MINUTES, totalMinutes = null),
        tracklist = emptyList(),
        trailer = null,
        inWatchlist = false,
        finished = false,
    )

    /** Две серии первого сезона досмотрены, третья начата. */
    private val tracks = (1..SEASONS).flatMap { season ->
        (1..EPISODES_PER_SEASON).map { number ->
            val status = when {
                season == 1 && number < RESUME_EPISODE -> 1
                season == 1 && number == RESUME_EPISODE -> 0
                else -> -1
            }
            MediaTrack(
                id = season * EPISODE_ID_STEP + number,
                number = number,
                seasonNumber = season,
                title = if (number == RESUME_EPISODE) "Мост" else "",
                thumbnail = "",
                durationSeconds = EPISODE_SECONDS,
                files = emptyList(),
                audios = emptyList(),
                subtitles = emptyList(),
                watchedSeconds = if (status == 0) WATCHED_SECONDS else 0,
                watchStatus = status,
            )
        }
    }

    private val series = calculateSeriesData(tracks)

    val movieLoaded = DetailsState(loading = false, item = movie)

    val seriesLoaded = DetailsState(
        loading = false,
        item = movie.copy(id = 2, title = "Хроники Пограничья", type = ItemType.SERIES, tracklist = tracks),
        series = series,
        selectedSeason = 1,
        isFav = true,
    )

    /** Домашний сервер: папок нет — и кнопки «В папку». */
    val withoutFolders = movieLoaded.copy(foldersAvailable = false)

    val all: List<DetailsState> = listOf(movieLoaded, seriesLoaded, withoutFolders)
}
