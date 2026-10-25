package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.CatalogGenres
import com.filmax.core.domain.catalog.model.Genre

private const val MOVIE = "movie"
private const val SERIAL = "serial"
private const val DOCUSERIAL = "docuserial"
private const val CONCERT = "concert"
private const val MUSIC = "music"

/**
 * Жанры — как их отдаёт `api/v1/genres`: с типом раздела и одним списком на все разделы, включая
 * музыкальные. Каталог приложения обязан отсеять «Blues» и «Chillout» — здесь видно, что отсеивает.
 *
 * Id — настоящие id kino.pub там, где они известны: тематические ряды Главной и чип «Аниме» ищут
 * жанр по id, и на выдуманных номерах мок-сборка показала бы в «Комедиях» драмы. У остальных id
 * свои, но ни с одним настоящим не совпадают.
 */
internal object Genres {
    val COMEDY = Genre(id = CatalogGenres.COMEDY, title = "Комедия", type = MOVIE)
    val ACTION = Genre(id = 2, title = "Боевик", type = MOVIE)
    val SCI_FI = Genre(id = CatalogGenres.SCI_FI, title = "Фантастика", type = MOVIE)
    val FANTASY = Genre(id = 5, title = "Фэнтези", type = MOVIE)
    val FAMILY = Genre(id = 6, title = "Семейный", type = MOVIE)
    val THRILLER = Genre(id = 7, title = "Триллер", type = MOVIE)
    val ADVENTURE = Genre(id = 8, title = "Приключения", type = MOVIE)
    val DRAMA = Genre(id = 9, title = "Драма", type = MOVIE)
    val MELODRAMA = Genre(id = 10, title = "Мелодрама", type = MOVIE)
    val HORROR = Genre(id = 12, title = "Ужасы", type = MOVIE)
    val DETECTIVE = Genre(id = 13, title = "Детектив", type = MOVIE)
    val HISTORY = Genre(id = 14, title = "История", type = MOVIE)
    val WAR = Genre(id = 15, title = "Военный", type = MOVIE)
    val SPORT = Genre(id = 16, title = "Спорт", type = MOVIE)
    val CRIME = Genre(id = 17, title = "Криминал", type = MOVIE)
    val SITCOM = Genre(id = 20, title = "Ситком", type = SERIAL)
    val MYSTIC = Genre(id = 21, title = "Мистика", type = SERIAL)
    val DORAMA = Genre(id = CatalogGenres.DORAMA, title = "Дорама", type = SERIAL)
    val CARTOON = Genre(id = CatalogGenres.CARTOON, title = "Мультфильм", type = MOVIE)
    val ANIME = Genre(id = CatalogGenres.ANIME, title = "Аниме", type = MOVIE)
    val NATURE = Genre(id = 30, title = "Природа", type = DOCUSERIAL)
    val SCIENCE = Genre(id = 31, title = "Наука", type = DOCUSERIAL)
    val TRAVEL = Genre(id = 32, title = "Путешествия", type = DOCUSERIAL)
    val ROCK = Genre(id = 40, title = "Рок", type = CONCERT)
    val CLASSICAL = Genre(id = 41, title = "Классика", type = CONCERT)
    private val BLUES = Genre(id = 90, title = "Blues", type = MUSIC)
    private val CHILLOUT = Genre(id = 91, title = "Chillout", type = MUSIC)

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
