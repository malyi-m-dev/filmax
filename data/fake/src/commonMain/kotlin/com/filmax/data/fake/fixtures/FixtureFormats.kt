package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.data.fake.FakeIds

// Тайтлы для тематических рядов Главной: мультфильмы, дорама и форматы не про игровое кино —
// документальные фильмы, ТВ-шоу, концерты. Без них ряды на мок-сборке пусты, и экран их не рисует.
//
// Оценок КП и IMDb у этих тайтлов нет: так в каждом формате видна карточка без оценки, а
// «Зрители рекомендуют» (все тайтлы с оценкой) остаётся опорной подборкой на 45 тайтлов — на неё
// ссылаются FakeIds и тесты подборок.

/** Мультфильмы — фильмы с жанром «Мультфильм»: так их отдаёт kino.pub, отдельного типа нет. */
internal val CARTOONS: List<TitleSpec> = listOf(
    TitleSpec(
        id = 1027,
        title = "Лис и северное сияние",
        type = ItemType.MOVIE,
        year = 2025,
        genres = listOf(Genres.CARTOON, Genres.FAMILY, Genres.ADVENTURE),
        country = Countries.RUSSIA,
        director = People.BELETSKAYA,
        cast = listOf(People.ERMOLAEVA, People.PASECHNIK),
        plot = "Лисёнок с Кольского полуострова уходит искать, куда пропадает северное сияние каждой весной.",
        media = MovieMedia(runtimeMinutes = 84),
        views = 24_000,
    ),
    TitleSpec(
        id = 1028,
        title = "Робот в саду",
        type = ItemType.MOVIE,
        year = 2026,
        genres = listOf(Genres.CARTOON, Genres.FAMILY, Genres.COMEDY),
        country = Countries.USA,
        director = People.ROCKWOOD,
        cast = listOf(People.DONOVAN, People.CALDWELL),
        plot = "Садовый робот решает, что лучший способ спасти розы — переселить хозяев на другую планету.",
        media = MovieMedia(runtimeMinutes = 91),
        views = 18_000,
    ),
)

private val JEJU_TITLES = listOf(
    "Письмо без адреса",
    "Мандариновый сад",
    "Почтальон",
    "Ветер с моря",
    "Старый маяк",
    "Чужая тайна",
    "Сезон дождей",
    "Ответное письмо",
    "Паром",
    "Возвращение",
)

/** Дорама — сериал с жанром «Дорама»: вторая к «Сеулу под дождём», чтобы ряду было что листать. */
internal val DORAMAS: List<TitleSpec> = listOf(
    TitleSpec(
        id = 2014,
        title = "Письма с острова Чеджу",
        type = ItemType.SERIES,
        year = 2024,
        genres = listOf(Genres.DORAMA, Genres.MELODRAMA),
        country = Countries.KOREA,
        director = People.CHOI,
        cast = listOf(People.YOON_SO_JIN, People.LEE_MIN_SO, People.KANG_HYE_RIN),
        plot = paragraph(
            "Почтальон с острова Чеджу находит в старом ящике письма, которые тридцать лет никто не забирал.",
            "Она решает доставить их все — и каждое меняет чью-то жизнь, включая её собственную.",
        ),
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = JEJU_TITLES, minutes = 58..66))),
        views = 26_000,
        finished = false,
    ),
)

/** Документальные фильмы — тип `documovie`: одно видео, рассказчик вместо актёров. */
internal val DOCUMENTARY_FILMS: List<TitleSpec> = listOf(
    TitleSpec(
        id = 5001,
        title = "Последний ледник",
        type = ItemType.DOCUMENTARY_MOVIE,
        year = 2024,
        genres = listOf(Genres.NATURE, Genres.SCIENCE),
        country = Countries.RUSSIA,
        director = People.VOLSKY,
        cast = listOf(People.VALDAEV),
        plot = "Год с гляциологами на Алтае: как тает ледник, который помнит мамонтов.",
        media = MovieMedia(runtimeMinutes = 78),
        views = 9_000,
    ),
    TitleSpec(
        id = 5002,
        title = "Мастера тишины",
        type = ItemType.DOCUMENTARY_MOVIE,
        year = 2022,
        genres = listOf(Genres.HISTORY),
        country = Countries.UK,
        director = People.PEYTON,
        cast = listOf(People.ALDRIDGE),
        plot = "Последние реставраторы немого кино спасают плёнки, которые рассыпаются от прикосновения.",
        media = MovieMedia(runtimeMinutes = 95),
        views = 6_000,
        hasTrailer = false,
    ),
)

