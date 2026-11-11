package com.filmax.core.domain.catalog.model

/**
 * Адрес видео внутри тайтла. kino.pub адресует видео номером (`number`), и тем же числом принимает
 * и отдаёт прогресс. Номер серии уникален только внутри сезона, поэтому серия без сезона — не
 * адрес: S3E2 нашлась бы как S1E2. Тип делает это правило проверкой компилятора, а не соглашением.
 */
sealed interface VideoRef {
    val number: Int

    /** Видео фильма: у фильма из нескольких частей их больше одного. */
    data class Movie(override val number: Int) : VideoRef

    /** Серия: [number] — номер внутри [season]. */
    data class Episode(val season: Int, override val number: Int) : VideoRef

    companion object {
        /** «Не выбрано» в маршрутах и протоколе связи с ТВ, где адрес — пара чисел. */
        const val NONE = -1

        /**
         * Адрес из пары «сезон + номер», как её носят маршруты и протокол: номер ≤ 0 — видео не
         * выбрано (плеер возьмёт продолжение или первое), сезон ≤ 0 — фильм.
         */
        fun of(season: Int, number: Int): VideoRef? = when {
            number <= 0 -> null
            season > 0 -> Episode(season, number)
            else -> Movie(number)
        }
    }
}

/** Сезон для пары чисел маршрута: у фильма и невыбранного видео — [VideoRef.NONE]. */
val VideoRef?.seasonOrNone: Int get() = (this as? VideoRef.Episode)?.season ?: VideoRef.NONE

/** Номер для пары чисел маршрута: у невыбранного видео — [VideoRef.NONE]. */
val VideoRef?.numberOrNone: Int get() = this?.number ?: VideoRef.NONE

/** Адрес трека: у сериала трек — серия сезона, у фильма — видео по номеру. */
val MediaTrack.ref: VideoRef
    get() = if (seasonNumber > 0) VideoRef.Episode(seasonNumber, number) else VideoRef.Movie(number)
