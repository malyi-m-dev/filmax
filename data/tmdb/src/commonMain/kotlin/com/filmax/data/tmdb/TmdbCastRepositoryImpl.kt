package com.filmax.data.tmdb

import com.filmax.core.domain.person.CastMember
import com.filmax.core.domain.person.CastRepository
import com.filmax.data.tmdb.remote.TmdbApi

internal class TmdbCastRepositoryImpl(
    private val api: TmdbApi,
) : CastRepository {

    override suspend fun getCast(imdbId: String?): List<CastMember> {
        val imdbTag = imdbTag(imdbId).takeIf { api.hasKey } ?: return emptyList()
        // Любая неудача (нет совпадения, сбой сети, невалидный ответ) — пустой список: фото
        // украшают детали, ронять или тормозить из-за них экран нельзя.
        return runCatching { fetchCast(imdbTag) }.getOrDefault(emptyList())
    }

    private suspend fun fetchCast(imdbTag: String): List<CastMember> {
        val found = api.findByImdb(imdbTag)
        // Тип берём из ответа find, а не угадываем: TMDB сам говорит, фильм это или сериал.
        val credits = when {
            found.movieResults.isNotEmpty() -> api.movieCredits(found.movieResults.first().id)
            found.tvResults.isNotEmpty() -> api.tvCredits(found.tvResults.first().id)
            else -> return emptyList()
        }
        return credits.cast.take(MAX_CAST).map { dto ->
            CastMember(
                name = dto.name,
                character = dto.character?.takeIf { it.isNotBlank() },
                photoUrl = dto.profilePath?.let { path -> IMAGE_BASE + path },
            )
        }
    }

    /**
     * kino.pub отдаёт числовой IMDb-id (например `2861424`); TMDB ждёт полный тег `tt…`.
     * Классические id — 7 цифр с ведущими нулями, поэтому добиваем до семи; более длинные
     * (8-значные) остаются как есть. Значение уже с `tt` принимаем как есть.
     */
    private fun imdbTag(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        return when {
            value.isEmpty() -> null
            value.startsWith("tt") -> value
            else -> value.filter { it.isDigit() }
                .takeIf { it.isNotEmpty() }
                ?.let { digits -> "tt" + digits.padStart(IMDB_MIN_DIGITS, '0') }
        }
    }

    private companion object {
        const val IMAGE_BASE = "https://image.tmdb.org/t/p/w185"
        const val MAX_CAST = 20
        const val IMDB_MIN_DIGITS = 7
    }
}
