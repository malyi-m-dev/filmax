package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.data.fake.FakeIds

// Названия серий объявлены выше списков тайтлов: top-level val инициализируются сверху вниз.

private val BLADE_TITLES = listOf(
    "Осколок звезды",
    "Дух клинка",
    "Беглая жрица",
    "Лис, который слишком много болтал",
    "Город без солнца",
    "Орден мечников",
    "Испытание",
    "Сумеречный дух",
    "Дорога на север",
    "Предательство",
    "Первый луч",
    "Обещание",
    "Новая луна",
    "Храм ветров",
)

private val SCHOOL_TITLES = listOf(
    "Новенькая",
    "Записка в книге",
    "Библиотека после уроков",
    "Чужой почерк",
    "Школьный фестиваль",
    "Дождливый вторник",
    "Ответ",
    "Летние каникулы",
    "Голос",
    "Ссора",
    "Последний звонок",
    "Выпускной",
)

private val TOKYO_TITLES = listOf(
    "Компаньон",
    "Сны машины",
    "Неоновый дождь",
    "Пропавший код",
    "Старый квартал",
    "Сердце из стали",
    "Охота",
    "Перезагрузка",
    "Город спит",
    "Память",
    "Сбой",
    "Последний оператор",
    "Рассвет над заливом",
)

private val COOK_TITLES = listOf(
    "Первый бульон",
    "Дракон голоден",
    "Соль и магия",
    "Рынок гоблинов",
    "Специи королевы",
    "Конкурс поваров",
    "Рамен для героя",
    "Пир",
    "Гильдия",
    "Секретный соус",
    "Голодная зима",
    "Повар и рыцарь",
    "Открытие сезона",
)

private val ANIME_MINUTES = 23..24

/**
 * Аниме — жанр «Аниме» поверх типа serial/movie, как у kino.pub: чип «Аниме» в каталоге ищет
 * именно жанр, а карточка подписывает такой тайтл «Сериалом».
 */
