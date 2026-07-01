package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import java.util.Locale

// Статусы просмотра kino.pub `watching.status` (см. MediaTrack.watchStatus).
private const val WATCH_STATUS_IN_PROGRESS = 0
private const val WATCH_STATUS_FINISHED = 1

/** Чипы качества в свёрнутой glass-панели TV — только столько помещается в ряд. */
private const val MAX_PANEL_QUALITY_CHIPS = 5

/** Бейджи (качества + языки) в развёрнутом оверлее панели. */
private const val MAX_DIALOG_QUALITY_BADGES = 8

/** Жанров в пилюлях хиро. */
private const val MAX_HERO_GENRES = 2

private const val MINUTES_IN_HOUR = 60
private const val HIGH_RATING_THRESHOLD = 8.5

/**
 * Маппинг domain → готовый к отрисовке [DetailsUi]. Вся подготовка данных деталей —
 * парсинг состава, группировка сезонов, выбор эпизода «продолжить», строки меты —
 * живёт здесь и покрывается юнит-тестами; вью только отрисовывают готовые поля.
 */
fun Item.toDetailsUi(similar: List<Item>): DetailsUi {
    // Продолжить: эпизод «в процессе», иначе последний досмотренный.
    val resumeTrack = tracklist.firstOrNull { it.watchStatus == WATCH_STATUS_IN_PROGRESS }
        ?: tracklist.lastOrNull { it.watchStatus == WATCH_STATUS_FINISHED }
    val seasons = buildSeasons(resumeTrack?.id)
    val videoQualities = tracklist.asSequence()
        .flatMap { it.files }
        .map { it.quality }
        .filter { it.isNotBlank() }
        .distinct()
        .take(MAX_PANEL_QUALITY_CHIPS)
        .toList()
    return DetailsUi(
        id = id,
        title = title,
        year = year,
        plot = plot,
        country = country,
        director = director,
        primaryGenre = genres.firstOrNull()?.title.orEmpty(),
        topGenres = genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title },
        durationMinutes = duration.averageMinutes?.toInt(),
        externalRating = rating.external,
        externalRatingText = rating.external?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
        isRatingHigh = (rating.external ?: 0.0) >= HIGH_RATING_THRESHOLD,
        metaLine = metaLine(includeDuration = true),
        metaLineNoDuration = metaLine(includeDuration = false),
        posterBig = posters.big,
        backdropUrl = posters.wide ?: posters.big,
        trailerUrl = trailer?.url,
        castLine = cast,
        castMembers = parseCastMembers(cast),
        stats = buildStats(),
        crew = buildCrew(),
        isSeriesLike = tracklist.size > 1 || type == ItemType.SERIES || type == ItemType.DOCUMENTARY,
        seasonsCaption = "Сериал · ${seasons.size} ${seasonsWord(seasons.size)}",
        seasons = seasons,
        resume = resumeTrack?.toResumeUi(),
        resumeSeasonIndex = resumeSeasonIndex(seasons, resumeTrack),
        videoQualities = videoQualities,
        qualityBadges = buildQualityBadges(videoQualities),
        similar = similar.map { sim ->
            SimilarUi(
                id = sim.id,
                title = sim.title,
                posterUrl = sim.posters.medium.ifEmpty { sim.posters.big },
            )
        },
    )
}

/** «2016 · 2ч 15м · США · Драма · Триллер» — присутствуют только известные части. */
private fun Item.metaLine(includeDuration: Boolean): String = buildString {
    append(year)
    if (includeDuration) {
        duration.averageMinutes?.toInt()?.takeIf { it > 0 }?.let { minutes ->
            append("  ·  ${minutes / MINUTES_IN_HOUR}ч ${minutes % MINUTES_IN_HOUR}м")
        }
    }
    if (country.isNotBlank()) append("  ·  $country")
    if (genres.isNotEmpty()) {
        append("  ·  ${genres.take(MAX_HERO_GENRES).joinToString(" · ") { it.title }}")
    }
}

/** Состав приходит одной строкой через запятую; инициалы — для аватаров-заглушек. */
private fun parseCastMembers(cast: String): List<CastMemberUi> = cast
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .map { name ->
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
        CastMemberUi(name = name, initials = initials)
    }

