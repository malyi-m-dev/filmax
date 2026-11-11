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
    /** Числовой IMDb-id тайтла — по нему TMDB отдаёт фото актёров. null, если API его не прислал. */
    val imdbId: String? = null,
)

/**
 * Тип тайтла. [episodic] — у тайтла сезоны и серии, а не одно видео: по нему детали решают, нужен
 * ли выбор сезона. Решать по типу, а не по числу видео, приходится потому, что у фильма их тоже
 * бывает несколько. Как тип называется в API источника — забота data-слоя.
 */
enum class ItemType(val episodic: Boolean) {
    MOVIE(episodic = false),
    SERIES(episodic = true),

    /**
     * Раздел каталога, а не тип тайтла: аниме у источников — жанр поверх фильмов и сериалов. Из
     * API тайтл с таким типом не приходит.
     */
    ANIME(episodic = true),

    /** Документальные сериалы. Документальные фильмы — отдельный тип [DOCUMENTARY_MOVIE]. */
    DOCUMENTARY(episodic = true),
    DOCUMENTARY_MOVIE(episodic = false),

    /** ТВ-шоу выходят сезонами и выпусками — для деталей это сериал, а не фильм. */
    TV_SHOW(episodic = true),
    CONCERT(episodic = false),

    /** Эфирные каналы. */
    TV(episodic = false),
}

/**
 * Жанр каталога. [video] — жанр кино и сериалов: справочник kino.pub отдаёт одним списком жанры
 * всех типов, включая музыкальные, и без отбора в киношный каталог попали бы «Blues» и «Chillout».
 */
data class Genre(val id: Int, val title: String, val video: Boolean = true)

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
    /** Порядковый номер дорожки в потоке (1-based) — совпадает с порядком в HLS-манифесте. */
    val index: Int,
    val lang: String?,
    val title: String?,
    val channels: Int,
    /** Тип озвучки: «Многоголосый», «Дубляж», «Оригинал»… null — API не отдал. */
    val voiceType: String? = null,
    /** Студия озвучки: «BaibaKo», «Русский дубляж»… null — API не отдал. */
    val voiceAuthor: String? = null,
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
