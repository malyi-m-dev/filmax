package com.filmax.data.fake.fixtures

import com.filmax.core.domain.catalog.model.Country
import com.filmax.core.domain.catalog.model.Genre
import com.filmax.core.domain.catalog.model.ItemType

/** Страна производства. [language] — язык оригинальной дорожки: от него зависят озвучки тайтла. */
internal data class FixtureCountry(val id: Int, val title: String, val language: String) {
    fun toDomain(): Country = Country(id = id, title = title)
}

/** Человек из титров. [id] нужен только фото: одно лицо у актёра во всех тайтлах. */
internal data class Person(val id: Int, val name: String)

/** Носитель тайтла: у фильма — хронометраж, у сериала — сезоны. */
internal sealed interface MediaSpec

internal data class MovieMedia(val runtimeMinutes: Int) : MediaSpec

internal data class SeriesMedia(val seasons: List<SeasonSpec>) : MediaSpec

internal data class SeasonSpec(val number: Int, val episodes: List<EpisodeSpec>)

/** Серия. Пустой [title] и [hasStill] = false — как у kino.pub, когда серия не размечена. */
internal data class EpisodeSpec(
    val number: Int,
    val title: String,
    val durationSeconds: Int,
    val hasStill: Boolean = true,
)

/**
 * Описание тайтла: только то, чем один тайтл отличается от другого. Постеры, файлы, озвучки,
 * рейтинг Filmax и длительность выводятся из него при сборке каталога — так полсотни тайтлов
 * не тонут в одинаковом шуме.
 */
internal data class TitleSpec(
    val id: Int,
    val title: String,
    val type: ItemType,
    val year: Int,
    val genres: List<Genre>,
    val country: FixtureCountry,
    val director: Person,
    val cast: List<Person>,
    val plot: String,
    val media: MediaSpec,
    val kinopoisk: Double? = null,
    val imdb: Double? = null,
    /** Популярность: по ней «горячее», «в тренде» и hero главной. */
    val views: Int = 0,
    /** Сериал завершён. Фильм завершён всегда — иначе фильтр «Завершённые» прятал бы все фильмы. */
    val finished: Boolean = true,
    /** Есть 2160p — фильтр «Только 4K» и пункт качества в плеере. */
    val uhd: Boolean = false,
    val hasTrailer: Boolean = true,
    /** false — у тайтла нет IMDb-id, «TMDB» актёров не найдёт, и детали покажут инициалы. */
    val hasCastPhotos: Boolean = true,
)

/** Описание из нескольких предложений: в коде по предложению на строку, на экране — один абзац. */
internal fun paragraph(vararg sentences: String): String = sentences.joinToString(" ")