/** Четыре stat-карточки мобильной вкладки «О фильме»: рейтинг/длительность/режиссёр/жанр. */
private fun Item.buildStats(): List<DetailsStatUi> {
    val directorOrDash = director.ifBlank { "—" }
    return listOf(
        DetailsStatUi(
            label = "Рейтинг",
            value = rating.external?.let { String.format(Locale.US, "%.1f", it) } ?: "N/A",
            sub = "IMDb · КП",
        ),
        DetailsStatUi(
            label = "Длительность",
            value = duration.averageMinutes?.toInt()?.toString() ?: "?",
            sub = "мин",
        ),
        DetailsStatUi(
            label = "Режиссёр",
            value = directorOrDash.substringBefore(" "),
            sub = directorOrDash.substringAfter(" ", ""),
        ),
        DetailsStatUi(
            label = "Жанр",
            value = genres.getOrNull(0)?.title ?: "—",
            sub = genres.getOrNull(1)?.title ?: "—",
        ),
    )
}

private fun Item.buildCrew(): List<InfoRowUi> = listOf(
    InfoRowUi("Режиссёр", director.ifBlank { "—" }),
    InfoRowUi("Страна", country.ifBlank { "—" }),
    InfoRowUi("Жанр", genres.joinToString(", ") { it.title }.ifBlank { "—" }),
    InfoRowUi("Год", year.toString()),
)

/** Сезоны: группируем эпизоды по номеру сезона, серии — по порядку. */
private fun Item.buildSeasons(resumeId: Int?): List<SeasonUi> = tracklist
    .groupBy { it.seasonNumber }
    .toSortedMap()
    .map { (number, episodes) ->
        val sorted = episodes.sortedBy { it.number }
        SeasonUi(
            number = number,
            chipLabel = if (number > 0) "Сезон $number" else "Серии",
            countLabel = "${sorted.size} ${episodesWord(sorted.size)}",
            episodes = sorted.map { it.toEpisodeUi(resumeId) },
        )
    }

private fun MediaTrack.toEpisodeUi(resumeId: Int?): EpisodeUi = EpisodeUi(
    id = id,
    number = number,
    thumbnail = thumbnail,
    label = buildString {
        append("Серия $number")
        if (title.isNotBlank()) append(". $title")
    },
    metaLine = buildString {
        durationSeconds.takeIf { it > 0 }?.let { append("${it / SECONDS_IN_MINUTE} мин") }
        if (id == resumeId) append(if (isEmpty()) "продолжить" else " · продолжить")
    },
)

private fun MediaTrack.toResumeUi(): ResumeUi = ResumeUi(
    episodeId = id,
    episodeNumber = number,
    positionLabel = "Сезон $seasonNumber · Серия $number",
    progress = durationSeconds.takeIf { it > 0 }
        ?.let { (watchedSeconds.toFloat() / it).coerceIn(0f, 1f) },
)

private fun resumeSeasonIndex(seasons: List<SeasonUi>, resume: MediaTrack?): Int =
    resume?.let { track ->
        seasons.indexOfFirst { it.number == track.seasonNumber }.takeIf { it >= 0 }
    } ?: 0

/** Качества видео + языки аудио — бейджи развёрнутого оверлея glass-панели. */
private fun Item.buildQualityBadges(videoQualities: List<String>): List<String> {
    val audioLanguages = tracklist.asSequence()
        .flatMap { it.audios }
        .mapNotNull { it.lang }
        .map { audioLabel(it) }
        .distinct()
    return (videoQualities + audioLanguages).distinct().take(MAX_DIALOG_QUALITY_BADGES)
}

private fun audioLabel(code: String): String = when (code.lowercase()) {
    "rus", "ru" -> "Русский"
    "eng", "en" -> "Eng"
    "ukr", "uk" -> "Укр"
    else -> code
}

private const val SECONDS_IN_MINUTE = 60

// Модули русских правил склонения по числу (последние две / одна цифра).
private const val PLURAL_MOD_HUNDRED = 100
private const val PLURAL_MOD_TEN = 10

private fun seasonsWord(count: Int): String = when {
    count % PLURAL_MOD_HUNDRED in 11..14 -> "сезонов"
    count % PLURAL_MOD_TEN == 1 -> "сезон"
    count % PLURAL_MOD_TEN in 2..4 -> "сезона"
    else -> "сезонов"
}

private fun episodesWord(count: Int): String = when {
    count % PLURAL_MOD_HUNDRED in 11..14 -> "серий"
    count % PLURAL_MOD_TEN == 1 -> "серия"
    count % PLURAL_MOD_TEN in 2..4 -> "серии"
    else -> "серий"
}
