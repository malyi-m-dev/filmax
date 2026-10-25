package com.filmax.feature.details.common

import com.filmax.core.domain.catalog.model.Item
import com.filmax.core.domain.catalog.model.MediaTrack
import com.filmax.core.domain.person.CastMember

/**
 * Состояние деталей: данные тайтла и всё, что экраны рисуют готовым — подписи шапки, цель
 * главной кнопки, строки серий. Производные считаются в одном месте ([loaded], [withSeason],
 * [withCast]) при смене данных, а не на каждом чтении; телефон и ТВ берут их из одних полей.
 */
data class DetailsState(
    val loading: Boolean = true,
    val item: Item? = null,
    val similar: List<Item> = emptyList(),
    /**
     * Актёры с фото (TMDB) — украшение поверх строки имён от kino.pub. Пустой список, когда фото
     * недоступны (нет ключа TMDB, нет совпадения по IMDb, сбой): тогда [people] строятся из `item.cast`.
     */
    val cast: List<CastMember> = emptyList(),
    val isFav: Boolean = false,
    val isDownloaded: Boolean = false,
    val error: String? = null,
    /** Сезоны и точка «продолжить» — только у сериала, у фильма null. */
    val series: SeriesData? = null,
    /** Номер сезона, чьи серии на экране. Стартует с сезона серии «продолжить». */
    val selectedSeason: Int? = null,
    /** Открыта боковая панель с полным описанием. */
    val descriptionOpen: Boolean = false,
    val heading: DetailsHeading = DetailsHeading(),
    /** Главная кнопка «Смотреть»; null — играть нечем (сериал без серий), кнопки нет. */
    val play: PlayButton? = null,
    /** Прямой url трейлера, если он играбельный (http(s)); null — кнопки «Трейлер» нет. */
    val trailerUrl: String? = null,
    /** Люди для секции «Актёры»: с фото TMDB или из строки имён kino.pub. */
    val people: List<CastMember> = emptyList(),
    /** Режиссёры по одному — у kino.pub они строкой через запятую. */
    val directors: List<String> = emptyList(),
    /** «В ролях: Анна Лунд, Эрик Сунд, Мия Хольм» — строка состава для колонки сериала на ТВ. */
    val castLine: String? = null,
    val facts: List<DetailsFact> = emptyList(),
    /** «О фильме» / «О сериале» — заголовок фактов и панели описания. */
    val factsTitle: String = "О фильме",
    /** Панель серий — только у сериала. */
    val episodes: EpisodesPanel? = null,
) {
    /** Тайтл загружен: шапка, кнопка, состав, факты и сезон серии «продолжить» — за один проход. */
    fun loaded(item: Item, similar: List<Item>, series: SeriesData?): DetailsState {
        val season = series?.seasons?.getOrNull(series.resumeSeasonIndex)?.first
        return copy(
            loading = false,
            item = item,
            similar = similar,
            series = series,
            heading = detailsHeading(item, series),
            trailerUrl = item.trailer?.url?.takeIf { it.startsWith("http") },
            directors = item.director.split(",").map { it.trim() }.filter { it.isNotBlank() },
            facts = detailsFacts(item, series),
            factsTitle = if (series == null) "О фильме" else "О сериале",
        ).withCast(cast).withSeason(season)
    }

    /** Сезон [number] на экране: серии, их строки, кнопка и соседние сезоны пересчитаны. */
    fun withSeason(number: Int?): DetailsState {
        val shownItem = item ?: return copy(selectedSeason = number)
        val panel = series?.let { episodesPanel(it, number) }
        return copy(
            selectedSeason = number,
            episodes = panel,
            play = playButton(shownItem, series, panel?.rows?.map { it.episode }.orEmpty()),
        )
    }

    /** Фото актёров доехали (или нет) — люди и строка состава пересчитаны. */
    fun withCast(cast: List<CastMember>): DetailsState {
        val people = resolveCast(cast, item?.cast.orEmpty())
        return copy(cast = cast, people = people, castLine = castLine(people))
    }
}

