package com.filmax.data.jellyfin.mapper

import com.filmax.core.domain.catalog.model.AudioTrack
import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.catalog.model.Duration
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.ItemRating
import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.catalog.model.Posters
import com.filmax.core.domain.catalog.model.VideoFile
import com.filmax.data.jellyfin.JellyfinIds
import com.filmax.data.jellyfin.remote.dto.ItemDto
import com.filmax.data.jellyfin.remote.dto.MediaStreamDto
import com.filmax.data.jellyfin.remote.dto.PersonDto
import com.filmax.data.jellyfin.remote.dto.UserDataDto
import kotlin.math.roundToInt

/**
 * Ответы Jellyfin → доменные модели. GUID превращаются в числа через [JellyfinIds]; оценка — это
 * `CommunityRating` сервера (обычно из TMDB/IMDb по шкале 0–10), поэтому она идёт в поле IMDb:
 * Кинопоиска у домашнего сервера нет, и выдумывать его не стоит.
 */
internal class JellyfinMapper(private val ids: JellyfinIds, val images: JellyfinImages) {

    fun item(dto: ItemDto, tracklist: List<MediaTrack> = emptyList()): Item {
        val type = itemType(dto.type)
        return Item(
            id = ids.intFor(dto.id.orEmpty()),
            title = dto.name.orEmpty(),
            type = type,
            year = dto.productionYear ?: 0,
            plot = dto.overview.orEmpty(),
            director = dto.people.names(DIRECTOR).joinToString(", "),
            cast = dto.people.names(ACTOR).take(CAST_LIMIT).joinToString(", "),
            country = dto.productionLocations?.firstOrNull().orEmpty(),
            genres = dto.genreItems.orEmpty().mapNotNull { genre -> genre(genre.id, genre.name, type) },
            rating = ItemRating(
                filmax = 0,
                filmaxPercentage = "",
                imdb = dto.communityRating?.let(::ratingText),
                kinopoisk = null,
            ),
            posters = posters(dto),
            duration = Duration(
                averageMinutes = dto.runTimeTicks?.let { ticksToSeconds(it) / SECONDS_PER_MINUTE },
                totalMinutes = null,
            ),
            tracklist = tracklist,
            // Трейлеры сервера — ссылки на YouTube, а плеер трейлеров играет прямой поток.
            trailer = null,
            inWatchlist = dto.userData?.isFavorite == true,
            finished = dto.status == STATUS_ENDED,
            imdbId = dto.providerIds?.get(IMDB_PROVIDER),
        )
    }

    /** Жанр с типом каталога: по нему поиск отделяет киношные жанры от музыкальных. */
    fun genre(guid: String?, name: String?, type: ItemType = ItemType.MOVIE): Genre? {
        if (guid == null || name.isNullOrBlank()) return null
        return Genre(id = ids.intFor(guid), title = name, type = if (type.episodic) SERIAL_GENRE else MOVIE_GENRE)
    }

    fun collection(dto: ItemDto): Collection = Collection(
        id = ids.intFor(dto.id.orEmpty()),
        title = dto.name.orEmpty(),
        description = dto.overview,
        posters = posters(dto),
    )

    /** Фильм — одно видео: номер 1, без сезона (так его адресует и маршрут плеера). */
    fun movieTrack(dto: ItemDto): MediaTrack = MediaTrack(
        id = JellyfinIds.preferredId(dto.id.orEmpty()),
        number = 1,
        seasonNumber = 0,
        title = dto.name.orEmpty(),
        thumbnail = posters(dto).wide ?: posters(dto).medium,
        durationSeconds = dto.runTimeTicks?.let(::ticksToSeconds)?.toInt() ?: 0,
        // Ссылок здесь нет — их выдаёт сервер под запуск (JellyfinStreamRepository); качество —
        // только подпись для меты деталей.
        files = qualityLabel(dto.width, dto.height)?.let { listOf(VideoFile(it, hls4 = null, hls = null, http = null)) }
            .orEmpty(),
        audios = audios(dto.mediaStreams),
        subtitles = emptyList(),
        watchedSeconds = watchedSeconds(dto.userData),
        watchStatus = watchStatus(dto.userData),
    )

    /**
     * Серия; null — без номера или из «Спецвыпусков» (сезон 0). Маршрут плеера адресует серию
     * парой «сезон + номер», и сезон 0 для него означает фильм — спецвыпуски перепутались бы.
     */
    fun episode(dto: ItemDto): MediaTrack? {
        val number = dto.indexNumber
        val season = dto.parentIndexNumber?.takeIf { it > 0 }
        if (number == null || season == null) return null
        return MediaTrack(
            id = JellyfinIds.preferredId(dto.id.orEmpty()),
            number = number,
            seasonNumber = season,
            title = dto.name.orEmpty(),
            thumbnail = images.primary(dto.id, dto.imageTags?.get(PRIMARY), JellyfinImages.EPISODE_STILL),
            durationSeconds = dto.runTimeTicks?.let(::ticksToSeconds)?.toInt() ?: 0,
            files = emptyList(),
            audios = emptyList(),
            subtitles = emptyList(),
            watchedSeconds = watchedSeconds(dto.userData),
            watchStatus = watchStatus(dto.userData),
        )
    }

