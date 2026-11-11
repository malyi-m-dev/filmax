package com.filmax.core.domain.catalog.model

/** Чья это оценка. */
enum class RatingSource {
    KINOPOISK,
    IMDB,

    /** Оценка самого источника — у домашнего сервера это его рейтинг из метаданных медиатеки. */
    COMMUNITY,
}

/** Оценка [value] по шкале 0–10 от [source]. */
data class Rating(val source: RatingSource, val value: Double)

/**
 * Оценки тайтла. Набор зависит от источника: у kino.pub — КиноПоиск и IMDb, у домашнего сервера —
 * его собственная. Раньше модель знала только поля kino.pub, и тайтлы сервера оставались без
 * оценки на карточках, а фильтр «КП от N» выбрасывал их все.
 *
 * Порядок [values] значим: первая — главная оценка источника ([primary]), её показывает пилюля
 * на постере.
 */
data class ItemRating(val values: List<Rating> = emptyList()) {

    /** Главная оценка источника: КиноПоиск у kino.pub, своя — у сервера. null — оценок нет. */
    val primary: Double? get() = values.firstOrNull()?.value

    /** Оценка от [source]; null — у тайтла её нет. */
    fun of(source: RatingSource): Double? = values.firstOrNull { it.source == source }?.value

    /** Средняя по всем оценкам, шкала 0–10; null — оценок нет (в UI — «N/A»). */
    val external: Double? get() = values.takeIf { it.isNotEmpty() }?.map { it.value }?.average()

    companion object {
        /** Оценки из строк, как их присылают API: пустые и нечисловые («—») пропускаются. */
        fun of(vararg ratings: Pair<RatingSource, String?>): ItemRating = ItemRating(
            ratings.mapNotNull { (source, raw) ->
                raw?.toDoubleOrNull()?.takeIf { it > 0 }?.let { Rating(source, it) }
            },
        )
    }
}
