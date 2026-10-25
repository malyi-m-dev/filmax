package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.VideoFile
import com.filmax.core.domain.person.CastMember
import com.filmax.data.fake.store.CatalogEntry
import com.filmax.data.fake.store.MOVIE_SEASON
import kotlin.math.roundToInt

private const val SECONDS_IN_MINUTE = 60

/** Номер единственного видео фильма — как в `videos` у kino.pub. */
private const val MOVIE_VIDEO_NUMBER = 1

/** Рейтинг Filmax — средняя КП/IMDb ×10: на сервере это голоса, а сортировке нужен лишь порядок. */
private const val RATING_SCALE = 10

/** Сколько первых актёров получают роль; у остальных роль неизвестна, как часто бывает у TMDB. */
private const val NAMED_ROLES = 4

/** Основа выдуманных IMDb-id: семь цифр, как у настоящих. */
private const val IMDB_ID_BASE = 7_100_000

// Id трека: тайтл, сезон и номер серии в одном числе — уникален в пределах каталога.
private const val TRACK_ID_ITEM_FACTOR = 10_000
private const val TRACK_ID_SEASON_FACTOR = 100

/** Собирает «серверный» тайтл из описания: серии, озвучки, файлы, постеры, рейтинг, актёры. */
internal fun TitleSpec.toEntry(): CatalogEntry = CatalogEntry(
    item = toItem(tracks = tracks(voicesFor(this))),
    views = views,
    countryId = country.id,
    has4k = uhd,
    cast = castMembers(),
)

private fun TitleSpec.toItem(tracks: List<MediaTrack>): Item = Item(
    id = id,
    title = title,
    type = type,
    year = year,
    plot = plot,
    director = director.name,
    cast = cast.joinToString(", ") { it.name },
    country = country.title,
    // Внутри тайтла жанр приходит без типа: тип есть только в списке api/v1/genres.
    genres = genres.map { it.copy(type = null) },
    rating = rating(),
    posters = titlePosters(id),
    duration = durationOf(tracks),
    tracklist = tracks,
    trailer = if (hasTrailer) trailerFor(id) else null,
    inWatchlist = false,
    finished = finished,
    imdbId = if (hasCastPhotos) "${IMDB_ID_BASE + id}" else null,
)

/**
 * Файлы и озвучки — одни на все видео тайтла. Субтитров нет ни у кого: публичного .vtt под эти
 * потоки нет, а битая ссылка на субтитры роняла бы воспроизведение целиком.
 */
private data class TrackAssets(val files: List<VideoFile>, val voices: List<AudioTrack>)

private fun TitleSpec.tracks(voices: List<AudioTrack>): List<MediaTrack> {
    val assets = TrackAssets(files = videoFiles(streamFor(voices), uhd), voices = voices)
    return when (val spec = media) {
        is MovieMedia -> listOf(movieTrack(spec, assets))
        is SeriesMedia -> spec.seasons.flatMap { season ->
            season.episodes.map { episode -> episodeTrack(season.number, episode, assets) }
        }
    }
}

/** Фильм приходит в `videos`: одно видео, номер 1, без сезона — как в маппере data:catalog. */
private fun TitleSpec.movieTrack(spec: MovieMedia, assets: TrackAssets): MediaTrack = MediaTrack(
    id = trackId(MOVIE_SEASON, MOVIE_VIDEO_NUMBER),
    number = MOVIE_VIDEO_NUMBER,
    seasonNumber = MOVIE_SEASON,
    title = title,
    thumbnail = stillUrl(id, MOVIE_SEASON, MOVIE_VIDEO_NUMBER),
    durationSeconds = spec.runtimeMinutes * SECONDS_IN_MINUTE,
    files = assets.files,
    audios = assets.voices,
    subtitles = emptyList(),
)

/** Серия: `number` — номер ВНУТРИ сезона, сезон — отдельным полем (videoId уникален только в сезоне). */
private fun TitleSpec.episodeTrack(season: Int, episode: EpisodeSpec, assets: TrackAssets): MediaTrack =
    MediaTrack(
        id = trackId(season, episode.number),
        number = episode.number,
        seasonNumber = season,
        title = episode.title,
        thumbnail = if (episode.hasStill) stillUrl(id, season, episode.number) else "",
        durationSeconds = episode.durationSeconds,
        files = assets.files,
        audios = assets.voices,
        subtitles = emptyList(),
    )

private fun TitleSpec.trackId(season: Int, number: Int): Int =
    id * TRACK_ID_ITEM_FACTOR + season * TRACK_ID_SEASON_FACTOR + number

/** Длительность в минутах, как её считает маппер: средняя по видео и сумма всех видео. */
private fun durationOf(tracks: List<MediaTrack>): Duration {
    val seconds = tracks.map { it.durationSeconds }
    return Duration(
        averageMinutes = seconds.average() / SECONDS_IN_MINUTE,
        totalMinutes = seconds.sum() / SECONDS_IN_MINUTE,
    )
}

/** Оценки строками, как после маппера (`7.0`, а не `7`); нет оценок — рейтинг Filmax 0. */
private fun TitleSpec.rating(): ItemRating {
    val scores = listOfNotNull(kinopoisk, imdb)
    val filmax = if (scores.isEmpty()) 0 else (scores.average() * RATING_SCALE).roundToInt()
    return ItemRating(
        filmax = filmax,
        filmaxPercentage = filmax.toString(),
        imdb = imdb?.toString(),
        kinopoisk = kinopoisk?.toString(),
    )
}

private fun TitleSpec.castMembers(): List<CastMember> = cast.mapIndexed { index, person ->
    CastMember(name = person.name, character = roleOf(index, person), photoUrl = personPhotoUrl(person.id))
}

private fun TitleSpec.roleOf(index: Int, person: Person): String? =
    FORMAT_ROLES[type] ?: ROLES[(person.id + id) % ROLES.size].takeIf { index < NAMED_ROLES }
