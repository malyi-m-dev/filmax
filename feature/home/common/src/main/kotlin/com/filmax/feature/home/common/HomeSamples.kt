// Неизменные образцы состояния Главной — для превью телефона и ТВ. Карточки «Продолжить» и
// подборок собраны теми же функциями, что в модели, поэтому превью показывает настоящие подписи.
package com.filmax.feature.home.common

import com.filmax.core.domain.catalog.model.Collection
import com.filmax.core.domain.watching.model.WatchHistory
import com.filmax.core.domain.watching.model.WatchProgress
import com.filmax.core.presentation.cards.TitleCard
import com.filmax.core.presentation.cards.watchCard

private const val SERIES_DURATION_SECONDS = 3_120
private const val MOVIE_DURATION_SECONDS = 7_860
private const val STATUS_IN_PROGRESS = 0

object HomeSamples {

    private val heroes = listOf(
        HeroCard(
            id = 1,
            title = "Хроники Пограничья",
            plot = "Картограф находит на старой карте город, которого нет ни в одном атласе.",
            posterUrl = "",
            backdropUrl = "",
            rating = "8.4",
            meta = listOf("Фантастика", "2024", "2 ч 46 мин"),
            wideMeta = listOf("Фантастика · Драма · Приключения", "2024", "2 ч 46 мин"),
        ),
        HeroCard(
            id = 2,
            title = "Северный ветер",
            plot = "Метеостанция за полярным кругом теряет связь с материком на всю зиму.",
            posterUrl = "",
            backdropUrl = "",
            rating = null,
            meta = listOf("Триллер", "2023"),
            wideMeta = listOf("Триллер · Детектив", "2023"),
        ),
    )

    private val series = WatchHistory(
        itemId = 1,
        title = "Хроники Пограничья",
        posterSmall = null,
        progress = WatchProgress(STATUS_IN_PROGRESS, 1_260, SERIES_DURATION_SECONDS, videoId = 7, season = 4),
        lastSeenAt = null,
    )

    private val movie = WatchHistory(
        itemId = 3,
        title = "Дом на краю карты",
        posterSmall = null,
        progress = WatchProgress(STATUS_IN_PROGRESS, 5_400, MOVIE_DURATION_SECONDS, videoId = 1, season = null),
        lastSeenAt = null,
    )

    private val continueRow = HomeRow.Continue(listOf(watchCard(series), watchCard(movie)))

    private val titles = listOf(
        card(3, "Дом на краю карты", rating = "8.1", typeAndYear = "Фильм · 2022", yearAndLength = "2022 · 1 ч 52 мин"),
        card(4, "Код доступа", rating = "7.1", typeAndYear = "Сериал · 2021", yearAndLength = "2021"),
        card(5, "Полярный круг", rating = null, typeAndYear = "Концерт · 2019", yearAndLength = "2019 · 1 ч 35 мин"),
    )

    private val collections = HomeRow.Collections(
        paging = RowPaging(
            listOf(
                Collection(id = 10, title = "Лучшее за десятилетие", description = null, posters = null),
                Collection(id = 11, title = "Тихие вечера", description = null, posters = null),
            ),
        ),
        previews = mapOf(10 to CollectionPreview(posters = emptyList(), count = 12)),
    )

    private fun card(id: Int, title: String, rating: String?, typeAndYear: String, yearAndLength: String) = TitleCard(
        id = id,
        title = title,
        posterUrl = "",
        widePosterUrl = "",
        rating = rating,
        ratingHigh = rating == "8.1",
        typeAndYear = typeAndYear,
        yearAndLength = yearAndLength,
        freshBadge = if (typeAndYear.startsWith("Фильм")) "Премьера" else "Новинка",
    )

    /** Лента целиком: hero, «Продолжить», топ, подборки и новинки. */
    val loaded = HomeState(
        loading = false,
        initials = "МА",
        heroes = heroes,
        watchLaterIds = setOf(1),
        rows = listOf(
            continueRow,
            HomeRow.Titles(HomeRowId.TRENDING, RowPaging(titles)),
            collections,
            HomeRow.Titles(HomeRowId.FRESH, RowPaging(titles.reversed())),
        ),
    )

    /** Первый вход: истории просмотра ещё нет — ряда «Продолжить» тоже. */
    val withoutHistory = loaded.copy(rows = loaded.rows.drop(1))

    /** Холодный старт без сети: показывать нечего. */
    val empty = HomeState(loading = false)
    val loading = HomeState()

    val all: List<HomeState> = listOf(loaded, withoutHistory, empty, loading)
}