internal val ANIME: List<TitleSpec> = listOf(
    TitleSpec(
        id = FakeIds.ANIME_SERIES,
        title = "Клинок рассветного неба",
        type = ItemType.SERIES,
        year = 2020,
        genres = listOf(Genres.ANIME, Genres.FANTASY, Genres.ACTION),
        country = Countries.JAPAN,
        director = People.MATSUDA,
        cast = listOf(People.TANABE, People.ASAHINA, People.KISARAGI),
        plot = paragraph(
            "В этом мире солнце встаёт раз в сто лет, а всё остальное время небом правят духи сумерек.",
            "Юный кузнец Сора куёт клинок из осколка упавшей звезды и случайно будит в нём дух древнего воина.",
            "Теперь за ним охотятся и демоны тьмы, и орден мечников, считающий клинок своей реликвией.",
            "С беглой жрицей и болтливым лисом-оборотнем Сора идёт к краю мира, где спрятан ключ к рассвету.",
            "С каждым боем он всё лучше владеет мечом и всё хуже понимает, чьи желания исполняет — свои или духа.",
        ),
        media = SeriesMedia(
            seasons = pooledSeasons(
                episodeCounts = listOf(12, 12),
                pool = BLADE_TITLES,
                minutes = ANIME_MINUTES,
                seed = FakeIds.ANIME_SERIES,
            ),
        ),
        kinopoisk = 8.0,
        imdb = 8.2,
        views = 68_000,
        finished = false,
    ),
    TitleSpec(
        id = 3002,
        title = "Школа тихих голосов",
        type = ItemType.SERIES,
        year = 2021,
        genres = listOf(Genres.ANIME, Genres.DRAMA),
        country = Countries.JAPAN,
        director = People.MATSUDA,
        cast = listOf(People.KISARAGI, People.ASAHINA),
        plot = paragraph(
            "Новенькая в классе не произносит ни слова.",
            "Но её записки в библиотечных книгах меняют жизнь всего класса.",
        ),
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = SCHOOL_TITLES, minutes = ANIME_MINUTES))),
        kinopoisk = 7.7,
        imdb = 7.9,
        views = 31_000,
    ),
    TitleSpec(
        id = 3003,
        title = "Механическое сердце Токио",
        type = ItemType.SERIES,
        year = 2018,
        genres = listOf(Genres.ANIME, Genres.SCI_FI, Genres.ACTION),
        country = Countries.JAPAN,
        director = People.OHASHI,
        cast = listOf(People.TANABE, People.KISARAGI, People.ASAHINA),
        plot = paragraph(
            "Токио, 2099 год: у каждого жителя есть механический компаньон.",
            "У одного детектива компаньон однажды начинает видеть сны.",
        ),
        media = SeriesMedia(
            seasons = pooledSeasons(
                episodeCounts = listOf(12, 13, 12),
                pool = TOKYO_TITLES,
                minutes = 23..25,
                seed = 3003,
            ),
        ),
        kinopoisk = 7.5,
        imdb = 7.6,
        views = 40_000,
    ),
    TitleSpec(
        id = 3004,
        title = "Повар из другого мира",
        type = ItemType.SERIES,
        year = 2022,
        genres = listOf(Genres.ANIME, Genres.COMEDY, Genres.FANTASY),
        country = Countries.JAPAN,
        director = People.OHASHI,
        cast = listOf(People.ASAHINA, People.TANABE),
        plot = paragraph(
            "Повар токийской закусочной попадает в мир драконов и магов, где никто не слышал о специях.",
            "И открывает там первую в истории раменную.",
        ),
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = COOK_TITLES, minutes = ANIME_MINUTES))),
        kinopoisk = 7.2,
        imdb = 7.4,
        views = 29_000,
        finished = false,
    ),
    TitleSpec(
        id = 3005,
        title = "Сад падающих звёзд",
        type = ItemType.MOVIE,
        year = 2019,
        // Аниме-фильм у kino.pub — ещё и «Мультфильм»: он есть и в ряду мультфильмов на Главной.
        genres = listOf(Genres.ANIME, Genres.CARTOON, Genres.FANTASY, Genres.MELODRAMA),
        country = Countries.JAPAN,
        director = People.MATSUDA,
        cast = listOf(People.TANABE, People.ASAHINA, People.KISARAGI),
        plot = "Каждое лето девушка ждёт на крыше обсерватории звездопад — и юношу, который приходит только с ним.",
        media = MovieMedia(runtimeMinutes = 106),
        kinopoisk = 7.9,
        imdb = 7.7,
        views = 43_000,
    ),
)

private val BAIKAL_TITLES = listOf("Лёд", "Весна", "Нерпа", "Лето на Ольхоне", "Шторм")

private val DEEP_SEA_SEASON_1 = listOf(
    "Сумеречная зона",
    "Чёрные курильщики",
    "Гиганты глубин",
    "Светящиеся",
    "Подводные вулканы",
    "Будущее океана",
)

private val DEEP_SEA_SEASON_2 = listOf("Арктика", "Антарктида", "Подо льдом", "Киты", "Таяние", "Последний холод")

private val RIVERS_TITLES = listOf("Волга", "Енисей", "Амур", "Обь", "Лена", "Нева", "Дон", "Кама")

private val SPACE_TITLES = listOf(
    "Первая ракета",
    "Спутник",
    "Человек в космосе",
    "Луна",
    "Орбитальные станции",
    "Частный космос",
)

private val SILK_ROAD_TITLES = listOf(
    "Сиань",
    "Ланьчжоу",
    "Кашгар",
    "Алматы",
    "Ташкент",
    "Самарканд",
    "Бухара",
    "Тегеран",
    "Тебриз",
    "Стамбул",
)

private val DEEP_SEA_MINUTES = 50..58

