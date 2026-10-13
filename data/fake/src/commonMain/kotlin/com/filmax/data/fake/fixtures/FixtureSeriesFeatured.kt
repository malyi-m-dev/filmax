package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.ItemType
import com.filmax.data.fake.FakeIds

// Порядок объявлений важен: top-level val инициализируются сверху вниз, поэтому названия серий
// стоят выше списка сериалов, который на них ссылается.

private val DELIVERY_TITLES = listOf(
    "Первый заказ",
    "Пицца для депутата",
    "Код от домофона",
    "Собака, которая знала адрес",
    "Ночная смена в метель",
    "Последняя доставка, или Почему никто не открывает дверь на девятом этаже",
)

private val NORTH_WIND_SEASON_1 = listOf(
    "Пустая лодка",
    "Следы на льду",
    "Старая верфь",
    "Ложный маяк",
    "Полярная ночь",
    "Шторм",
    "Чужие сети",
    "Возвращение",
)

private val NORTH_WIND_SEASON_2 = listOf(
    "Новый прокурор",
    "Тень на воде",
    "Свидетель из Териберки",
    "Мёртвый сезон",
    "Цена молчания",
    "Без связи",
    "Дом у причала",
    "Северное сияние",
    "Признание",
    "Ледоход",
)

private val NORTH_WIND_SEASON_3 = listOf(
    "Через год",
    "Рыбный король",
    "Карта течений",
    "Последний паром",
    "Огни Мурманска",
    "Ледяной плен",
    "Кто прав, тот и виноват: долгая ночь в посёлке Лиинахамари",
    "Буря",
    "Ветер стихает",
)

private val NORTH_WIND_MINUTES = 48..56

private val BORDERLANDS_TITLES = listOf(
    "Посол",
    "Северная стена",
    "Цена короны",
    "Дорога на Эшфорд",
    "Совет пяти домов",
    "Беглянка",
    "Пепел и сталь",
    "Клятва",
    "Ледяной перевал",
    "Последний караван",
    "Дети тумана",
    "Змеиный двор",
    "Турнир",
    "Бастард",
    "Огни на холмах",
    "Чужая кровь",
    "Осада",
    "Тайный ход",
    "Ворон",
    "Свадьба",
    "Перемирие",
    "Шёпот в библиотеке",
    "Мост через Серую реку",
    "Изгнанник",
    "Белая королева",
    "Старые долги",
    "Предатель среди нас",
    "Песнь о павших",
    "Буря над Пограничьем",
    "Цитадель",
    "Ключ от северных врат",
    "Дорога домой",
    "Ночной дозор",
    "Возвращение наследницы",
    "Тот, кто выжил",
    "Камень и шёпот",
    "Последний рассвет",
    "Корона без короля",
    "Всё, что мы не успели сказать друг другу у северной стены",
    "Долгая зима",
)

/** Серий в сезонах «Хроник Пограничья»; к седьмому дописываются ещё четыре неразмеченные. */
private val BORDERLANDS_EPISODES = listOf(12, 13, 22, 24, 22, 20, 12)
private const val BORDERLANDS_UNTITLED_TAIL = 4
private val BORDERLANDS_MINUTES = 40..58

/**
 * Три сериала для навигации по сезонам: короткий (1 × 6), средний (3 × 8–10) и длинный
 * (7 сезонов по 12–24 серии) — стресс-тест списка серий и селектора сезонов. Прогресс по ним
 * задан в FixtureHistory.
 */
