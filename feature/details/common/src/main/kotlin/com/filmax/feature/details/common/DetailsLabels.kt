// Подписи и цели экрана деталей — чистые функции над тайтлом, общие для телефона и ТВ. Раньше
// у каждой платформы был свой набор (DetailsLabels в mobile, TvDetailsLabels в tv), и правило
// «что играет кнопка „Смотреть“» было записано дважды.

package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember
import com.filmax.core.presentation.labels.durationLabel
import com.filmax.core.presentation.labels.ratingLabel
import com.filmax.core.presentation.labels.typeLabel

/** Фильм играется целиком, без выбора дорожки: плеер ждёт videoId = -1. */
const val MOVIE_VIDEO_ID = -1

/** «Сезона нет» — фильм или сезон неизвестен (PlayerRoute.season = -1). */
const val NO_SEASON = -1

private const val SECONDS_IN_MINUTE = 60

/** Сколько жанров влезает в мету ТВ без переноса. */
private const val MAX_META_GENRES = 2

/** Сколько актёров в строке «В ролях» у сериала — дальше строка уходит за край колонки. */
private const val CAST_LINE_NAMES = 3

/**
 * Главная кнопка: фильм — целиком; сериал — серия «продолжить», иначе первая серия выбранного
 * сезона, иначе первая вообще. Сериал без серий играть нечем — null, кнопки нет.
 */
fun playButton(item: Item, series: SeriesData?, seasonEpisodes: List<MediaTrack>): PlayButton? =
    if (series == null) moviePlayButton(item) else seriesPlayButton(item, series, seasonEpisodes)

private fun moviePlayButton(item: Item): PlayButton {
    val subtitle = listOfNotNull(volumeLabel(item, null), "4K".takeIf { hasUhd(item) }).joinToString(" · ")
    return PlayButton(
        season = NO_SEASON,
        videoId = MOVIE_VIDEO_ID,
        title = "Смотреть",
        subtitle = subtitle.ifBlank { null },
        compact = "Смотреть",
        progress = null,
    )
}

private fun seriesPlayButton(item: Item, series: SeriesData, seasonEpisodes: List<MediaTrack>): PlayButton? {
    val episode = series.resume ?: seasonEpisodes.firstOrNull() ?: item.tracklist.firstOrNull() ?: return null
    val resume = episode == series.resume
    val code = if (episode.seasonNumber > 0) "S${episode.seasonNumber}E${episode.number}" else "Серия ${episode.number}"
    val title = episode.title.takeIf { it.isNotBlank() }?.let { "«$it»" }
    val left = remainingMinutes(episode)?.let { "осталось ${durationLabel(it)}" }.takeIf { resume }
    return PlayButton(
        season = episode.seasonNumber,
        videoId = episode.number,
        title = if (resume) "Продолжить" else "Смотреть",
        subtitle = listOfNotNull(listOfNotNull(code, title).joinToString(" "), left).joinToString(" · "),
        compact = if (resume) "Продолжить · $code" else "Смотреть",
        progress = episodeProgress(episode).takeIf { resume && it > 0f },
    )
}

private fun remainingMinutes(episode: MediaTrack): Int? {
    if (episode.watchedSeconds <= 0) return null
    return ((episode.durationSeconds - episode.watchedSeconds) / SECONDS_IN_MINUTE).takeIf { it > 0 }
}

/** Подписи шапки для обеих платформ — см. поля [DetailsHeading]. */
fun detailsHeading(item: Item, series: SeriesData?): DetailsHeading {
    val year = item.year.takeIf { it > 0 }?.toString()
    val firstCountry = item.country.substringBefore(',').trim().takeIf { it.isNotBlank() }
    val countries = item.country.trim().takeIf { it.isNotBlank() }
    val genres = item.genres.take(MAX_META_GENRES).joinToString(", ") { it.title }.takeIf { it.isNotBlank() }
    val volume = volumeLabel(item, series)
    val type = typeLabel(item.type)
    val yearOrVolume = if (series == null) year else volume
    val metaParts = listOfNotNull(year.takeIf { series != null }, genres, volume.takeIf { series == null })
    return DetailsHeading(
        overline = listOfNotNull(type, yearOrVolume, firstCountry).joinToString(" · "),
        typeAndGenre = listOfNotNull(type, item.genres.firstOrNull()?.title).joinToString(" · "),
        originLine = listOfNotNull(year, countries).joinToString(" · ").ifBlank { null },
        metaLine = metaParts.joinToString(" · "),
        sheetMeta = listOfNotNull(year, countries, fullVolume(item, series)).joinToString(" · ").ifBlank { null },
        volume = volume,
        hasUhd = hasUhd(item),
        ratingBadges = listOfNotNull(
            ratingLabel(item.rating.kinopoisk)?.let { RatingBadge(it, "КиноПоиск", highlighted = true) },
            ratingLabel(item.rating.imdb)?.let { RatingBadge(it, "IMDb", highlighted = false) },
        ),
    )
}

