package com.filmax.data.jellyfin.mapper

/**
 * Даты Jellyfin — ISO-8601 в UTC («2024-05-01T18:30:12.1234567Z»). Своя пара функций вместо
 * kotlinx-datetime: из дат нужны только «когда смотрели» и границы года для фильтра, а лишняя
 * библиотека в iOS-фреймворке стоит дороже двадцати строк.
 */
internal object IsoDates {

    private val ISO = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})""")

    /** Мс от эпохи; null — строка не разобрана. Доли секунды и смещение отбрасываются: сервер пишет UTC. */
    fun toEpochMillis(text: String?): Long? {
        val parts = text?.let { ISO.find(it) }?.groupValues?.drop(1)?.map { it.toInt() } ?: return null
        val days = daysFromCivil(year = parts[YEAR], month = parts[MONTH], day = parts[DAY])
        val hours = days * HOURS_PER_DAY + parts[HOUR]
        return (hours * SECONDS_PER_HOUR + parts[MINUTE] * SECONDS_PER_MINUTE + parts[SECOND]) * MILLIS
    }

    /** Начало года [year] — нижняя граница фильтра по году выхода. */
    fun startOfYear(year: Int): String = "$year-01-01T00:00:00Z"

    /** Конец года [year] — верхняя граница фильтра. */
    fun endOfYear(year: Int): String = "$year-12-31T23:59:59Z"

    /**
     * Дней от 1970-01-01 до даты по григорианскому календарю (алгоритм days_from_civil
     * Говарда Хиннанта): без таблиц месяцев и високосных лет вручную.
     */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        val shiftedYear = (if (month <= 2) year - 1 else year).toLong()
        val era = (if (shiftedYear >= 0) shiftedYear else shiftedYear - ERA_YEARS + 1) / ERA_YEARS
        val yearOfEra = shiftedYear - era * ERA_YEARS
        val monthFromMarch = (month + (if (month > 2) -MARCH else MONTHS_PER_YEAR - MARCH)).toLong()
        val dayOfYear = (DAYS_IN_FIVE_MONTHS * monthFromMarch + 2) / FIVE_MONTHS + day - 1
        val dayOfEra = yearOfEra * DAYS_PER_YEAR + yearOfEra / LEAP_EVERY - yearOfEra / CENTURY + dayOfYear
        return era * DAYS_PER_ERA + dayOfEra - EPOCH_SHIFT_DAYS
    }

    // Номера групп регулярки ISO по порядку: год, месяц, день, час, минута, секунда.
    private const val YEAR = 0
    private const val MONTH = 1
    private const val DAY = 2
    private const val HOUR = 3
    private const val MINUTE = 4
    private const val SECOND = 5
    private const val HOURS_PER_DAY = 24L
    private const val SECONDS_PER_HOUR = 3600L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MILLIS = 1000L
    private const val ERA_YEARS = 400L
    private const val MARCH = 3
    private const val MONTHS_PER_YEAR = 12
    private const val DAYS_IN_FIVE_MONTHS = 153L
    private const val FIVE_MONTHS = 5L
    private const val DAYS_PER_YEAR = 365L
    private const val LEAP_EVERY = 4L
    private const val CENTURY = 100L
    private const val DAYS_PER_ERA = 146_097L
    private const val EPOCH_SHIFT_DAYS = 719_468L
}