    fun posters(dto: ItemDto): Posters {
        val primaryTag = dto.imageTags?.get(PRIMARY)
        return Posters(
            small = images.primary(dto.id, primaryTag, JellyfinImages.POSTER_SMALL),
            medium = images.primary(dto.id, primaryTag, JellyfinImages.POSTER_MEDIUM),
            big = images.primary(dto.id, primaryTag, JellyfinImages.POSTER_BIG),
            wide = backdrop(dto),
        )
    }

    /** Широкий фон: свой у тайтла, иначе родительский (у серии — фон сериала). */
    fun backdrop(dto: ItemDto): String? =
        images.backdrop(dto.id, dto.backdropImageTags?.firstOrNull())
            ?: images.backdrop(dto.parentBackdropItemId, dto.parentBackdropImageTags?.firstOrNull())

    private fun List<PersonDto>?.names(type: String): List<String> =
        orEmpty().filter { it.type == type }.mapNotNull { it.name?.takeIf(String::isNotBlank) }

    companion object {
        const val PRIMARY = "Primary"
        const val TYPE_EPISODE = "Episode"
        private const val DIRECTOR = "Director"
        private const val ACTOR = "Actor"
        private const val STATUS_ENDED = "Ended"
        private const val IMDB_PROVIDER = "Imdb"
        private const val MOVIE_GENRE = "movie"
        private const val SERIAL_GENRE = "serial"

        /** Строка актёров в деталях: дальше десятого имени её никто не дочитывает. */
        private const val CAST_LIMIT = 10
        private const val SECONDS_PER_MINUTE = 60.0
        private const val RATING_PRECISION = 10.0

        fun itemType(type: String?): ItemType = when (type) {
            "Series", "Season", TYPE_EPISODE -> ItemType.SERIES
            else -> ItemType.MOVIE
        }

        /** Тики Jellyfin — по 100 нс. */
        fun ticksToSeconds(ticks: Long): Long = ticks / TICKS_PER_SECOND

        fun secondsToTicks(seconds: Int): Long = seconds * TICKS_PER_SECOND

        const val TICKS_PER_SECOND = 10_000_000L

        fun watchedSeconds(userData: UserDataDto?): Int =
            userData?.playbackPositionTicks?.let(::ticksToSeconds)?.toInt()?.coerceAtLeast(0) ?: 0

        /** Статус в шкале kino.pub: 1 — досмотрен, 0 — начат, −1 — не начат. */
        fun watchStatus(userData: UserDataDto?): Int = when {
            userData?.played == true -> 1
            watchedSeconds(userData) > 0 -> 0
            else -> -1
        }

        fun ratingText(rating: Double): String =
            ((rating * RATING_PRECISION).roundToInt() / RATING_PRECISION).toString()

        /**
         * Подпись качества по кадру: «1080p», «2160p». По ширине надёжнее, чем по высоте: у
         * широкоэкранного фильма 1920×800 высота меньше 1080, но это Full HD.
         */
        fun qualityLabel(width: Int?, height: Int?): String? {
            val byWidth = width?.let { value -> QUALITY_BY_WIDTH.firstOrNull { value >= it.first }?.second }
            val byHeight = height?.let { value -> QUALITY_BY_HEIGHT.firstOrNull { value >= it.first }?.second }
            return listOfNotNull(byWidth, byHeight).maxOrNull()?.let { "${it}p" }
        }

        /** Порог ширины (и высоты) кадра → строки качества; пороги с запасом под обрезанные кадры. */
        private val QUALITY_BY_WIDTH = listOf(
            3800 to 2160,
            2500 to 1440,
            1900 to 1080,
            1260 to 720,
            840 to 480,
            0 to 360,
        )
        private val QUALITY_BY_HEIGHT = listOf(
            2000 to 2160,
            1400 to 1440,
            1000 to 1080,
            700 to 720,
            460 to 480,
            0 to 360,
        )

        /**
         * Аудиодорожки по порядку в файле: этот же порядок у групп дорожек в плеере, и по номеру
         * ([AudioTrack.index], с 1) плеер подписывает каждую группу языком и названием дорожки.
         */
        fun audios(streams: List<MediaStreamDto>?): List<AudioTrack> =
            streams.orEmpty().filter { it.type == STREAM_AUDIO }.mapIndexed { position, stream ->
                AudioTrack(
                    id = stream.index ?: position,
                    index = position + 1,
                    lang = stream.language,
                    title = stream.displayTitle,
                    channels = stream.channels ?: STEREO,
                    voiceType = null,
                    // В названии дорожки файла обычно и лежит озвучка: «Дубляж», «MVO LostFilm».
                    voiceAuthor = stream.title?.takeIf(String::isNotBlank),
                )
            }

        const val STREAM_AUDIO = "Audio"
        const val STREAM_VIDEO = "Video"
        const val STREAM_SUBTITLE = "Subtitle"
        private const val STEREO = 2
    }
}