private val KITCHEN_TITLES = listOf(
    "Казань",
    "Астрахань",
    "Дагестан",
    "Калининград",
    "Карелия",
    "Владивосток",
    "Якутск",
    "Псков",
)

private val LAUGH_TITLES = listOf(
    "Премьера сезона",
    "Гости из провинции",
    "Новогодний выпуск",
    "Импровизация",
    "Семейный вечер",
    "Битва команд",
    "Лучшее за год",
    "Финал",
    "Сборная звёзд",
    "Юбилей",
)

private val SHOW_MINUTES = 40..48

/** ТВ-шоу — тип `tvshow`: сезоны и выпуски, как у сериала, но с ведущим вместо актёров. */
internal val TV_SHOWS: List<TitleSpec> = listOf(
    TitleSpec(
        id = FakeIds.TV_SHOW,
        title = "Кухня на колёсах",
        type = ItemType.TV_SHOW,
        year = 2022,
        genres = listOf(Genres.TRAVEL),
        country = Countries.RUSSIA,
        director = People.CHISTYAKOV,
        cast = listOf(People.ROSTOV),
        plot = "Шеф на фудтраке проезжает страну и в каждом городе готовит вместе с местными их главное блюдо.",
        media = SeriesMedia(
            seasons = pooledSeasons(
                episodeCounts = listOf(8, 8),
                pool = KITCHEN_TITLES,
                minutes = SHOW_MINUTES,
                seed = FakeIds.TV_SHOW,
            ),
        ),
        views = 21_000,
        finished = false,
    ),
    TitleSpec(
        id = 6002,
        title = "Поле смеха",
        type = ItemType.TV_SHOW,
        year = 2021,
        genres = listOf(Genres.COMEDY),
        country = Countries.RUSSIA,
        director = People.LANSKOY,
        cast = listOf(People.ZVONAREVA, People.KEDROV),
        plot = "Импровизационное шоу: две команды комиков и ни одной заготовленной шутки.",
        media = SeriesMedia(
            seasons = pooledSeasons(
                episodeCounts = listOf(10, 10, 10),
                pool = LAUGH_TITLES,
                minutes = SHOW_MINUTES,
                seed = 6002,
            ),
        ),
        views = 14_000,
        hasTrailer = false,
    ),
)

/** Концерты — тип `concert`: одно видео, оригинальный звук без перевода (см. voicesFor). */
internal val CONCERTS: List<TitleSpec> = listOf(
    TitleSpec(
        id = FakeIds.CONCERT,
        title = "Полярный круг. Живой звук",
        type = ItemType.CONCERT,
        year = 2023,
        genres = listOf(Genres.ROCK),
        country = Countries.RUSSIA,
        director = People.CHISTYAKOV,
        cast = listOf(People.STRELTSOV),
        plot = "Юбилейный концерт группы «Полярный круг» в Мурманске: двадцать песен за двадцать лет.",
        media = MovieMedia(runtimeMinutes = 112),
        views = 12_000,
    ),
    TitleSpec(
        id = 7002,
        title = "Ночь в Альберт-холле",
        type = ItemType.CONCERT,
        year = 2019,
        genres = listOf(Genres.CLASSICAL),
        country = Countries.UK,
        director = People.PEYTON,
        cast = listOf(People.BRIGHT),
        plot = "Скрипачка Оливия Брайт и симфонический оркестр — от Баха до музыки из кино.",
        media = MovieMedia(runtimeMinutes = 128),
        views = 8_000,
        uhd = true,
    ),
)
