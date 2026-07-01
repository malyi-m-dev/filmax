package com.filmax.feature.details.common

/**
 * Контракт Details. Стейт полностью готовится в [DetailsScreenModel] (через маппер
 * `Item.toDetailsUi`): вью не парсит и не агрегирует доменные модели, а только
 * отрисовывает готовые поля [DetailsUi].
 */
data class DetailsUiState(
    val loading: Boolean = true,
    /** Готовые к отрисовке детали; null — ещё не загружены (или загрузка упала). */
    val details: DetailsUi? = null,
    val isFav: Boolean = false,
    val isDownloaded: Boolean = false,
    /** Активная вкладка мобильного экрана. */
    val selectedTab: DetailsTab = DetailsTab.ABOUT,
    /** Индекс выбранного сезона в [DetailsUi.seasons] (TV-браузер серий). */
    val selectedSeasonIndex: Int = 0,
) {
    /** Выбранный сезон (для чипов и подписи «N серий» на TV). */
    val currentSeason: SeasonUi?
        get() = details?.seasons?.getOrNull(selectedSeasonIndex)

    /** Эпизоды выбранного сезона. */
    val currentEpisodes: List<EpisodeUi>
        get() = currentSeason?.episodes.orEmpty()

    /**
     * Эпизод для кнопки «Продолжить/Смотреть» сериала: незавершённый, иначе первый
     * в выбранном сезоне, иначе самый первый в списке.
     */
    val playEpisodeId: Int?
        get() = details?.resume?.episodeId
            ?: currentEpisodes.firstOrNull()?.id
            ?: details?.seasons?.firstOrNull()?.episodes?.firstOrNull()?.id
}

/** Вкладки мобильного экрана деталей. */
enum class DetailsTab { ABOUT, CAST }

/**
 * Полностью подготовленные к отрисовке детали тайтла. Строки собираются в маппере
 * (строковых ресурсов в проекте пока нет); при переходе на ресурсы текстовые шаблоны
 * переедут в UI-слой, а здесь останутся значения.
 */
data class DetailsUi(
    val id: Int,
    val title: String,
    val year: Int,
    val plot: String,
    val country: String,
    val director: String,
    /** Первый жанр («Драма») — пилюля мобильного хиро; "" если жанров нет. */
    val primaryGenre: String,
    /** До двух жанров через « · » — пилюля TV-хиро. */
    val topGenres: String,
    /** Средняя длительность в минутах; null — неизвестна. */
    val durationMinutes: Int?,
    /** Средняя внешняя оценка (IMDb + КП) по шкале 0–10; null — оценок нет. */
    val externalRating: Double?,
    /** Та же оценка строкой («7.9»), «N/A» если оценок нет. */
    val externalRatingText: String,
    /** Высокая оценка (иная подсветка рейтинга на TV). */
    val isRatingHigh: Boolean,
    /** «2016 · 2ч 15м · США · Драма · Триллер» — строка меты TV-хиро фильма. */
    val metaLine: String,
    /** То же без длительности — для сериала. */
    val metaLineNoDuration: String,
    val posterBig: String,
    /** Широкий кадр для TV-бэкдропа (с откатом на big). */
    val backdropUrl: String,
    val trailerUrl: String?,
    /** Исходная строка состава — правая glass-панель TV. */
    val castLine: String,
    /** Распарсенный состав с инициалами — вкладка «Актёры» на мобильном. */
    val castMembers: List<CastMemberUi>,
    /** Четыре stat-карточки мобильной вкладки «О фильме» (рейтинг/длительность/режиссёр/жанр). */
    val stats: List<DetailsStatUi>,
    /** Строки блока «Команда» мобильной вкладки «Актёры». */
    val crew: List<InfoRowUi>,
    /** Сериалоподобный контент: браузер сезонов вместо экрана фильма. */
    val isSeriesLike: Boolean,
    /** «Сериал · 3 сезона» — пилюля TV-хиро сериала. */
    val seasonsCaption: String,
    /** Сезоны с отсортированными сериями; пуст для фильма. */
    val seasons: List<SeasonUi>,
    /** Эпизод «продолжить просмотр»; null — просмотр не начат. */
    val resume: ResumeUi?,
    /** Индекс сезона эпизода [resume] (начальный выбор в браузере серий). */
    val resumeSeasonIndex: Int,
    /** Качества видео для свёрнутой glass-панели. */
    val videoQualities: List<String>,
    /** Качества + языки аудио для развёрнутого оверлея панели. */
    val qualityBadges: List<String>,
    /** Похожие тайтлы (рельса на TV). */
    val similar: List<SimilarUi>,
)

data class CastMemberUi(val name: String, val initials: String)

data class DetailsStatUi(val label: String, val value: String, val sub: String)

data class InfoRowUi(val label: String, val value: String)

data class SeasonUi(
    val number: Int,
    /** «Сезон 2», либо «Серии» для контента без сезонов. */
    val chipLabel: String,
    /** «8 серий» — подпись над списком. */
    val countLabel: String,
    val episodes: List<EpisodeUi>,
)

data class EpisodeUi(
    val id: Int,
    val number: Int,
    val thumbnail: String,
    /** «Серия 5. Название» (название опускается, если пустое). */
    val label: String,
    /** «42 мин · продолжить» — длительность и маркер продолжения, что из этого есть. */
    val metaLine: String,
)

data class ResumeUi(
    val episodeId: Int,
    val episodeNumber: Int,
    /** «Сезон 2 · Серия 5». */
    val positionLabel: String,
    /** Доля просмотра 0..1; null — длительность неизвестна (полоску не показываем). */
    val progress: Float?,
)

data class SimilarUi(val id: Int, val title: String, val posterUrl: String)

sealed interface DetailsEvent {
    data object ToggleFav : DetailsEvent
    data object ToggleDownload : DetailsEvent
    data class SelectTab(val tab: DetailsTab) : DetailsEvent
    data class SelectSeason(val index: Int) : DetailsEvent
}

sealed interface DetailsSideEffect