/** Документальные сериалы: у каждого один рассказчик в «актёрах». */
internal val DOCUMENTARIES: List<TitleSpec> = listOf(
    TitleSpec(
        id = 4001,
        title = "Байкал: живая вода",
        type = ItemType.DOCUMENTARY,
        year = 2021,
        genres = listOf(Genres.NATURE),
        country = Countries.RUSSIA,
        director = People.VOLSKY,
        cast = listOf(People.VALDAEV),
        plot = "Год на самом глубоком озере планеты: от ледяных торосов до нерпичьих лежбищ.",
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = BAIKAL_TITLES, minutes = 48..52))),
        kinopoisk = 8.3,
        imdb = 8.0,
        views = 15_000,
    ),
    TitleSpec(
        id = FakeIds.DOCUMENTARY_SERIES,
        title = "Тайны глубин",
        type = ItemType.DOCUMENTARY,
        year = 2019,
        genres = listOf(Genres.NATURE, Genres.SCIENCE),
        country = Countries.UK,
        director = People.PEYTON,
        cast = listOf(People.ALDRIDGE),
        plot = paragraph(
            "Океан покрывает две трети планеты, но изучен хуже, чем поверхность Луны.",
            "Съёмочная группа четыре года работала на исследовательских судах, батискафах и дрейфующих станциях.",
            "Здесь кальмары светятся, чтобы стать невидимыми, а рыбы годами ждут добычу в полной темноте.",
            "Целые экосистемы на дне живут теплом подводных вулканов, а не солнечным светом.",
            "Учёные объясняют, как глубины управляют климатом всей Земли и почему это касается каждого.",
            "Второй сезон посвящён полярным морям и тому, как они меняются прямо сейчас.",
        ),
        media = SeriesMedia(
            seasons = listOf(
                season(number = 1, titles = DEEP_SEA_SEASON_1, minutes = DEEP_SEA_MINUTES),
                season(number = 2, titles = DEEP_SEA_SEASON_2, minutes = DEEP_SEA_MINUTES),
            ),
        ),
        kinopoisk = 8.6,
        imdb = 8.8,
        views = 35_000,
        uhd = true,
    ),
    TitleSpec(
        id = 4003,
        title = "Города, которые построили реки",
        type = ItemType.DOCUMENTARY,
        year = 2022,
        genres = listOf(Genres.TRAVEL, Genres.HISTORY),
        country = Countries.RUSSIA,
        director = People.VOLSKY,
        cast = listOf(People.VALDAEV),
        plot = paragraph(
            "Как Волга, Енисей и Амур решили, где и как жить людям.",
            "Путешествие по восьми речным городам.",
        ),
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = RIVERS_TITLES, minutes = 44..48))),
        kinopoisk = 7.8,
        imdb = 7.5,
        views = 11_000,
    ),
    TitleSpec(
        id = 4004,
        title = "Космос: от первого шага",
        type = ItemType.DOCUMENTARY,
        year = 2023,
        genres = listOf(Genres.SCIENCE, Genres.HISTORY),
        country = Countries.USA,
        director = People.MORGAN,
        cast = listOf(People.ALDRIDGE),
        plot = "История космонавтики от первых ракет до частных полётов — глазами инженеров, которые их строили.",
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = SPACE_TITLES, minutes = 50..55))),
        kinopoisk = 8.1,
        imdb = 8.3,
        views = 27_000,
    ),
    TitleSpec(
        id = 4005,
        title = "Кухни Шёлкового пути",
        type = ItemType.DOCUMENTARY,
        year = 2024,
        genres = listOf(Genres.TRAVEL),
        country = Countries.KAZAKHSTAN,
        director = People.SAPAROVA,
        cast = listOf(People.AKHMETOV),
        plot = "Шеф-повар проезжает от Сианя до Стамбула и в каждом городе готовит одно блюдо вместе с местными.",
        media = SeriesMedia(seasons = listOf(season(number = 1, titles = SILK_ROAD_TITLES, minutes = 38..42))),
        views = 7_000,
        finished = false,
        hasCastPhotos = false,
    ),
)
