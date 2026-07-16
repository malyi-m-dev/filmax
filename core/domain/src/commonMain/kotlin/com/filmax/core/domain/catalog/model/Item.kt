package com.filmax.core.domain.catalog.model

data class Item(
    val id: Int,
    val title: String,
    val type: ItemType,
    val year: Int,
    val plot: String,
    val director: String,
    val cast: String,
    val country: String,
    val genres: List<Genre>,
    val rating: ItemRating,
    val posters: Posters,
    val duration: Duration,
    val tracklist: List<MediaTrack>,
    val trailer: Trailer?,
    val inWatchlist: Boolean,
    val finished: Boolean,
)

enum class ItemType(val apiValue: String) {
    MOVIE("movie"),
    SERIES("serial"),
    ANIME("anime"),
    DOCUMENTARY("docuserial"),
    TV("tv"),
    ;
    companion object {
        fun from(value: String) = entries.firstOrNull { it.apiValue == value } ?: MOVIE
    }
}

/**
 * Жанр каталога. [type] — тип контента, к которому жанр относится (`movie`, `serial`, `music`…):
 * `api/v1/genres` отдаёт одним списком жанры ВСЕХ типов, включая музыкальные, поэтому без
 * фильтра по типу в киношный каталог попадают «Blues» и «Chillout». Внутри тайтла поля нет —
 * там жанр приходит без типа, отсюда null по умолчанию.
 */
data class Genre(val id: Int, val title: String, val type: String? = null)

data class ItemRating(
    val filmax: Int,
    val filmaxPercentage: String,
    val imdb: String?,
    val kinopoisk: String?,
) {
    /**
     * Средняя внешняя оценка по шкале 0–10: берём доступные значения IMDb и Кинопоиска
     * и усредняем их. Если ни одной оценки нет — `null` (в UI показываем «N/A»).
     */
    val external: Double?
        get() {
            val scores = listOfNotNull(imdb?.toDoubleOrNull(), kinopoisk?.toDoubleOrNull())
            return scores.takeIf { it.isNotEmpty() }?.average()
        }
}

data class Posters(
    val small: String,
    val medium: String,
    val big: String,
    val wide: String?,
)

data class Duration(
    val averageMinutes: Double?,
    val totalMinutes: Int?,
)

data class MediaTrack(
    val id: Int,
    val number: Int,
    val seasonNumber: Int,
    val title: String,
    val thumbnail: String,
    val durationSeconds: Int,
    val files: List<VideoFile>,
    val audios: List<AudioTrack>,
    val subtitles: List<SubtitleTrack>,
    /** Прогресс просмотра в секундах (kino.pub `watching.time`); 0 — не начат. */
    val watchedSeconds: Int = 0,
    /** Статус просмотра: -1 не начат, 0 в процессе, 1 досмотрен (kino.pub `watching.status`). */
    val watchStatus: Int = -1,
)

data class VideoFile(
    val quality: String,
    val hls4: String?,
    val hls: String?,
    val http: String?,
)

data class AudioTrack(
    val id: Int,
    val lang: String?,
    val title: String?,
    val channels: Int,
)

data class SubtitleTrack(
    val lang: String,
    val url: String,
    val shiftMs: Int,
)

data class Trailer(
    val id: String,
    val url: String,
)