/** Подписи шапки: формулировки телефона и ТВ разные, поэтому у каждой своё поле. */
data class DetailsHeading(
    /** ТВ: «Фильм · 2022 · Норвегия», «Сериал · 7 сезонов · США». */
    val overline: String = "",
    /** Телефон: «Фильм · Драма», «Сериал · Фантастика». */
    val typeAndGenre: String = "",
    /** Телефон: «2021 · США» под названием — оригинального названия kino.pub не отдаёт. */
    val originLine: String? = null,
    /** ТВ, рядом с оценками: фильм — жанры и длительность, сериал — год и жанры. */
    val metaLine: String = "",
    /** ТВ, панель полного описания: «2022 · Норвегия, Дания · 2 ч 11 мин». */
    val sheetMeta: String? = null,
    /** «2 ч 11 мин» у фильма, «3 сезона» или «12 серий» у сериала. */
    val volume: String? = null,
    val hasUhd: Boolean = false,
    /** Оценки с источником: Кинопоиск — главный, IMDb — вторым. Нуля нет — бейджа нет. */
    val ratingBadges: List<RatingBadge> = emptyList(),
)

data class RatingBadge(val value: String, val source: String, val highlighted: Boolean)

/**
 * Главная кнопка: что играет ([season] и [videoId] — для плеера, номер серии уникален только в
 * сезоне) и что о себе пишет. [compact] — однострочная подпись для кнопки ТВ («Продолжить · S4E7»),
 * [subtitle] — вторая строка телефона («S4E7 «Мост» · осталось 29 мин»).
 */
data class PlayButton(
    val season: Int,
    val videoId: Int,
    val title: String,
    val subtitle: String?,
    val compact: String,
    /** Полоска досмотренного под кнопкой — только у серии, которую продолжают. */
    val progress: Float?,
)

/** Панель серий сериала: сезоны чипами, строки выбранного сезона и куда листать стрелками. */
data class EpisodesPanel(
    val seasons: List<SeasonTab>,
    val rows: List<EpisodeRow>,
    /** «10 серий · ~55 мин» — число серий сезона и средняя длительность; null — сезон пуст. */
    val summary: String?,
    /** С какой серии открывается список: «продолжить» или первая недосмотренная. */
    val startIndex: Int,
    /** Серия «продолжить», если она в этом сезоне: на ней стартует фокус ТВ. */
    val startFocusEpisodeId: Int?,
    /** Соседние сезоны для «←/→» с серии на ТВ; null — листать некуда. */
    val previous: SeasonStart?,
    val next: SeasonStart?,
)

/** Чип сезона: полная подпись («Сезон 4» / «Серии») и короткая для невыбранных на ТВ («4»). */
data class SeasonTab(val number: Int, val label: String, val compactLabel: String, val selected: Boolean)

/** Сезон и серия, с которой он откроется, — фокус ТВ назначается ей заранее. */
data class SeasonStart(val season: Int, val episodeId: Int)

/** Строка серии: «7. Тайный совет», мета о том, где в серии зритель, прогресс и отметки. */
data class EpisodeRow(
    val episode: MediaTrack,
    val title: String,
    val meta: String?,
    /** Доля просмотренного 0..1; null — серию не начинали. */
    val progress: Float?,
    val watched: Boolean,
    /** Серия «продолжить» (и не досмотрена): подсвечена. */
    val resume: Boolean,
)

sealed interface DetailsEvent {
    data object Back : DetailsEvent
    data object ToggleFav : DetailsEvent
    data object ToggleDownload : DetailsEvent

    /** Главная кнопка — играет [DetailsState.play]. */
    data object Play : DetailsEvent

    /** Строка серии: [videoId] — номер серии, уникален только внутри [season]. */
    data class PlayEpisode(val season: Int, val videoId: Int) : DetailsEvent

    /** Телефон: то же, что [Play], но на телевизоре — выбор ТВ в сети и пульт. */
    data object PlayOnTv : DetailsEvent
    data object PlayTrailer : DetailsEvent
    data object Share : DetailsEvent
    data class OpenItem(val itemId: Int) : DetailsEvent

    /** Тап по актёру или режиссёру — его фильмография ([isDirector] различает запрос к API). */
    data class OpenPerson(val name: String, val isDirector: Boolean) : DetailsEvent

    /**
     * Сезон в фокусе. На ТВ сезон меняется фокусом на чипе, без «ОК», — модель выжидает 250 мс,
     * чтобы быстрое пролистывание чипов не перерисовывало каждый сезон по пути.
     */
    data class SelectSeason(val number: Int) : DetailsEvent

    /**
     * Сезон выбран стрелкой с серии или тапом на телефоне — применяется сразу: это одно осознанное
     * нажатие, а не пролистывание чипов. Отменяет выбор чипом, если тот ещё выжидает.
     */
    data class SwitchSeason(val number: Int) : DetailsEvent

    data object OpenDescription : DetailsEvent
    data object CloseDescription : DetailsEvent
}

sealed interface DetailsSideEffect {
    /** «Поделиться» — системный диалог умеет открыть только экран. */
    data class Share(val text: String) : DetailsSideEffect
}