/** «7 сезонов, 68 серий» для панели описания; в один сезон — только число серий. */
private fun fullVolume(item: Item, series: SeriesData?): String? {
    if (series == null) return volumeLabel(item, null)
    val episodes = series.seasons.sumOf { (_, seasonEpisodes) -> seasonEpisodes.size }
    return listOfNotNull(
        volumeLabel(item, series).takeIf { series.seasons.size > 1 },
        episodes.takeIf { it > 0 }?.let { "$it ${episodesWord(it)}" },
    ).joinToString(", ").ifBlank { null }
}

/** «В ролях: Анна Лунд, Эрик Сунд, Мия Хольм»; null — состав неизвестен. */
fun castLine(people: List<CastMember>): String? {
    val names = people.take(CAST_LINE_NAMES).joinToString(", ") { it.name }
    return if (names.isBlank()) null else "В ролях: $names"
}

/** Панель серий для сезона [selected]: чипы, строки, откуда стартовать и куда листать. */
fun episodesPanel(series: SeriesData, selected: Int?): EpisodesPanel {
    val episodes = series.seasons.firstOrNull { it.first == selected }?.second.orEmpty()
    val resumeId = series.resume?.id
    val index = series.seasons.indexOfFirst { it.first == selected }
    return EpisodesPanel(
        seasons = series.seasons.map { (number, _) ->
            SeasonTab(
                number = number,
                label = seasonLabel(number),
                compactLabel = if (number > 0) number.toString() else seasonLabel(number),
                selected = number == selected,
            )
        },
        rows = episodes.map { episodeRow(it, resumeId) },
        summary = seasonSummary(episodes),
        startIndex = startEpisodeIndex(episodes, resumeId),
        startFocusEpisodeId = resumeId?.takeIf { id -> episodes.any { it.id == id } },
        previous = if (index < 0) null else seasonStart(series, index - 1),
        next = if (index < 0) null else seasonStart(series, index + 1),
    )
}

/** Сезон по индексу в отсортированном списке и серия, с которой он откроется; null — такого нет. */
private fun seasonStart(series: SeriesData, index: Int): SeasonStart? {
    val (season, episodes) = series.seasons.getOrNull(index) ?: return null
    val episode = episodes.getOrNull(startEpisodeIndex(episodes, series.resume?.id))
    return episode?.let { SeasonStart(season, it.id) }
}

/** Строка серии: «7. Тайный совет», мета о том, где в серии зритель, прогресс. */
fun episodeRow(episode: MediaTrack, resumeId: Int?): EpisodeRow {
    val watched = episode.watchStatus == WATCH_STATUS_FINISHED
    val resume = episode.id == resumeId && !watched
    return EpisodeRow(
        episode = episode,
        title = numberedEpisodeTitle(episode),
        meta = episodeRowMeta(episode, resume),
        progress = episodeProgress(episode).takeIf { episode.watchStatus == WATCH_STATUS_IN_PROGRESS || watched },
        watched = watched,
        resume = resume,
    )
}

/** «10 серий · ~55 мин» — число серий сезона и средняя длительность; null — сезон пуст. */
fun seasonSummary(episodes: List<MediaTrack>): String? {
    if (episodes.isEmpty()) return null
    val durations = episodes.map { it.durationSeconds }.filter { it > 0 }
    val minutes = durations.takeIf { it.isNotEmpty() }?.let { it.average().toInt() / SECONDS_IN_MINUTE }
    return listOfNotNull("${episodes.size} ${episodesWord(episodes.size)}", minutes?.let { "~$it мин" })
        .joinToString(" · ")
}
