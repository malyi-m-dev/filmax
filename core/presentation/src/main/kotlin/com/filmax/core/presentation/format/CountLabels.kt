package com.filmax.core.presentation.format

private const val HUNDRED = 100
private const val TEN = 10
private val TEENS = 11..14
private val FEW = 2..4

/**
 * «1 тайтл», «3 тайтла», «12 тайтлов» — счётчик папки. «Тайтл», а не «фильм»: в папке лежат и
 * фильмы, и сериалы. Одна функция на «Моё», экран папки и панель «В папку».
 */
fun titlesLabel(count: Int): String = "$count ${plural(count, one = "тайтл", few = "тайтла", many = "тайтлов")}"

/**
 * «1 фильм», «3 фильма», «12 фильмов» — счётчик подборки на ТВ, как в макете. Раньше жил двумя
 * копиями: на Главной и на экране подборки.
 */
fun filmsLabel(count: Int): String = "$count ${plural(count, one = "фильм", few = "фильма", many = "фильмов")}"

/** Русское склонение по числу: 1 фильм, 3 фильма, 11 и 12 фильмов. */
private fun plural(count: Int, one: String, few: String, many: String): String {
    val lastTwo = count % HUNDRED
    val last = count % TEN
    return when {
        lastTwo in TEENS -> many
        last == 1 -> one
        last in FEW -> few
        else -> many
    }
}