internal val FEATURED_SERIES: List<TitleSpec> = listOf(
    TitleSpec(
        id = FakeIds.SHORT_SERIES,
        title = "Доставка до двери",
        type = ItemType.SERIES,
        year = 2024,
        genres = listOf(Genres.COMEDY),
        country = Countries.RUSSIA,
        director = People.CHISTYAKOV,
        cast = listOf(People.SHATALOV, People.LAVROVA, People.VELEKHOV, People.GRINEVSKAYA),
        plot = paragraph(
            "Веб-сериал о курьере, который за одну смену успевает спасти чужую свадьбу и сорвать ограбление.",
            "Только обед себе так и не довозит. Шесть серий по десять минут.",
        ),
        // Русский сериал играет на потоке с одной дорожкой, а серии ровно в его длину — их можно
        // досмотреть в плеере и увидеть, как «Продолжить» переезжает на следующую серию.
        media = SeriesMedia(
            seasons = listOf(
                streamLengthSeason(
                    number = 1,
                    titles = DELIVERY_TITLES,
                    seconds = TestStream.SINGLE_AUDIO.lengthSeconds,
                ),
            ),
        ),
        kinopoisk = 7.3,
        imdb = 7.0,
        views = 26_000,
    ),
    TitleSpec(
        id = FakeIds.MEDIUM_SERIES,
        title = "Северный ветер",
        type = ItemType.SERIES,
        year = 2019,
        genres = listOf(Genres.DRAMA, Genres.DETECTIVE),
        country = Countries.RUSSIA,
        director = People.SAMARIN,
        cast = listOf(
            People.VORONTSOVA,
            People.SEVERIN,
            People.GORDEEV,
            People.MARTYNYUK,
            People.KRAYNEV,
            People.PASECHNIK,
        ),
        plot = paragraph(
            "Следователь Вера Сомова возвращается из Москвы в родной посёлок на Кольском полуострове.",
            "Она хочет только продать дом умершего отца — но в первую же ночь в заливе находят пустую лодку.",
            "Мотор заведён, рыбака нет, а следы на льду ведут не к берегу, а от него.",
            "Расследование возвращает Веру к истории двадцатилетней давности: тогда в посёлке пропали трое.",
            "И её отец был последним, кто их видел.",
            "Каждый сезон — новое дело, но все нити сходятся к старой верфи.",
            "Северный нуар о месте, где все друг друга знают и поэтому молчат.",
        ),
        media = SeriesMedia(
            seasons = listOf(
                season(number = 1, titles = NORTH_WIND_SEASON_1, minutes = NORTH_WIND_MINUTES),
                season(number = 2, titles = NORTH_WIND_SEASON_2, minutes = NORTH_WIND_MINUTES),
                season(number = 3, titles = NORTH_WIND_SEASON_3, minutes = NORTH_WIND_MINUTES),
            ),
        ),
        kinopoisk = 8.2,
        imdb = 7.9,
        views = 71_000,
    ),
    TitleSpec(
        id = FakeIds.LONG_SERIES,
        title = "Хроники Пограничья",
        type = ItemType.SERIES,
        year = 2012,
        genres = listOf(Genres.FANTASY, Genres.DRAMA, Genres.ADVENTURE),
        country = Countries.USA,
        director = People.ROCKWOOD,
        cast = listOf(
            People.CALDWELL,
            People.DONOVAN,
            People.REILLY,
            People.STONE,
            People.BRANDON,
            People.ALDRIDGE,
        ),
        plot = paragraph(
            "Пограничье — полоса земли между двумя королевствами, где двести лет держится хрупкий мир.",
            "Когда в пограничной крепости убивают посла, под подозрением оказывается наследница дома Эшфорд.",
            "Лира бежит на север, в земли, откуда не возвращаются.",
            "Её брат остаётся при дворе искать убийцу и быстро понимает, что во дворце опаснее, чем на войне.",
            "А за северной стеной тем временем просыпается то, о чём рассказывают только в детских страшилках.",
            "Семь сезонов, десятки героев и сюжет, в котором ни одно решение не проходит бесследно.",
        ),
        media = SeriesMedia(seasons = borderlandsSeasons()),
        kinopoisk = 8.4,
        imdb = 8.6,
        views = 92_000,
        // Седьмой сезон ещё выходит: свежие серии приходят без названий и кадров.
        finished = false,
    ),
)

private fun borderlandsSeasons(): List<SeasonSpec> {
    val seasons = pooledSeasons(
        episodeCounts = BORDERLANDS_EPISODES,
        pool = BORDERLANDS_TITLES,
        minutes = BORDERLANDS_MINUTES,
        seed = FakeIds.LONG_SERIES,
    )
    return seasons.dropLast(1) + seasons.last().withUntitledTail(BORDERLANDS_UNTITLED_TAIL, BORDERLANDS_MINUTES)
}
