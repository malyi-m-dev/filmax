package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.ReferenceGenre
import com.filmax.core.domain.catalog.model.Genre

/**
 * Id опорных жанров — настоящие id kino.pub: тематические ряды Главной и чип «Аниме» ищут жанр
 * через [ReferenceGenre], и на выдуманных номерах мок-сборка показала бы в «Комедиях» драмы.
 */
internal val ReferenceGenre.fixtureId: Int
    get() = when (this) {
        ReferenceGenre.COMEDY -> 1
        ReferenceGenre.SCI_FI -> 4
        ReferenceGenre.CARTOON -> 23
        ReferenceGenre.ANIME -> 25
        ReferenceGenre.DORAMA -> 107
    }

/**
 * Жанры — как их отдаёт `api/v1/genres`: одним списком на все разделы, включая музыкальные.
 * Каталог приложения обязан отсеять «Blues» и «Chillout» — здесь видно, что отсеивает.
 *
 * Id опорных жанров — настоящие id kino.pub ([fixtureId]); у остальных свои, но ни с одним
 * настоящим не совпадают.
 */
internal object Genres {
    val COMEDY = Genre(id = ReferenceGenre.COMEDY.fixtureId, title = "Комедия")
    val ACTION = Genre(id = 2, title = "Боевик")
    val SCI_FI = Genre(id = ReferenceGenre.SCI_FI.fixtureId, title = "Фантастика")
    val FANTASY = Genre(id = 5, title = "Фэнтези")
    val FAMILY = Genre(id = 6, title = "Семейный")
    val THRILLER = Genre(id = 7, title = "Триллер")
    val ADVENTURE = Genre(id = 8, title = "Приключения")
    val DRAMA = Genre(id = 9, title = "Драма")
    val MELODRAMA = Genre(id = 10, title = "Мелодрама")
    val HORROR = Genre(id = 12, title = "Ужасы")
    val DETECTIVE = Genre(id = 13, title = "Детектив")
    val HISTORY = Genre(id = 14, title = "История")
    val WAR = Genre(id = 15, title = "Военный")
    val SPORT = Genre(id = 16, title = "Спорт")
    val CRIME = Genre(id = 17, title = "Криминал")
    val SITCOM = Genre(id = 20, title = "Ситком")
    val MYSTIC = Genre(id = 21, title = "Мистика")
    val DORAMA = Genre(id = ReferenceGenre.DORAMA.fixtureId, title = "Дорама")
    val CARTOON = Genre(id = ReferenceGenre.CARTOON.fixtureId, title = "Мультфильм")
    val ANIME = Genre(id = ReferenceGenre.ANIME.fixtureId, title = "Аниме")
    val NATURE = Genre(id = 30, title = "Природа")
    val SCIENCE = Genre(id = 31, title = "Наука")
    val TRAVEL = Genre(id = 32, title = "Путешествия")
    val ROCK = Genre(id = 40, title = "Рок")
    val CLASSICAL = Genre(id = 41, title = "Классика")
    private val BLUES = Genre(id = 90, title = "Blues", video = false)
    private val CHILLOUT = Genre(id = 91, title = "Chillout", video = false)

    val ALL: List<Genre> = listOf(
        DRAMA,
        COMEDY,
        THRILLER,
        SCI_FI,
        DETECTIVE,
        ADVENTURE,
        MELODRAMA,
        HORROR,
        ACTION,
        FANTASY,
        CRIME,
        FAMILY,
        HISTORY,
        WAR,
        SPORT,
        CARTOON,
        SITCOM,
        MYSTIC,
        DORAMA,
        ANIME,
        NATURE,
        SCIENCE,
        TRAVEL,
        ROCK,
        CLASSICAL,
        BLUES,
        CHILLOUT,
    )
}

/**
 * Страны для фильтра каталога. Из Италии в каталоге нет ни одного тайтла — фильтр по ней проверяет
 * пустую выдачу.
 */
internal object Countries {
    val RUSSIA = FixtureCountry(id = 1, title = "Россия", language = "rus")
    val USA = FixtureCountry(id = 2, title = "США", language = "eng")
    val UK = FixtureCountry(id = 3, title = "Великобритания", language = "eng")
    val FRANCE = FixtureCountry(id = 4, title = "Франция", language = "fra")
    val JAPAN = FixtureCountry(id = 5, title = "Япония", language = "jpn")
    val KOREA = FixtureCountry(id = 6, title = "Южная Корея", language = "kor")
    val GERMANY = FixtureCountry(id = 7, title = "Германия", language = "deu")
    val SPAIN = FixtureCountry(id = 8, title = "Испания", language = "spa")
    val CANADA = FixtureCountry(id = 9, title = "Канада", language = "eng")
    val SWEDEN = FixtureCountry(id = 10, title = "Швеция", language = "swe")
    val KAZAKHSTAN = FixtureCountry(id = 11, title = "Казахстан", language = "kaz")
    private val ITALY = FixtureCountry(id = 12, title = "Италия", language = "ita")

    val ALL: List<FixtureCountry> = listOf(
        RUSSIA,
        USA,
        UK,
        FRANCE,
        JAPAN,
        KOREA,
        GERMANY,
        SPAIN,
        CANADA,
        SWEDEN,
        KAZAKHSTAN,
        ITALY,
    )
}
